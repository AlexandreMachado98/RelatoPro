package com.relatopro.app.domain.repository

import com.relatopro.app.data.local.entity.TemplateEntity
import com.relatopro.app.data.local.entity.TemplateFieldEntity
import kotlinx.coroutines.flow.Flow

interface TemplateRepository {
    fun getAllTemplates(): Flow<List<TemplateEntity>>
    fun getTemplatesForUser(userId: String): Flow<List<TemplateEntity>>
    fun getMyChecklists(userId: String): Flow<List<TemplateEntity>>
    fun getGlobalTemplates(): Flow<List<TemplateEntity>>
    fun getTemplateFields(templateId: Long): Flow<List<TemplateFieldEntity>>
    suspend fun getTemplateFieldsList(templateId: Long): List<TemplateFieldEntity>
    suspend fun getTemplateById(id: Long): TemplateEntity?
    suspend fun createTemplate(template: TemplateEntity, fields: List<TemplateFieldEntity>): Long
    suspend fun updateTemplate(template: TemplateEntity, fields: List<TemplateFieldEntity>)
    suspend fun duplicateTemplate(templateId: Long, userId: String, newName: String? = null): Long
    suspend fun deleteTemplate(templateId: Long)
    suspend fun seedDefaultTemplatesIfEmpty()
}
