package com.relatopro.app.ui.screens.importer

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.relatopro.app.data.local.entity.TemplateEntity
import com.relatopro.app.data.local.entity.TemplateFieldEntity
import com.relatopro.app.domain.repository.TemplateRepository
import com.relatopro.app.importer.SmartImportEngine
import com.relatopro.app.importer.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SmartImportViewModel @Inject constructor(
    application: Application,
    private val templateRepository: TemplateRepository
) : AndroidViewModel(application) {

    private val engine = SmartImportEngine()
    private var importJob: Job? = null

    private val prefs = application.getSharedPreferences("relatopro_prefs", Context.MODE_PRIVATE)
    private val currentUserEmail: String = prefs.getString("user_email", "")?.ifBlank { "default_user" } ?: "default_user"

    private val _importState = MutableStateFlow<ImportProgressState>(ImportProgressState.Idle)
    val importState: StateFlow<ImportProgressState> = _importState.asStateFlow()

    private val _parsedChecklist = MutableStateFlow<ParsedChecklist?>(null)
    val parsedChecklist: StateFlow<ParsedChecklist?> = _parsedChecklist.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun startImport(uri: Uri) {
        val context = getApplication<Application>()
        val fileName = getFileNameFromUri(context, uri)

        _importState.value = ImportProgressState.Processing(
            stage = "Iniciando processamento...",
            progressPercent = 0,
            details = "Arquivo: $fileName"
        )

        importJob?.cancel()
        importJob = viewModelScope.launch {
            try {
                val result = engine.parseDocument(
                    context = context,
                    uri = uri,
                    fileName = fileName,
                    onProgress = { stage, percent, detail ->
                        _importState.value = ImportProgressState.Processing(stage, percent, detail)
                    }
                )
                _parsedChecklist.value = result
                _importState.value = ImportProgressState.Success(result)
            } catch (e: Exception) {
                e.printStackTrace()
                _importState.value = ImportProgressState.Error(
                    e.localizedMessage ?: "Erro desconhecido ao importar arquivo."
                )
            }
        }
    }

    fun cancelImport() {
        importJob?.cancel()
        importJob = null
        _importState.value = ImportProgressState.Idle
    }

    fun resetState() {
        _importState.value = ImportProgressState.Idle
        _parsedChecklist.value = null
    }

    // ==========================================
    // CHECKLIST HEADER EDITING
    // ==========================================
    fun updateTitle(newTitle: String) {
        val current = _parsedChecklist.value ?: return
        _parsedChecklist.value = current.copy(title = newTitle.trim())
    }

    fun updateCategory(newCategory: String) {
        val current = _parsedChecklist.value ?: return
        _parsedChecklist.value = current.copy(category = newCategory.trim())
    }

    fun updateDescription(newDesc: String) {
        val current = _parsedChecklist.value ?: return
        _parsedChecklist.value = current.copy(description = newDesc.trim())
    }

    // ==========================================
    // STATUS CUSTOMIZATION
    // ==========================================
    fun addCustomStatus(
        name: String,
        shortCode: String,
        description: String,
        colorHex: String,
        scoreMultiplier: Float?
    ) {
        val current = _parsedChecklist.value ?: return
        val cleanCode = shortCode.trim().uppercase()
        if (cleanCode.isBlank() || name.isBlank()) return

        if (current.availableStatuses.any { it.shortCode.equals(cleanCode, ignoreCase = true) }) {
            return
        }

        val newStatus = CustomStatusConfig(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            shortCode = cleanCode,
            description = description.trim(),
            colorHex = colorHex,
            scoreMultiplier = scoreMultiplier,
            isSystemDefault = false
        )

        val updatedStatuses = current.availableStatuses + newStatus
        _parsedChecklist.value = current.copy(availableStatuses = updatedStatuses)
    }

    fun removeStatus(statusShortCode: String): Boolean {
        val current = _parsedChecklist.value ?: return false

        val updatedStatuses = current.availableStatuses.filterNot { it.shortCode.equals(statusShortCode, ignoreCase = true) }

        val updatedSections = current.sections.map { section ->
            section.copy(
                items = section.items.map { item ->
                    item.copy(allowedStatuses = item.allowedStatuses.filterNot { it.equals(statusShortCode, ignoreCase = true) })
                },
                categories = section.categories.map { category ->
                    category.copy(
                        items = category.items.map { item ->
                            item.copy(allowedStatuses = item.allowedStatuses.filterNot { it.equals(statusShortCode, ignoreCase = true) })
                        }
                    )
                }
            )
        }

        _parsedChecklist.value = current.copy(
            availableStatuses = updatedStatuses,
            sections = updatedSections
        )
        return true
    }

    // ==========================================
    // SECTION MANAGEMENT
    // ==========================================
    fun addSection(sectionName: String) {
        val current = _parsedChecklist.value ?: return
        val cleanName = sectionName.trim()
        if (cleanName.isBlank()) return

        val newSection = ParsedSection(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            orderIndex = current.sections.size,
            items = emptyList(),
            categories = emptyList()
        )

        _parsedChecklist.value = current.copy(sections = current.sections + newSection)
    }

    fun renameSection(sectionId: String, newName: String) {
        val current = _parsedChecklist.value ?: return
        val cleanName = newName.trim()
        if (cleanName.isBlank()) return

        val updated = current.sections.map { sec ->
            if (sec.id == sectionId) sec.copy(name = cleanName) else sec
        }
        _parsedChecklist.value = current.copy(sections = updated)
    }

    fun deleteSection(sectionId: String) {
        val current = _parsedChecklist.value ?: return
        val updated = current.sections.filterNot { it.id == sectionId }
            .mapIndexed { idx, sec -> sec.copy(orderIndex = idx) }
        _parsedChecklist.value = current.copy(sections = updated)
    }

    fun moveSectionUp(sectionId: String) {
        val current = _parsedChecklist.value ?: return
        val list = current.sections.toMutableList()
        val idx = list.indexOfFirst { it.id == sectionId }
        if (idx > 0) {
            val item = list.removeAt(idx)
            list.add(idx - 1, item)
            val reordered = list.mapIndexed { i, s -> s.copy(orderIndex = i) }
            _parsedChecklist.value = current.copy(sections = reordered)
        }
    }

    fun moveSectionDown(sectionId: String) {
        val current = _parsedChecklist.value ?: return
        val list = current.sections.toMutableList()
        val idx = list.indexOfFirst { it.id == sectionId }
        if (idx in 0 until list.lastIndex) {
            val item = list.removeAt(idx)
            list.add(idx + 1, item)
            val reordered = list.mapIndexed { i, s -> s.copy(orderIndex = i) }
            _parsedChecklist.value = current.copy(sections = reordered)
        }
    }

    // ==========================================
    // CATEGORY MANAGEMENT
    // ==========================================
    fun addCategory(sectionId: String, categoryName: String) {
        val current = _parsedChecklist.value ?: return
        val cleanName = categoryName.trim()
        if (cleanName.isBlank()) return

        val updated = current.sections.map { sec ->
            if (sec.id == sectionId) {
                val newCat = ParsedCategory(
                    id = UUID.randomUUID().toString(),
                    name = cleanName,
                    orderIndex = sec.categories.size,
                    items = emptyList()
                )
                sec.copy(categories = sec.categories + newCat)
            } else sec
        }
        _parsedChecklist.value = current.copy(sections = updated)
    }

    fun renameCategory(sectionId: String, categoryId: String, newName: String) {
        val current = _parsedChecklist.value ?: return
        val cleanName = newName.trim()
        if (cleanName.isBlank()) return

        val updated = current.sections.map { sec ->
            if (sec.id == sectionId) {
                sec.copy(
                    categories = sec.categories.map { cat ->
                        if (cat.id == categoryId) cat.copy(name = cleanName) else cat
                    }
                )
            } else sec
        }
        _parsedChecklist.value = current.copy(sections = updated)
    }

    fun deleteCategory(sectionId: String, categoryId: String) {
        val current = _parsedChecklist.value ?: return
        val updated = current.sections.map { sec ->
            if (sec.id == sectionId) {
                sec.copy(categories = sec.categories.filterNot { it.id == categoryId })
            } else sec
        }
        _parsedChecklist.value = current.copy(sections = updated)
    }

    // ==========================================
    // ITEM MANAGEMENT
    // ==========================================
    fun addItem(sectionId: String, categoryId: String? = null, label: String, type: String = "C_NC_NA") {
        val current = _parsedChecklist.value ?: return
        val cleanLabel = label.trim()
        if (cleanLabel.isBlank()) return

        val targetSec = current.sections.find { it.id == sectionId } ?: return
        val targetCat = targetSec.categories.find { it.id == categoryId }

        val newItem = ParsedItem(
            id = UUID.randomUUID().toString(),
            sectionName = targetSec.name,
            categoryName = targetCat?.name,
            hierarchyPath = if (targetCat != null) "${targetSec.name} > ${targetCat.name}" else targetSec.name,
            label = cleanLabel,
            type = type,
            allowedStatuses = current.availableStatuses.map { it.shortCode },
            allowObservation = true,
            allowPhoto = true,
            allowAttachment = false,
            isRequired = true
        )

        val updatedSections = current.sections.map { sec ->
            if (sec.id == sectionId) {
                if (categoryId != null) {
                    sec.copy(
                        categories = sec.categories.map { cat ->
                            if (cat.id == categoryId) cat.copy(items = cat.items + newItem) else cat
                        }
                    )
                } else {
                    sec.copy(items = sec.items + newItem)
                }
            } else sec
        }
        _parsedChecklist.value = current.copy(sections = updatedSections)
    }

    fun updateItem(
        sectionId: String,
        categoryId: String? = null,
        itemId: String,
        label: String,
        type: String,
        allowObservation: Boolean,
        allowPhoto: Boolean,
        allowAttachment: Boolean,
        isRequired: Boolean
    ) {
        val current = _parsedChecklist.value ?: return
        val cleanLabel = label.trim()
        if (cleanLabel.isBlank()) return

        val updatedSections = current.sections.map { sec ->
            if (sec.id == sectionId) {
                if (categoryId != null) {
                    sec.copy(
                        categories = sec.categories.map { cat ->
                            if (cat.id == categoryId) {
                                cat.copy(
                                    items = cat.items.map { item ->
                                        if (item.id == itemId) {
                                            item.copy(
                                                label = cleanLabel,
                                                type = type,
                                                allowObservation = allowObservation,
                                                allowPhoto = allowPhoto,
                                                allowAttachment = allowAttachment,
                                                isRequired = isRequired
                                            )
                                        } else item
                                    }
                                )
                            } else cat
                        }
                    )
                } else {
                    sec.copy(
                        items = sec.items.map { item ->
                            if (item.id == itemId) {
                                item.copy(
                                    label = cleanLabel,
                                    type = type,
                                    allowObservation = allowObservation,
                                    allowPhoto = allowPhoto,
                                    allowAttachment = allowAttachment,
                                    isRequired = isRequired
                                )
                            } else item
                        }
                    )
                }
            } else sec
        }
        _parsedChecklist.value = current.copy(sections = updatedSections)
    }

    fun deleteItem(sectionId: String, categoryId: String? = null, itemId: String) {
        val current = _parsedChecklist.value ?: return
        val updatedSections = current.sections.map { sec ->
            if (sec.id == sectionId) {
                if (categoryId != null) {
                    sec.copy(
                        categories = sec.categories.map { cat ->
                            if (cat.id == categoryId) {
                                cat.copy(items = cat.items.filterNot { it.id == itemId })
                            } else cat
                        }
                    )
                } else {
                    sec.copy(items = sec.items.filterNot { it.id == itemId })
                }
            } else sec
        }
        _parsedChecklist.value = current.copy(sections = updatedSections)
    }

    fun duplicateItem(sectionId: String, categoryId: String? = null, itemId: String) {
        val current = _parsedChecklist.value ?: return
        val targetSec = current.sections.find { it.id == sectionId } ?: return

        val updatedSections = current.sections.map { sec ->
            if (sec.id == sectionId) {
                if (categoryId != null) {
                    sec.copy(
                        categories = sec.categories.map { cat ->
                            if (cat.id == categoryId) {
                                val idx = cat.items.indexOfFirst { it.id == itemId }
                                if (idx != -1) {
                                    val original = cat.items[idx]
                                    val copy = original.copy(id = UUID.randomUUID().toString(), label = "${original.label} (Cópia)")
                                    val newItems = cat.items.toMutableList()
                                    newItems.add(idx + 1, copy)
                                    cat.copy(items = newItems)
                                } else cat
                            } else cat
                        }
                    )
                } else {
                    val idx = sec.items.indexOfFirst { it.id == itemId }
                    if (idx != -1) {
                        val original = sec.items[idx]
                        val copy = original.copy(id = UUID.randomUUID().toString(), label = "${original.label} (Cópia)")
                        val newItems = sec.items.toMutableList()
                        newItems.add(idx + 1, copy)
                        sec.copy(items = newItems)
                    } else sec
                }
            } else sec
        }
        _parsedChecklist.value = current.copy(sections = updatedSections)
    }

    fun moveItemUp(sectionId: String, itemId: String) {
        val current = _parsedChecklist.value ?: return
        val updatedSections = current.sections.map { sec ->
            if (sec.id == sectionId) {
                val list = sec.items.toMutableList()
                val idx = list.indexOfFirst { it.id == itemId }
                if (idx > 0) {
                    val item = list.removeAt(idx)
                    list.add(idx - 1, item)
                    sec.copy(items = list)
                } else {
                    val updatedCats = sec.categories.map { cat ->
                        val catList = cat.items.toMutableList()
                        val cIdx = catList.indexOfFirst { it.id == itemId }
                        if (cIdx > 0) {
                            val item = catList.removeAt(cIdx)
                            catList.add(cIdx - 1, item)
                            cat.copy(items = catList)
                        } else cat
                    }
                    sec.copy(categories = updatedCats)
                }
            } else sec
        }
        _parsedChecklist.value = current.copy(sections = updatedSections)
    }

    fun moveItemDown(sectionId: String, itemId: String) {
        val current = _parsedChecklist.value ?: return
        val updatedSections = current.sections.map { sec ->
            if (sec.id == sectionId) {
                val list = sec.items.toMutableList()
                val idx = list.indexOfFirst { it.id == itemId }
                if (idx in 0 until list.lastIndex) {
                    val item = list.removeAt(idx)
                    list.add(idx + 1, item)
                    sec.copy(items = list)
                } else {
                    val updatedCats = sec.categories.map { cat ->
                        val catList = cat.items.toMutableList()
                        val cIdx = catList.indexOfFirst { it.id == itemId }
                        if (cIdx in 0 until catList.lastIndex) {
                            val item = catList.removeAt(cIdx)
                            catList.add(cIdx + 1, item)
                            cat.copy(items = catList)
                        } else cat
                    }
                    sec.copy(categories = updatedCats)
                }
            } else sec
        }
        _parsedChecklist.value = current.copy(sections = updatedSections)
    }

    fun promoteToSection(sectionId: String, categoryId: String? = null, itemId: String) {
        val current = _parsedChecklist.value ?: return
        val targetSec = current.sections.find { it.id == sectionId } ?: return

        val itemToPromote = if (categoryId != null) {
            targetSec.categories.find { it.id == categoryId }?.items?.find { it.id == itemId }
        } else {
            targetSec.items.find { it.id == itemId }
        } ?: return

        // Create new section with the item's label
        val newSection = ParsedSection(
            id = UUID.randomUUID().toString(),
            name = itemToPromote.label,
            orderIndex = current.sections.size,
            items = emptyList(),
            categories = emptyList()
        )

        // Remove from previous location
        val updatedSections = current.sections.map { sec ->
            if (sec.id == sectionId) {
                if (categoryId != null) {
                    sec.copy(
                        categories = sec.categories.map { cat ->
                            if (cat.id == categoryId) {
                                cat.copy(items = cat.items.filterNot { it.id == itemId })
                            } else cat
                        }
                    )
                } else {
                    sec.copy(items = sec.items.filterNot { it.id == itemId })
                }
            } else sec
        } + newSection

        _parsedChecklist.value = current.copy(sections = updatedSections)
    }

    fun resolveAnswerConflict(itemId: String, chosenStatus: String) {
        val current = _parsedChecklist.value ?: return
        val updatedSections = current.sections.map { sec ->
            sec.copy(
                items = sec.items.map { item ->
                    if (item.id == itemId) {
                        val updatedAnswer = item.preFilledAnswer?.copy(
                            statusShortCode = chosenStatus,
                            hasConflict = false
                        )
                        item.copy(preFilledAnswer = updatedAnswer)
                    } else item
                },
                categories = sec.categories.map { cat ->
                    cat.copy(
                        items = cat.items.map { item ->
                            if (item.id == itemId) {
                                val updatedAnswer = item.preFilledAnswer?.copy(
                                    statusShortCode = chosenStatus,
                                    hasConflict = false
                                )
                                item.copy(preFilledAnswer = updatedAnswer)
                            } else item
                        }
                    )
                }
            )
        }

        // Also resolve in validationAlerts
        val updatedAlerts = current.validationAlerts.filterNot { it.elementLabel != null && it.description.contains(itemId) }

        _parsedChecklist.value = current.copy(
            sections = updatedSections,
            validationAlerts = updatedAlerts
        )
    }

    // ==========================================
    // PERSISTENCE AS NATIVE TEMPLATE
    // ==========================================
    fun saveAsNativeTemplate(
        onSuccess: (templateId: Long) -> Unit,
        onError: (String) -> Unit
    ) {
        val checklist = _parsedChecklist.value ?: run {
            onError("Nenhum checklist disponível para salvar.")
            return
        }

        if (checklist.title.isBlank()) {
            onError("Informe o nome do checklist antes de salvar.")
            return
        }

        if (checklist.sections.isEmpty() || checklist.totalItemsCount == 0) {
            onError("O checklist precisa conter pelo menos um item de verificação.")
            return
        }

        if (_isSaving.value) return
        _isSaving.value = true

        viewModelScope.launch {
            try {
                // Serialize custom statuses and visual config into visualConfig JSON
                val visualConfigJson = JSONObject().apply {
                    val statusesArray = JSONArray()
                    checklist.availableStatuses.forEach { status ->
                        val obj = JSONObject().apply {
                            put("name", status.name)
                            put("shortCode", status.shortCode)
                            put("description", status.description)
                            put("colorHex", status.colorHex)
                            put("scoreMultiplier", status.scoreMultiplier?.toDouble() ?: JSONObject.NULL)
                            put("isSystemDefault", status.isSystemDefault)
                        }
                        statusesArray.put(obj)
                    }
                    put("customStatuses", statusesArray)
                    put("importedFrom", checklist.sourceFormat.name)
                    put("originalFileName", checklist.fileName)
                    put("totalConfidenceScore", checklist.totalConfidenceScore.toDouble())
                }.toString()

                val now = System.currentTimeMillis()
                val newTemplate = TemplateEntity(
                    name = checklist.title.trim(),
                    description = checklist.description.trim(),
                    category = checklist.category.trim().ifBlank { "Segurança do Trabalho" },
                    createdAt = now,
                    updatedAt = now,
                    status = "ACTIVE",
                    visualConfig = visualConfigJson,
                    userId = currentUserEmail,
                    isGlobal = false
                )

                // Build TemplateFieldEntities with order index and hierarchical categories
                val templateFields = mutableListOf<TemplateFieldEntity>()
                var globalIndex = 0

                checklist.sections.forEach { section ->
                    // Direct items under section
                    section.items.forEach { item ->
                        val extraConfigJson = JSONObject().apply {
                            put("allowedStatuses", JSONArray(item.allowedStatuses))
                            put("allowObservation", item.allowObservation)
                            put("allowPhoto", item.allowPhoto)
                            put("allowAttachment", item.allowAttachment)
                            put("numberPrefix", item.numberPrefix ?: "")
                            put("hierarchyPath", item.hierarchyPath)
                            if (item.preFilledAnswer != null) {
                                put("preFilledStatus", item.preFilledAnswer.statusShortCode)
                            }
                            put("confidenceScore", item.confidenceScore.toDouble())
                            put("classificationReason", item.classificationReason)
                        }.toString()

                        templateFields.add(
                            TemplateFieldEntity(
                                templateId = 0L,
                                category = section.name.trim(),
                                label = item.label.trim(),
                                type = item.type,
                                orderIndex = globalIndex++,
                                isRequired = item.isRequired,
                                requireObservationOnNC = item.requireObservationOnNC,
                                requirePhotoOnNC = item.requirePhotoOnNC,
                                maxPhotos = item.maxPhotos,
                                extraConfig = extraConfigJson
                            )
                        )
                    }

                    // Items under subcategories
                    section.categories.forEach { category ->
                        val combinedCategoryName = "${section.name.trim()} > ${category.name.trim()}"
                        category.items.forEach { item ->
                            val extraConfigJson = JSONObject().apply {
                                put("allowedStatuses", JSONArray(item.allowedStatuses))
                                put("allowObservation", item.allowObservation)
                                put("allowPhoto", item.allowPhoto)
                                put("allowAttachment", item.allowAttachment)
                                put("numberPrefix", item.numberPrefix ?: "")
                                put("hierarchyPath", combinedCategoryName)
                                if (item.preFilledAnswer != null) {
                                    put("preFilledStatus", item.preFilledAnswer.statusShortCode)
                                }
                                put("confidenceScore", item.confidenceScore.toDouble())
                                put("classificationReason", item.classificationReason)
                            }.toString()

                            templateFields.add(
                                TemplateFieldEntity(
                                    templateId = 0L,
                                    category = combinedCategoryName,
                                    label = item.label.trim(),
                                    type = item.type,
                                    orderIndex = globalIndex++,
                                    isRequired = item.isRequired,
                                    requireObservationOnNC = item.requireObservationOnNC,
                                    requirePhotoOnNC = item.requirePhotoOnNC,
                                    maxPhotos = item.maxPhotos,
                                    extraConfig = extraConfigJson
                                )
                            )
                        }
                    }
                }

                val generatedTemplateId = templateRepository.createTemplate(newTemplate, templateFields)
                _isSaving.value = false
                onSuccess(generatedTemplateId)
            } catch (e: Exception) {
                e.printStackTrace()
                _isSaving.value = false
                onError("Erro ao salvar modelo no banco de dados: ${e.localizedMessage}")
            }
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String {
        var name = "arquivo_importado"
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex) ?: name
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return name
    }
}
