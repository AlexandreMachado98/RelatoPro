package com.relatopro.app.ui.screens.fieldmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relatopro.app.data.local.entity.ReportAnswerEntity
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.data.local.entity.TemplateFieldEntity
import com.relatopro.app.domain.repository.ReportRepository
import com.relatopro.app.domain.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.relatopro.app.pdf.PdfGenerator

import kotlinx.coroutines.flow.first

@HiltViewModel
class FieldModeViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val templateRepository: TemplateRepository,
    private val pdfGenerator: PdfGenerator
) : ViewModel() {

    private val _currentReport = MutableStateFlow<ReportEntity?>(null)
    val currentReport: StateFlow<ReportEntity?> = _currentReport.asStateFlow()

    private val _fields = MutableStateFlow<List<TemplateFieldEntity>>(emptyList())
    val fields: StateFlow<List<TemplateFieldEntity>> = _fields.asStateFlow()

    private val _answers = MutableStateFlow<Map<Long, ReportAnswerEntity>>(emptyMap())
    val answers: StateFlow<Map<Long, ReportAnswerEntity>> = _answers.asStateFlow()

    // 1. Inicializa o relatório a partir de um Modelo Existente (Req: Fluxo de Inicialização)
    fun initializeReportFromTemplate(templateId: Long, location: String, responsible: String) {
        viewModelScope.launch {
            // Verifica se o template existe (Evita Crash de Foreign Key no SQLite)
            var template = templateRepository.getTemplateById(templateId)
            if (template == null) {
                // Cria um mock para o MVP não fechar
                val mockTemplate = com.relatopro.app.data.local.entity.TemplateEntity(
                    id = templateId,
                    name = "Inspeção Veicular (Mock)",
                    description = "Gerado automaticamente para testes",
                    category = "Geral",
                    version = 1,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    status = "ACTIVE",
                    visualConfig = "{}"
                )
                val mockFields = listOf(
                    com.relatopro.app.data.local.entity.TemplateFieldEntity(
                        templateId = templateId, orderIndex = 1, label = "Pneus dianteiros", type = "C_NC_NA", category = "Exterior", isRequired = true
                    ),
                    com.relatopro.app.data.local.entity.TemplateFieldEntity(
                        templateId = templateId, orderIndex = 2, label = "Pneus traseiros", type = "C_NC_NA", category = "Exterior", isRequired = true
                    )
                )
                templateRepository.createTemplate(mockTemplate, mockFields)
            }

            // O uso do 'first()' evita que o collect rode infinitamente recriando relatórios
            val templateFields = templateRepository.getTemplateFields(templateId).first()
            _fields.value = templateFields
            
            val newReport = ReportEntity(
                templateId = templateId,
                title = "Relatório " + System.currentTimeMillis().toString().takeLast(4),
                reportNumber = "REP-${System.currentTimeMillis()}",
                date = System.currentTimeMillis(),
                responsible = responsible,
                location = location,
                lat = null,
                lng = null,
                status = "DRAFT",
                generalObservations = "",
                pdfLocalPath = null,
                syncStatus = "PENDING"
            )
            
            val reportId = reportRepository.createReport(newReport)
            _currentReport.value = newReport.copy(id = reportId)
        }
    }

    // 2. Lógica de Auto-save Progressivo e Imediato (Req 28 e 29 - Prevenção de Perda de Dados)
    fun updateAnswer(fieldId: Long, answerValue: String?, observation: String?) {
        val reportId = _currentReport.value?.id ?: return

        viewModelScope.launch {
            val existingAnswer = _answers.value[fieldId]
            
            val newAnswer = ReportAnswerEntity(
                id = existingAnswer?.id ?: 0,
                reportId = reportId,
                templateFieldId = fieldId,
                answerValue = answerValue ?: existingAnswer?.answerValue,
                observation = observation ?: existingAnswer?.observation,
                status = "VALID"
            )

            reportRepository.saveAnswer(newAnswer)
            
            val updatedMap = _answers.value.toMutableMap()
            updatedMap[fieldId] = newAnswer
            _answers.value = updatedMap
        }
    }

    // Marca como FINALIZED impedindo edições (Req 15) e Gera o PDF
    fun finalizeReport(onPdfGenerated: () -> Unit) {
        val report = _currentReport.value ?: return
        viewModelScope.launch {
            // Gera o PDF físico
            val pdfFile = try {
                pdfGenerator.generateReportPdf(
                    report = report,
                    fields = _fields.value,
                    answers = _answers.value.values.toList(),
                    photos = emptyMap() // Busca de fotos via DB poderia entrar aqui
                )
            } catch (e: Exception) {
                null
            }

            // Atualiza o relatório no banco de dados com status Finalizado e o caminho do PDF
            val finalized = report.copy(
                status = "FINALIZED",
                pdfLocalPath = pdfFile?.absolutePath
            )
            reportRepository.updateReport(finalized)
            _currentReport.value = finalized
            
            onPdfGenerated()
        }
    }

    // Lógica para salvar foto otimizada (Req 8 e 9)
    fun savePhoto(fieldId: Long, localPath: String) {
        val reportId = _currentReport.value?.id ?: return
        viewModelScope.launch {
            val photoEntity = com.relatopro.app.data.local.entity.PhotoEntity(
                reportId = reportId,
                templateFieldId = fieldId,
                localPath = localPath,
                timestamp = System.currentTimeMillis(),
                description = "",
                lat = null,
                lng = null
            )
            reportRepository.savePhoto(photoEntity)
            // Na vida real, atualizaríamos um StateFlow de fotos aqui para a UI refletir
        }
    }
}
