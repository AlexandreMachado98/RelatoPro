package com.relatopro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.relatopro.app.data.local.entity.TemplateEntity
import com.relatopro.app.data.local.entity.TemplateFieldEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TemplateEntity): Long

    @androidx.room.Update
    suspend fun updateTemplate(template: TemplateEntity)

    @androidx.room.Delete
    suspend fun deleteTemplate(template: TemplateEntity)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteTemplateById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFields(fields: List<TemplateFieldEntity>)

    @Query("DELETE FROM template_fields WHERE templateId = :templateId")
    suspend fun deleteFieldsByTemplateId(templateId: Long)

    @Query("SELECT * FROM templates WHERE status != 'ARCHIVED' ORDER BY updatedAt DESC")
    fun getAllTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE status != 'ARCHIVED' AND (userId = :userId OR isGlobal = 1) ORDER BY isGlobal DESC, updatedAt DESC")
    fun getTemplatesForUser(userId: String): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE status != 'ARCHIVED' AND userId = :userId AND isGlobal = 0 ORDER BY updatedAt DESC")
    fun getMyChecklists(userId: String): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE status != 'ARCHIVED' AND isGlobal = 1 ORDER BY updatedAt DESC")
    fun getGlobalTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM template_fields WHERE templateId = :templateId ORDER BY orderIndex ASC")
    fun getTemplateFields(templateId: Long): Flow<List<TemplateFieldEntity>>

    @Query("SELECT * FROM template_fields WHERE templateId = :templateId ORDER BY orderIndex ASC")
    suspend fun getTemplateFieldsList(templateId: Long): List<TemplateFieldEntity>

    @Query("SELECT COUNT(*) FROM template_fields WHERE templateId = :templateId")
    suspend fun getFieldsCount(templateId: Long): Int

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): TemplateEntity?

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun getTemplatesCount(): Int
}
