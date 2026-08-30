package com.relatopro.app.data.repository

import com.relatopro.app.data.local.dao.TemplateDao
import com.relatopro.app.data.local.entity.TemplateEntity
import com.relatopro.app.data.local.entity.TemplateFieldEntity
import com.relatopro.app.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TemplateRepositoryImpl @Inject constructor(
    private val dao: TemplateDao,
) : TemplateRepository {
    override fun getAllTemplates(): Flow<List<TemplateEntity>> = dao.getAllTemplates()

    override fun getTemplateFields(templateId: Long): Flow<List<TemplateFieldEntity>> = dao.getTemplateFields(templateId)

    override suspend fun getTemplateById(id: Long): TemplateEntity? = dao.getTemplateById(id)

    override suspend fun createTemplate(template: TemplateEntity, fields: List<TemplateFieldEntity>): Long {
        val id = dao.insertTemplate(template)
        val fieldsWithId = fields.map { it.copy(templateId = id) }
        dao.insertFields(fieldsWithId)
        return id
    }
}
