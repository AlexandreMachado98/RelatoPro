package com.relatopro.app.ui.screens.companies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relatopro.app.data.local.entity.CompanyEntity
import com.relatopro.app.domain.repository.CompanyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CompaniesViewModel @Inject constructor(
    private val companyRepository: CompanyRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val companies: StateFlow<List<CompanyEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                companyRepository.getAllCompanies()
            } else {
                companyRepository.searchCompanies(query.trim())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveCompany(
        id: Long = 0L,
        name: String,
        tradeName: String,
        cnpj: String,
        segment: String,
        units: List<String>,
        contactName: String,
        contactEmail: String,
        contactPhone: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val unitsString = if (units.isEmpty()) "Matriz" else units.filter { it.isNotBlank() }.joinToString(", ")
            val company = CompanyEntity(
                id = id,
                name = name.trim(),
                tradeName = tradeName.trim(),
                cnpj = cnpj.trim(),
                segment = segment.trim(),
                units = unitsString,
                contactName = contactName.trim(),
                contactEmail = contactEmail.trim(),
                contactPhone = contactPhone.trim(),
                createdAt = if (id == 0L) System.currentTimeMillis() else System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            if (id == 0L) {
                companyRepository.createCompany(company)
            } else {
                companyRepository.updateCompany(company)
            }
            onSuccess()
        }
    }

    fun deleteCompany(company: CompanyEntity) {
        viewModelScope.launch {
            companyRepository.deleteCompany(company)
        }
    }
}
