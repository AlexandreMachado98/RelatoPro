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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFields(fields: List<TemplateFieldEntity>)

    @Query("SELECT * FROM templates WHERE status != 'ARCHIVED'")
    fun getAllTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM template_fields WHERE templateId = :templateId ORDER BY orderIndex ASC")
    fun getTemplateFields(templateId: Long): Flow<List<TemplateFieldEntity>>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): TemplateEntity?
}
