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
            val matchesTab = when (tab) {
                "MEUS_CHECKLISTS" -> !t.isGlobal && (t.userId == currentUserEmail || t.userId.isBlank())
                "MODELOS" -> t.isGlobal
                else -> t.isGlobal || t.userId == currentUserEmail || t.userId.isBlank()
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
            repository.seedDefaultTemplatesIfEmpty()
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
}
