package com.relatopro.app.data.local.dao

import androidx.room.*
import com.relatopro.app.data.local.entity.CompanyEntity
import com.relatopro.app.data.local.entity.CorrectiveActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {

    // Companies
    @Query("SELECT * FROM companies ORDER BY name ASC")
    fun getAllCompanies(): Flow<List<CompanyEntity>>

    @Query("SELECT * FROM companies ORDER BY name ASC")
    suspend fun getAllCompaniesList(): List<CompanyEntity>

    @Query("SELECT * FROM companies WHERE id = :id")
    suspend fun getCompanyById(id: Long): CompanyEntity?

    @Query("SELECT * FROM companies WHERE name LIKE '%' || :query || '%' OR tradeName LIKE '%' || :query || '%' OR cnpj LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchCompanies(query: String): Flow<List<CompanyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompany(company: CompanyEntity): Long

    @Update
    suspend fun updateCompany(company: CompanyEntity)

    @Delete
    suspend fun deleteCompany(company: CompanyEntity)

    @Query("DELETE FROM companies WHERE id = :id")
    suspend fun deleteCompanyById(id: Long)

    // Corrective Actions
    @Query("SELECT * FROM corrective_actions WHERE companyId = :companyId ORDER BY createdAt DESC")
    fun getActionsByCompany(companyId: Long): Flow<List<CorrectiveActionEntity>>

    @Query("SELECT * FROM corrective_actions WHERE reportId = :reportId ORDER BY createdAt DESC")
    fun getActionsByReport(reportId: Long): Flow<List<CorrectiveActionEntity>>

    @Query("SELECT * FROM corrective_actions ORDER BY createdAt DESC")
    fun getAllActions(): Flow<List<CorrectiveActionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: CorrectiveActionEntity): Long

    @Update
    suspend fun updateAction(action: CorrectiveActionEntity)

    @Delete
    suspend fun deleteAction(action: CorrectiveActionEntity)
}
