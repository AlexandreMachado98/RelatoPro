package com.relatopro.app.ui.screens.templatebuilder

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.relatopro.app.data.local.entity.TemplateEntity
import com.relatopro.app.data.local.entity.TemplateFieldEntity
import com.relatopro.app.domain.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemplateBuilderViewModel @Inject constructor(
    application: Application,
    private val templateRepository: TemplateRepository,
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("relatopro_prefs", Context.MODE_PRIVATE)
    val currentUserEmail: String = prefs.getString("user_email", "")?.ifBlank { "default_user" } ?: "default_user"

    private var editingTemplateId: Long = 0L

    private val _templateName = MutableStateFlow("")
    val templateName: StateFlow<String> = _templateName.asStateFlow()

    private val _templateCategory = MutableStateFlow("Segurança do Trabalho")
    val templateCategory: StateFlow<String> = _templateCategory.asStateFlow()

    private val _templateDescription = MutableStateFlow("")
    val templateDescription: StateFlow<String> = _templateDescription.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(listOf("Geral"))
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _fields = MutableStateFlow<List<TemplateFieldEntity>>(emptyList())
    val fields: StateFlow<List<TemplateFieldEntity>> = _fields.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun loadTemplate(templateId: Long) {
        if (templateId <= 0L) return
        editingTemplateId = templateId
        viewModelScope.launch {
            val template = templateRepository.getTemplateById(templateId)
            if (template != null) {
                _templateName.value = template.name
                _templateCategory.value = template.category
                _templateDescription.value = template.description
                val fieldsList = templateRepository.getTemplateFieldsList(templateId)
                _fields.value = fieldsList

                val distinctCats = fieldsList.map { it.category.ifBlank { "Geral" } }.distinct()
                _categories.value = if (distinctCats.isNotEmpty()) distinctCats else listOf("Geral")
            }
        }
    }

    fun updateName(name: String) {
        _templateName.value = name
    }

    fun updateCategory(category: String) {
        _templateCategory.value = category
    }

    fun updateDescription(desc: String) {
        _templateDescription.value = desc
    }

    // CATEGORY ACTIONS
    fun addCategory(categoryName: String) {
        val clean = categoryName.trim()
        if (clean.isBlank()) return
        if (!_categories.value.contains(clean)) {
            _categories.value = _categories.value + clean
        }
    }

    fun renameCategory(oldName: String, newName: String) {
        val cleanNew = newName.trim()
        if (cleanNew.isBlank() || oldName == cleanNew) return
        val currentCats = _categories.value.toMutableList()
        val idx = currentCats.indexOf(oldName)
        if (idx != -1) {
            currentCats[idx] = cleanNew
            _categories.value = currentCats
        }
        val currentFields = _fields.value.map { field ->
            if (field.category.equals(oldName, ignoreCase = true) || (oldName == "Geral" && field.category.isBlank())) {
                field.copy(category = cleanNew)
            } else {
                field
            }
        }
        _fields.value = currentFields
    }

    fun deleteCategory(categoryName: String) {
        val currentCats = _categories.value.toMutableList()
        currentCats.remove(categoryName)
        _categories.value = currentCats
        val filteredFields = _fields.value.filterNot { 
            it.category.equals(categoryName, ignoreCase = true) || (categoryName == "Geral" && it.category.isBlank()) 
        }
        val reordered = filteredFields.mapIndexed { i, f -> f.copy(orderIndex = i) }
        _fields.value = reordered
    }

    fun duplicateCategory(categoryName: String) {
        val newCatName = "$categoryName (Cópia)"
        _categories.value = _categories.value + newCatName
        val fieldsToCopy = _fields.value.filter { 
            it.category.equals(categoryName, ignoreCase = true) || (categoryName == "Geral" && it.category.isBlank()) 
        }
        val newCopiedFields = fieldsToCopy.map { f ->
            f.copy(id = 0, category = newCatName)
        }
        val combined = _fields.value + newCopiedFields
        val reordered = combined.mapIndexed { i, f -> f.copy(orderIndex = i) }
        _fields.value = reordered
    }

    fun moveCategoryUp(categoryName: String) {
        val currentCats = _categories.value.toMutableList()
        val idx = currentCats.indexOf(categoryName)
        if (idx > 0) {
            val item = currentCats.removeAt(idx)
            currentCats.add(idx - 1, item)
            _categories.value = currentCats
            reorderFieldsByCategories(currentCats)
        }
    }

    fun moveCategoryDown(categoryName: String) {
        val currentCats = _categories.value.toMutableList()
        val idx = currentCats.indexOf(categoryName)
        if (idx in 0 until currentCats.lastIndex) {
            val item = currentCats.removeAt(idx)
            currentCats.add(idx + 1, item)
            _categories.value = currentCats
            reorderFieldsByCategories(currentCats)
        }
    }

    private fun reorderFieldsByCategories(categoryOrder: List<String>) {
        val sortedFields = mutableListOf<TemplateFieldEntity>()
        categoryOrder.forEach { cat ->
            val catFields = _fields.value.filter { it.category.equals(cat, ignoreCase = true) || (cat == "Geral" && it.category.isBlank()) }
            sortedFields.addAll(catFields)
        }
        val otherFields = _fields.value.filterNot { f ->
            categoryOrder.any { cat -> f.category.equals(cat, ignoreCase = true) || (cat == "Geral" && f.category.isBlank()) }
        }
        sortedFields.addAll(otherFields)
        val reordered = sortedFields.mapIndexed { i, f -> f.copy(orderIndex = i) }
        _fields.value = reordered
    }

    // ITEM / QUESTION ACTIONS
    fun addItemToCategory(categoryName: String, label: String, type: String = "C_NC_NA") {
        val cleanLabel = label.trim()
        if (cleanLabel.isBlank()) return
        val currentList = _fields.value.toMutableList()
        val newField = TemplateFieldEntity(
            templateId = editingTemplateId,
            category = categoryName,
            label = cleanLabel,
            type = type,
            orderIndex = currentList.size,
            isRequired = true
        )
        currentList.add(newField)
        _fields.value = currentList
        if (!_categories.value.contains(categoryName)) {
            _categories.value = _categories.value + categoryName
        }
    }

    fun updateItem(globalIndex: Int, newLabel: String, newType: String) {
        val currentList = _fields.value.toMutableList()
        if (globalIndex in currentList.indices) {
            val updated = currentList[globalIndex].copy(label = newLabel.trim(), type = newType)
            currentList[globalIndex] = updated
            _fields.value = currentList
        }
    }

    fun duplicateItem(globalIndex: Int) {
        val currentList = _fields.value.toMutableList()
        if (globalIndex in currentList.indices) {
            val original = currentList[globalIndex]
            val copy = original.copy(id = 0, label = "${original.label} (Cópia)")
            currentList.add(globalIndex + 1, copy)
            val reordered = currentList.mapIndexed { i, f -> f.copy(orderIndex = i) }
            _fields.value = reordered
        }
    }

    fun removeItem(globalIndex: Int) {
        val currentList = _fields.value.toMutableList()
        if (globalIndex in currentList.indices) {
            currentList.removeAt(globalIndex)
            val reordered = currentList.mapIndexed { i, f -> f.copy(orderIndex = i) }
            _fields.value = reordered
        }
    }

    fun moveItemUp(globalIndex: Int) {
        if (globalIndex <= 0) return
        val currentList = _fields.value.toMutableList()
        if (globalIndex in currentList.indices) {
            val item = currentList.removeAt(globalIndex)
            currentList.add(globalIndex - 1, item)
            val reordered = currentList.mapIndexed { i, f -> f.copy(orderIndex = i) }
            _fields.value = reordered
        }
    }

    fun moveItemDown(globalIndex: Int) {
        val currentList = _fields.value.toMutableList()
        if (globalIndex in 0 until currentList.lastIndex) {
            val item = currentList.removeAt(globalIndex)
            currentList.add(globalIndex + 1, item)
            val reordered = currentList.mapIndexed { i, f -> f.copy(orderIndex = i) }
            _fields.value = reordered
        }
    }

    fun saveTemplate(onComplete: () -> Unit) {
        if (_templateName.value.isBlank() || _isSaving.value) return
        _isSaving.value = true

        viewModelScope.launch {
            if (editingTemplateId > 0L) {
                val existing = templateRepository.getTemplateById(editingTemplateId)
                val updatedTemplate = (existing ?: TemplateEntity(
                    id = editingTemplateId,
                    name = _templateName.value.trim(),
                    description = _templateDescription.value.trim(),
                    category = _templateCategory.value.trim(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    userId = currentUserEmail,
                    isGlobal = false
                )).copy(
                    name = _templateName.value.trim(),
                    description = _templateDescription.value.trim(),
                    category = _templateCategory.value.trim(),
                    updatedAt = System.currentTimeMillis(),
                    userId = currentUserEmail,
                    isGlobal = false
                )
                templateRepository.updateTemplate(updatedTemplate, _fields.value)
            } else {
                val newTemplate = TemplateEntity(
                    name = _templateName.value.trim().ifEmpty { "Novo Checklist" },
                    description = _templateDescription.value.trim(),
                    category = _templateCategory.value.trim().ifEmpty { "Geral" },
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    status = "ACTIVE",
                    visualConfig = "{}",
                    userId = currentUserEmail,
                    isGlobal = false
                )
                templateRepository.createTemplate(newTemplate, _fields.value)
            }
            _isSaving.value = false
            onComplete()
        }
    }
}
