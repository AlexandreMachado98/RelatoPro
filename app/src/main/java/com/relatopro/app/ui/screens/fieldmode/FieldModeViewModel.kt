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

@HiltViewModel
class FieldModeViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val templateRepository: TemplateRepository,
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
            // Busca os campos do modelo no banco
            templateRepository.getTemplateFields(templateId).collect { templateFields ->
                _fields.value = templateFields
                
                // Cria o rascunho do relatório
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
                    syncStatus = "PENDING",
                )
                
                // Salva no banco e guarda a referência
                val reportId = reportRepository.createReport(newReport)
                _currentReport.value = newReport.copy(id = reportId)
            }
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
                status = "VALID",
            )

            // Salva no Room Database instantaneamente 
            reportRepository.saveAnswer(newAnswer)
            
            // Atualiza estado local da UI
            val updatedMap = _answers.value.toMutableMap()
            updatedMap[fieldId] = newAnswer
            _answers.value = updatedMap
        }
    }

    // Marca como FINALIZED impedindo edições (Req 15)
    fun finalizeReport() {
        val report = _currentReport.value ?: return
        viewModelScope.launch {
            val finalized = report.copy(status = "FINALIZED")
            reportRepository.updateReport(finalized)
            _currentReport.value = finalized
        }
    }
}
