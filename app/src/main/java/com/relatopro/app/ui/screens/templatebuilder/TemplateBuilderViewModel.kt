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

    fun addField(label: String, type: String, category: String = "Geral") {
        val currentList = _fields.value.toMutableList()
        val newField = TemplateFieldEntity(
            templateId = editingTemplateId,
            category = category,
            label = label,
            type = type, // e.g. "C_NC_NA", "TEXT", "PHOTO"
            orderIndex = currentList.size,
            isRequired = true,
        )
        currentList.add(newField)
        _fields.value = currentList
    }

    fun removeField(index: Int) {
        val currentList = _fields.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            val reordered = currentList.mapIndexed { i, field -> field.copy(orderIndex = i) }
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
