package com.relatopro.app.data.repository

import com.relatopro.app.data.local.dao.CompanyDao
import com.relatopro.app.data.local.entity.CompanyEntity
import com.relatopro.app.data.local.entity.CorrectiveActionEntity
import com.relatopro.app.domain.repository.CompanyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanyRepositoryImpl @Inject constructor(
    private val companyDao: CompanyDao
) : CompanyRepository {

    override fun getAllCompanies(): Flow<List<CompanyEntity>> = companyDao.getAllCompanies()

    override suspend fun getAllCompaniesList(): List<CompanyEntity> = companyDao.getAllCompaniesList()

    override suspend fun getCompanyById(id: Long): CompanyEntity? = companyDao.getCompanyById(id)

    override fun searchCompanies(query: String): Flow<List<CompanyEntity>> = companyDao.searchCompanies(query)

    override suspend fun createCompany(company: CompanyEntity): Long = companyDao.insertCompany(company)

    override suspend fun updateCompany(company: CompanyEntity) = companyDao.updateCompany(company)

    override suspend fun deleteCompany(company: CompanyEntity) = companyDao.deleteCompany(company)

    override suspend fun deleteCompanyById(id: Long) = companyDao.deleteCompanyById(id)

    override fun getActionsByCompany(companyId: Long): Flow<List<CorrectiveActionEntity>> =
        companyDao.getActionsByCompany(companyId)

    override fun getActionsByReport(reportId: Long): Flow<List<CorrectiveActionEntity>> =
        companyDao.getActionsByReport(reportId)

    override fun getAllActions(): Flow<List<CorrectiveActionEntity>> = companyDao.getAllActions()

    override suspend fun createAction(action: CorrectiveActionEntity): Long = companyDao.insertAction(action)

    override suspend fun updateAction(action: CorrectiveActionEntity) = companyDao.updateAction(action)

    override suspend fun deleteAction(action: CorrectiveActionEntity) = companyDao.deleteAction(action)
}
