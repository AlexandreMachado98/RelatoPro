package com.relatopro.app.ui.screens.checklists

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.relatopro.app.data.local.entity.TemplateEntity
import com.relatopro.app.domain.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChecklistsViewModel @Inject constructor(
    application: Application,
    private val repository: TemplateRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("relatopro_prefs", Context.MODE_PRIVATE)
    val currentUserEmail: String = prefs.getString("user_email", "")?.ifBlank { "default_user" } ?: "default_user"

    private val _selectedTab = MutableStateFlow("TODOS") // TODOS, MEUS_CHECKLISTS, MODELOS
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _fieldsCountMap = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val fieldsCountMap: StateFlow<Map<Long, Int>> = _fieldsCountMap.asStateFlow()

    private val _allTemplates = repository.getAllTemplates()

    val templates: StateFlow<List<TemplateEntity>> = combine(
        _allTemplates,
        _selectedTab,
        _searchQuery
    ) { list, tab, query ->
        val filtered = list.filter { t ->
            val isOfficial = t.isGlobal && t.userId.isBlank() && t.id <= 3L
            val isUserChecklist = !isOfficial

            val matchesTab = when (tab) {
                "MEUS_CHECKLISTS" -> isUserChecklist
                "MODELOS" -> isOfficial
                else -> true
            }
            val matchesQuery = query.isBlank() ||
                    t.name.contains(query, ignoreCase = true) ||
                    t.category.contains(query, ignoreCase = true) ||
                    t.description.contains(query, ignoreCase = true)

            matchesTab && matchesQuery
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val hasSeeded = prefs.getBoolean("has_seeded_initial_templates", false)
            if (!hasSeeded) {
                repository.seedDefaultTemplatesIfEmpty()
                prefs.edit().putBoolean("has_seeded_initial_templates", true).apply()
            }
        }
        
        viewModelScope.launch {
            _allTemplates.collect { list ->
                val countMap = mutableMapOf<Long, Int>()
                list.forEach { template ->
                    val count = repository.getTemplateFieldsList(template.id).size
                    countMap[template.id] = count
                }
                _fieldsCountMap.value = countMap
            }
        }
    }

    fun setTab(tab: String) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun duplicateChecklist(templateId: Long, onDuplicated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val newId = repository.duplicateTemplate(templateId, currentUserEmail)
            if (newId > 0) {
                onDuplicated(newId)
            }
        }
    }

    fun deleteChecklist(templateId: Long) {
        viewModelScope.launch {
            repository.deleteTemplate(templateId)
        }
    }

    suspend fun getTemplateFields(templateId: Long): List<com.relatopro.app.data.local.entity.TemplateFieldEntity> {
        return repository.getTemplateFieldsList(templateId)
    }

    fun importChecklistPackage(pkg: com.relatopro.app.utils.ChecklistShareUtil.ChecklistPackage, onImported: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val newTemplate = TemplateEntity(
                name = pkg.name,
                description = pkg.description,
                category = pkg.category.ifBlank { pkg.categories.firstOrNull()?.name ?: "Personalizados" },
                version = 1,
                createdAt = now,
                updatedAt = now,
                isGlobal = false,
                userId = currentUserEmail
            )

            var globalOrder = 0
            val fieldsToInsert = mutableListOf<com.relatopro.app.data.local.entity.TemplateFieldEntity>()
            for (cat in pkg.categories) {
                for (item in cat.items) {
                    fieldsToInsert.add(
                        com.relatopro.app.data.local.entity.TemplateFieldEntity(
                            templateId = 0L,
                            category = cat.name,
                            label = item.label,
                            type = item.type,
                            isRequired = item.isRequired,
                            orderIndex = globalOrder++
                        )
                    )
                }
            }

            val templateId = repository.createTemplate(newTemplate, fieldsToInsert)

            _selectedTab.value = "MEUS_CHECKLISTS"
            onImported(templateId)
        }
    }
}
