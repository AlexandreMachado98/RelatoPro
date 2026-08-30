package com.relatopro.app.domain.repository

import com.relatopro.app.data.local.entity.TemplateEntity
import com.relatopro.app.data.local.entity.TemplateFieldEntity
import kotlinx.coroutines.flow.Flow

interface TemplateRepository {
    fun getAllTemplates(): Flow<List<TemplateEntity>>
    fun getTemplateFields(templateId: Long): Flow<List<TemplateFieldEntity>>
    suspend fun getTemplateById(id: Long): TemplateEntity?
    suspend fun createTemplate(template: TemplateEntity, fields: List<TemplateFieldEntity>): Long
}
