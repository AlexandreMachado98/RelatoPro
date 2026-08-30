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

    override fun getTemplatesForUser(userId: String): Flow<List<TemplateEntity>> = dao.getTemplatesForUser(userId)

    override fun getMyChecklists(userId: String): Flow<List<TemplateEntity>> = dao.getMyChecklists(userId)

    override fun getGlobalTemplates(): Flow<List<TemplateEntity>> = dao.getGlobalTemplates()

    override fun getTemplateFields(templateId: Long): Flow<List<TemplateFieldEntity>> = dao.getTemplateFields(templateId)

    override suspend fun getTemplateFieldsList(templateId: Long): List<TemplateFieldEntity> = dao.getTemplateFieldsList(templateId)

    override suspend fun getTemplateById(id: Long): TemplateEntity? = dao.getTemplateById(id)

    override suspend fun createTemplate(template: TemplateEntity, fields: List<TemplateFieldEntity>): Long {
        val id = dao.insertTemplate(template)
        val fieldsWithId = fields.map { it.copy(id = 0, templateId = id) }
        dao.insertFields(fieldsWithId)
        return id
    }

    override suspend fun updateTemplate(template: TemplateEntity, fields: List<TemplateFieldEntity>) {
        dao.updateTemplate(template.copy(updatedAt = System.currentTimeMillis()))
        dao.deleteFieldsByTemplateId(template.id)
        val fieldsWithId = fields.map { it.copy(id = 0, templateId = template.id) }
        dao.insertFields(fieldsWithId)
    }

    override suspend fun duplicateTemplate(templateId: Long, userId: String, newName: String?): Long {
        val original = dao.getTemplateById(templateId) ?: return -1L
        val originalFields = dao.getTemplateFieldsList(templateId)
        
        val copyName = newName ?: "${original.name} (Cópia)"
        val newTemplate = TemplateEntity(
            name = copyName,
            description = original.description,
            category = original.category,
            version = 1,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            status = "ACTIVE",
            visualConfig = original.visualConfig,
            userId = userId,
            isGlobal = false
        )
        
        val newId = dao.insertTemplate(newTemplate)
        val newFields = originalFields.map { it.copy(id = 0, templateId = newId) }
        dao.insertFields(newFields)
        return newId
    }

    override suspend fun deleteTemplate(templateId: Long) {
        dao.deleteFieldsByTemplateId(templateId)
        dao.deleteTemplateById(templateId)
    }

    override suspend fun seedDefaultTemplatesIfEmpty() {
        if (dao.getTemplatesCount() > 0) return

        // 1. Inspeção de Extintores de Incêndio
        val extintorTemplate = TemplateEntity(
            name = "Inspeção de Extintores de Incêndio",
            description = "Verificação de conformidade técnica, validade, carga e sinalização.",
            category = "Proteção contra Incêndio",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            status = "ACTIVE",
            userId = "",
            isGlobal = true
        )
        val extintorId = dao.insertTemplate(extintorTemplate)
        val extintorFields = listOf(
            TemplateFieldEntity(templateId = extintorId, category = "Equipamento", label = "Extintor dentro do prazo de validade da carga e teste hidrostático", type = "C_NC_NA", orderIndex = 0, isRequired = true),
            TemplateFieldEntity(templateId = extintorId, category = "Equipamento", label = "Manômetro com indicador de pressão na faixa verde (pressurizado)", type = "C_NC_NA", orderIndex = 1, isRequired = true),
            TemplateFieldEntity(templateId = extintorId, category = "Segurança", label = "Lacre plástico e pino de segurança inviolados", type = "C_NC_NA", orderIndex = 2, isRequired = true),
            TemplateFieldEntity(templateId = extintorId, category = "Acesso", label = "Acesso livre e totalmente desobstruído no piso", type = "C_NC_NA", orderIndex = 3, isRequired = true),
            TemplateFieldEntity(templateId = extintorId, category = "Sinalização", label = "Placa de sinalização fotoluminescente visível e fixada", type = "C_NC_NA", orderIndex = 4, isRequired = true),
            TemplateFieldEntity(templateId = extintorId, category = "Estrutura", label = "Suporte ou abrigo em bom estado de conservação", type = "C_NC_NA", orderIndex = 5, isRequired = true)
        )
        dao.insertFields(extintorFields)

        // 2. Inspeção de EPI (Equipamentos de Proteção Individual)
        val epiTemplate = TemplateEntity(
            name = "Inspeção de EPI",
            description = "Fiscalização do uso obrigatório de EPIs conforme normas de segurança.",
            category = "Segurança do Trabalho",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            status = "ACTIVE",
            userId = "",
            isGlobal = true
        )
        val epiId = dao.insertTemplate(epiTemplate)
        val epiFields = listOf(
            TemplateFieldEntity(templateId = epiId, category = "Proteção da Cabeça", label = "Uso correto de capacete de segurança com jugular ajustada", type = "C_NC_NA", orderIndex = 0, isRequired = true),
            TemplateFieldEntity(templateId = epiId, category = "Proteção dos Olhos", label = "Óculos de proteção adequados ao risco da operação", type = "C_NC_NA", orderIndex = 1, isRequired = true),
            TemplateFieldEntity(templateId = epiId, category = "Proteção dos Pés", label = "Botinas de segurança com biqueira e solado íntegros", type = "C_NC_NA", orderIndex = 2, isRequired = true),
            TemplateFieldEntity(templateId = epiId, category = "Proteção das Mãos", label = "Luvas de proteção específicas para a tarefa em uso", type = "C_NC_NA", orderIndex = 3, isRequired = true),
            TemplateFieldEntity(templateId = epiId, category = "Proteção Auditiva", label = "Protetor auricular tipo plug ou concha em áreas com ruído", type = "C_NC_NA", orderIndex = 4, isRequired = true)
        )
        dao.insertFields(epiFields)

        // 3. Vistoria Predial e Instalações Elétricas
        val predialTemplate = TemplateEntity(
            name = "Vistoria Predial e Instalações Elétricas",
            description = "Auditoria das condições de segurança de quadros elétricos e rotas de fuga.",
            category = "Infraestrutura",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            status = "ACTIVE",
            userId = "",
            isGlobal = true
        )
        val predialId = dao.insertTemplate(predialTemplate)
        val predialFields = listOf(
            TemplateFieldEntity(templateId = predialId, category = "Elétrica", label = "Quadros de distribuição elétrica identificados e trancados", type = "C_NC_NA", orderIndex = 0, isRequired = true),
            TemplateFieldEntity(templateId = predialId, category = "Elétrica", label = "Ausência de fios expostos, cabos desencapados ou emendas", type = "C_NC_NA", orderIndex = 1, isRequired = true),
            TemplateFieldEntity(templateId = predialId, category = "Emergência", label = "Luminárias de emergência funcionando e testadas", type = "C_NC_NA", orderIndex = 2, isRequired = true),
            TemplateFieldEntity(templateId = predialId, category = "Circulação", label = "Corredores, escadas e rotas de fuga desimpedidos", type = "C_NC_NA", orderIndex = 3, isRequired = true),
            TemplateFieldEntity(templateId = predialId, category = "Estrutura", label = "Pisos nivelados e corrimãos de escadas firmemente fixados", type = "C_NC_NA", orderIndex = 4, isRequired = true)
        )
        dao.insertFields(predialFields)
    }
}
