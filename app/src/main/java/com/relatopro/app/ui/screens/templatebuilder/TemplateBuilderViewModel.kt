package com.relatopro.app.ui.screens.templatebuilder

import androidx.lifecycle.ViewModel
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
    private val templateRepository: TemplateRepository,
) : ViewModel() {

    private val _templateName = MutableStateFlow("")
    val templateName: StateFlow<String> = _templateName.asStateFlow()

    private val _fields = MutableStateFlow<List<TemplateFieldEntity>>(emptyList())
    val fields: StateFlow<List<TemplateFieldEntity>> = _fields.asStateFlow()

    fun updateName(name: String) {
        _templateName.value = name
    }

    fun addField(label: String, type: String) {
        val currentList = _fields.value.toMutableList()
        val newField = TemplateFieldEntity(
            templateId = 0, // Será setado no repositorio
            category = "Geral",
            label = label,
            type = type, // e.g. "C_NC_NA", "TEXT"
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
            // Reorder
            val reordered = currentList.mapIndexed { i, field -> field.copy(orderIndex = i) }
            _fields.value = reordered
        }
    }

    fun saveTemplate(onComplete: () -> Unit) {
        viewModelScope.launch {
            val template = TemplateEntity(
                name = _templateName.value.ifEmpty { "Novo Template" },
                description = "",
                category = "Geral",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                status = "ACTIVE",
                visualConfig = "{}",
            )
            templateRepository.createTemplate(template, _fields.value)
            onComplete()
        }
    }
}
