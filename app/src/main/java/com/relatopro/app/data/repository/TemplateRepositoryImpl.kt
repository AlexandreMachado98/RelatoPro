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
        // 1. Inspeção de Extintores de Incêndio
        if (dao.getTemplateByName("Inspeção de Extintores de Incêndio") == null) {
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
        }

        // 2. Inspeção de EPI (Equipamentos de Proteção Individual)
        if (dao.getTemplateByName("Inspeção de EPI") == null) {
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
        }

        // 3. Vistoria Predial e Instalações Elétricas
        if (dao.getTemplateByName("Vistoria Predial e Instalações Elétricas") == null) {
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

        // 4. Inspeção de Segurança — Caminhão Comboio
        if (dao.getTemplateByName("Inspeção de Segurança — Caminhão Comboio") == null) {
            val comboioTemplate = TemplateEntity(
                name = "Inspeção de Segurança — Caminhão Comboio",
                description = "Avaliação completa de segurança, equipamentos de abastecimento, lubrificação, emergência e conformidade operacional de caminhão comboio.",
                category = "Veículos e Equipamentos",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                status = "ACTIVE",
                userId = "",
                isGlobal = true
            )
            val comboioId = dao.insertTemplate(comboioTemplate)
            val comboioFields = listOf(
                // Categoria A: Documentação e Preparação
                TemplateFieldEntity(templateId = comboioId, category = "A. Documentação e Preparação", label = "CNH do condutor válida e compatível com a categoria do veículo", type = "C_NC_NA", orderIndex = 0, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "A. Documentação e Preparação", label = "Curso MOPP (Movimentação Operacional de Produtos Perigosos) válido", type = "C_NC_NA", orderIndex = 1, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "A. Documentação e Preparação", label = "CRLV do veículo regularizado e dentro do prazo de vigência", type = "C_NC_NA", orderIndex = 2, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "A. Documentação e Preparação", label = "CIV (Certificado de Inspeção Veicular) válido e portado", type = "C_NC_NA", orderIndex = 3, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "A. Documentação e Preparação", label = "CIPP (Certificado de Inspeção para Transporte de Produtos Perigosos) válido", type = "C_NC_NA", orderIndex = 4, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "A. Documentação e Preparação", label = "Ficha de Emergência e Envelope para Transporte presentes na cabine", type = "C_NC_NA", orderIndex = 5, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "A. Documentação e Preparação", label = "Ordem de Serviço / Permissão de Trabalho (PT) emitida e assinada", type = "C_NC_NA", orderIndex = 6, isRequired = true),

                // Categoria B: Condições do Veículo
                TemplateFieldEntity(templateId = comboioId, category = "B. Condições do Veículo", label = "Pneus em bom estado, calibrados e com profundidade de sulco adequada (sem recape na dianteira)", type = "C_NC_NA", orderIndex = 7, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "B. Condições do Veículo", label = "Sistema de freios (serviço e estacionamento) operando com eficiência", type = "C_NC_NA", orderIndex = 8, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "B. Condições do Veículo", label = "Faróis, lanternas, setas, luzes de freio e ré em perfeito funcionamento", type = "C_NC_NA", orderIndex = 9, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "B. Condições do Veículo", label = "Alarme sonoro de marcha à ré e giroflex/sinalizador visual operantes", type = "C_NC_NA", orderIndex = 10, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "B. Condições do Veículo", label = "Espelhos retrovisores, vidros e para-brisa íntegros e sem trincas", type = "C_NC_NA", orderIndex = 11, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "B. Condições do Veículo", label = "Limpadores e lavadores de para-brisa funcionando corretamente", type = "C_NC_NA", orderIndex = 12, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "B. Condições do Veículo", label = "Cintos de segurança de 3 pontos em perfeito estado para todos os ocupantes", type = "C_NC_NA", orderIndex = 13, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "B. Condições do Veículo", label = "Buzina e painel de instrumentos com indicadores funcionando", type = "C_NC_NA", orderIndex = 14, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "B. Condições do Veículo", label = "Bateria fixada, polos protegidos e chave geral elétrica operante", type = "C_NC_NA", orderIndex = 15, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "B. Condições do Veículo", label = "Para-choque traseiro e protetores laterais em conformidade técnica", type = "C_NC_NA", orderIndex = 16, isRequired = true),

                // Categoria C: Tanques e Armazenamento
                TemplateFieldEntity(templateId = comboioId, category = "C. Tanques e Armazenamento", label = "Tanque de combustível e compartimentos sem vazamentos, trincas ou deformações", type = "C_NC_NA", orderIndex = 17, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "C. Tanques e Armazenamento", label = "Tampas de inspeção, bocais de enchimento e válvulas vedando perfeitamente", type = "C_NC_NA", orderIndex = 18, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "C. Tanques e Armazenamento", label = "Válvulas de alívio de pressão/vácuo funcionando adequadamente", type = "C_NC_NA", orderIndex = 19, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "C. Tanques e Armazenamento", label = "Placas de identificação de produto perigoso e painéis de segurança (número ONU e risco)", type = "C_NC_NA", orderIndex = 20, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "C. Tanques e Armazenamento", label = "Sistema de aterramento estático com cabo e garra em bom estado", type = "C_NC_NA", orderIndex = 21, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "C. Tanques e Armazenamento", label = "Escada de acesso ao topo do tanque e passadiço com piso antiderrapante e guarda-corpo", type = "C_NC_NA", orderIndex = 22, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "C. Tanques e Armazenamento", label = "Válvula de fundo com acionamento de emergência operando corretamente", type = "C_NC_NA", orderIndex = 23, isRequired = true),

                // Categoria D: Abastecimento e Transferência
                TemplateFieldEntity(templateId = comboioId, category = "D. Abastecimento e Transferência", label = "Bomba de abastecimento operando sem ruídos anormais ou vazamentos", type = "C_NC_NA", orderIndex = 24, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "D. Abastecimento e Transferência", label = "Mangueiras de abastecimento íntegras, sem ressecamento, dobras ou fissuras", type = "C_NC_NA", orderIndex = 25, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "D. Abastecimento e Transferência", label = "Bicos de abastecimento automáticos com trava e vedação em perfeito estado", type = "C_NC_NA", orderIndex = 26, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "D. Abastecimento e Transferência", label = "Carretéis de recolhimento de mangueira com trava e retorno funcionando", type = "C_NC_NA", orderIndex = 27, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "D. Abastecimento e Transferência", label = "Medidores de vazão (volumétricos/digitais) aferidos e legíveis", type = "C_NC_NA", orderIndex = 28, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "D. Abastecimento e Transferência", label = "Sistema de bloqueio automático contra sobreabastecimento operante", type = "C_NC_NA", orderIndex = 29, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "D. Abastecimento e Transferência", label = "Conexões e acoplamentos rápidos (engate rápido) íntegros e com travas", type = "C_NC_NA", orderIndex = 30, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "D. Abastecimento e Transferência", label = "Ponto de aterramento conectado antes do início de qualquer abastecimento", type = "C_NC_NA", orderIndex = 31, isRequired = true),

                // Categoria E: Lubrificação e Equipamentos Auxiliares
                TemplateFieldEntity(templateId = comboioId, category = "E. Lubrificação e Equipamentos Auxiliares", label = "Bombas pneumáticas de graxa e óleo lubrificante sem vazamentos", type = "C_NC_NA", orderIndex = 32, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "E. Lubrificação e Equipamentos Auxiliares", label = "Mangueiras e propulsoras de graxa em bom estado e com bicos adequados", type = "C_NC_NA", orderIndex = 33, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "E. Lubrificação e Equipamentos Auxiliares", label = "Reservatórios de óleo lubrificante novo e usado identificados e vedados", type = "C_NC_NA", orderIndex = 34, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "E. Lubrificação e Equipamentos Auxiliares", label = "Compressor de ar auxiliar com manômetro e válvula de segurança calibrada", type = "C_NC_NA", orderIndex = 35, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "E. Lubrificação e Equipamentos Auxiliares", label = "Válvulas de retenção e drenagem do sistema de ar comprimido operantes", type = "C_NC_NA", orderIndex = 36, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "E. Lubrificação e Equipamentos Auxiliares", label = "Bacia de contenção sob bombas e carretéis limpa e sem acúmulo de óleo", type = "C_NC_NA", orderIndex = 37, isRequired = true),

                // Categoria F: Prevenção e Resposta a Emergências
                TemplateFieldEntity(templateId = comboioId, category = "F. Prevenção e Resposta a Emergências", label = "Extintores de pó químico (mínimo 8kg/ABC ou BC) carregados, válidos e lacrados", type = "C_NC_NA", orderIndex = 38, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "F. Prevenção e Resposta a Emergências", label = "Extintor adicional na cabine ao alcance imediato do condutor", type = "C_NC_NA", orderIndex = 39, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "F. Prevenção e Resposta a Emergências", label = "Kit de mitigação de vazamento (manta absorvente, cordão, turfa) completo", type = "C_NC_NA", orderIndex = 40, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "F. Prevenção e Resposta a Emergências", label = "Cones de sinalização e fita zebrada presentes para isolamento de área", type = "C_NC_NA", orderIndex = 41, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "F. Prevenção e Resposta a Emergências", label = "Pá e enxada antifaiscante presentes no conjunto de emergência", type = "C_NC_NA", orderIndex = 42, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "F. Prevenção e Resposta a Emergências", label = "Balde metálico/plástico antiestático para contenção e recolhimento", type = "C_NC_NA", orderIndex = 43, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "F. Prevenção e Resposta a Emergências", label = "Lava-olhos portátil ou frasco com solução fisiológica disponível e limpo", type = "C_NC_NA", orderIndex = 44, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "F. Prevenção e Resposta a Emergências", label = "EPIs específicos: macacão impermeável, luvas nitrílicas, óculos ampla visão e máscara com filtro", type = "C_NC_NA", orderIndex = 45, isRequired = true),

                // Categoria G: Organização e Meio Ambiente
                TemplateFieldEntity(templateId = comboioId, category = "G. Organização e Meio Ambiente", label = "Cabine limpa, organizada e isenta de materiais soltos ou inflamáveis", type = "C_NC_NA", orderIndex = 46, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "G. Organização e Meio Ambiente", label = "Compartimento de ferramentas e acessórios limpo e organizado", type = "C_NC_NA", orderIndex = 47, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "G. Organização e Meio Ambiente", label = "Ausência de gotejamentos ou manchas de óleo no chassi, motor e solo", type = "C_NC_NA", orderIndex = 48, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "G. Organização e Meio Ambiente", label = "Tambor/recipiente para descarte de panos contaminados devidamente rotulado", type = "C_NC_NA", orderIndex = 49, isRequired = true),
                TemplateFieldEntity(templateId = comboioId, category = "G. Organização e Meio Ambiente", label = "Sinalização \"PROIBIDO FUMAR\" e \"LÍQUIDO INFLAMÁVEL\" legível e visível", type = "C_NC_NA", orderIndex = 50, isRequired = true)
            )
            dao.insertFields(comboioFields)
        }
    }
}
