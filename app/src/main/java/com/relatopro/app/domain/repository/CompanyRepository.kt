package com.relatopro.app.domain.repository

import com.relatopro.app.data.local.entity.CompanyEntity
import com.relatopro.app.data.local.entity.CorrectiveActionEntity
import kotlinx.coroutines.flow.Flow

interface CompanyRepository {
    fun getAllCompanies(): Flow<List<CompanyEntity>>
    suspend fun getAllCompaniesList(): List<CompanyEntity>
    suspend fun getCompanyById(id: Long): CompanyEntity?
    fun searchCompanies(query: String): Flow<List<CompanyEntity>>
    suspend fun createCompany(company: CompanyEntity): Long
    suspend fun updateCompany(company: CompanyEntity)
    suspend fun deleteCompany(company: CompanyEntity)
    suspend fun deleteCompanyById(id: Long)

    // Actions
    fun getActionsByCompany(companyId: Long): Flow<List<CorrectiveActionEntity>>
    fun getActionsByReport(reportId: Long): Flow<List<CorrectiveActionEntity>>
    fun getAllActions(): Flow<List<CorrectiveActionEntity>>
    suspend fun createAction(action: CorrectiveActionEntity): Long
    suspend fun updateAction(action: CorrectiveActionEntity)
    suspend fun deleteAction(action: CorrectiveActionEntity)
}
