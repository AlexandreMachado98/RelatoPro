package com.relatopro.app.ui.screens.fieldmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relatopro.app.data.local.entity.PhotoEntity
import com.relatopro.app.data.local.entity.ReportAnswerEntity
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.data.local.entity.SignatureEntity
import com.relatopro.app.data.local.entity.TemplateFieldEntity
import com.relatopro.app.domain.repository.ReportRepository
import com.relatopro.app.domain.repository.TemplateRepository
import com.relatopro.app.pdf.PdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

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

    private val _photos = MutableStateFlow<List<PhotoEntity>>(emptyList())
    val photos: StateFlow<List<PhotoEntity>> = _photos.asStateFlow()

    private val _inspectorSignature = MutableStateFlow<SignatureEntity?>(null)
    val inspectorSignature: StateFlow<SignatureEntity?> = _inspectorSignature.asStateFlow()

    private val _operationSignature = MutableStateFlow<SignatureEntity?>(null)
    val operationSignature: StateFlow<SignatureEntity?> = _operationSignature.asStateFlow()

    fun initializeReportFromTemplate(templateId: Long, location: String, responsible: String) {
        viewModelScope.launch {
            val templateFields = templateRepository.getTemplateFields(templateId).first()
            _fields.value = templateFields
            
            val newReport = ReportEntity(
                templateId = templateId,
                title = "Inspeção " + SimpleDateFormatUtil.currentDateFormatted(),
                reportNumber = "REP-${System.currentTimeMillis().toString().takeLast(6)}",
                date = System.currentTimeMillis(),
                responsible = responsible.ifEmpty { "João da Silva" },
                location = location.ifEmpty { "Indústria ABC Lda." },
                lat = null,
                lng = null,
                status = "DRAFT",
                generalObservations = "",
                pdfLocalPath = null,
                syncStatus = "PENDING"
            )
            
            val reportId = reportRepository.createReport(newReport)
            val created = newReport.copy(id = reportId)
            _currentReport.value = created

            // Collect photos for this report
            reportRepository.getReportPhotos(reportId).collect { photoList ->
                _photos.value = photoList
            }
        }
    }

    fun updateReportInfo(title: String, location: String, responsible: String) {
        val report = _currentReport.value ?: return
        val updated = report.copy(
            title = title,
            location = location,
            responsible = responsible
        )
        _currentReport.value = updated
        viewModelScope.launch {
            reportRepository.updateReport(updated)
        }
    }

    fun updateGeneralObservations(observations: String) {
        val report = _currentReport.value ?: return
        val updated = report.copy(generalObservations = observations)
        _currentReport.value = updated
        viewModelScope.launch {
            reportRepository.updateReport(updated)
        }
    }

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

    fun savePhoto(fieldId: Long?, localPath: String, description: String = "") {
        val reportId = _currentReport.value?.id ?: return
        viewModelScope.launch {
            val photoEntity = PhotoEntity(
                reportId = reportId,
                templateFieldId = fieldId,
                localPath = localPath,
                timestamp = System.currentTimeMillis(),
                description = description,
                lat = null,
                lng = null
            )
            reportRepository.savePhoto(photoEntity)
        }
    }

    fun saveSignature(
        bitmap: android.graphics.Bitmap,
        context: android.content.Context,
        name: String,
        roleTag: String = "RESPONSAVEL_RELATORIO",
        roleTitle: String = "Inspetor Técnico"
    ) {
        val reportId = _currentReport.value?.id ?: return
        viewModelScope.launch {
            val file = File(context.filesDir, "signatures/sig_${roleTag}_${reportId}_${System.currentTimeMillis()}.png")
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            val finalName = name.ifBlank {
                if (roleTag == "RESPONSAVEL_RELATORIO") (_currentReport.value?.responsible?.ifBlank { "Inspetor Técnico" } ?: "Inspetor Técnico")
                else "Responsável no Local"
            }
            val finalRoleTitle = roleTitle.ifBlank {
                if (roleTag == "RESPONSAVEL_RELATORIO") "Inspetor Técnico" else "Acompanhante / Supervisor"
            }
            val sigEntity = SignatureEntity(
                reportId = reportId,
                name = finalName,
                role = "${roleTag}#${finalRoleTitle}",
                localPath = file.absolutePath,
                timestamp = System.currentTimeMillis()
            )
            reportRepository.saveSignature(sigEntity)
            if (roleTag == "RESPONSAVEL_RELATORIO") {
                _inspectorSignature.value = sigEntity
            } else {
                _operationSignature.value = sigEntity
            }
        }
    }

    fun clearSignature(roleTag: String) {
        val reportId = _currentReport.value?.id ?: return
        viewModelScope.launch {
            reportRepository.deleteSignatureByRole(reportId, roleTag)
            if (roleTag == "RESPONSAVEL_RELATORIO") {
                _inspectorSignature.value = null
            } else {
                _operationSignature.value = null
            }
        }
    }

    fun finalizeReport(onPdfGenerated: () -> Unit) {
        val report = _currentReport.value ?: return
        viewModelScope.launch {
            val photosList = reportRepository.getReportPhotos(report.id).first()
            val signaturesList = reportRepository.getSignatures(report.id)
            
            val photosMap = photosList
                .filter { it.templateFieldId != null }
                .groupBy { it.templateFieldId!! }
                .mapValues { entry ->
                    entry.value.map { it.localPath }
                }.toMutableMap()

            val reportToGenerate = report.copy(status = "FINALIZED")
            val pdfFile = try {
                pdfGenerator.generateReportPdf(
                    report = reportToGenerate,
                    fields = _fields.value,
                    answers = _answers.value.values.toList(),
                    photos = photosMap,
                    signatures = signaturesList,
                    photoEntities = photosList
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            val finalized = reportToGenerate.copy(
                pdfLocalPath = pdfFile?.absolutePath
            )
            reportRepository.updateReport(finalized)
            _currentReport.value = finalized
            
            onPdfGenerated()
        }
    }
}

object SimpleDateFormatUtil {
    fun currentDateFormatted(): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
}
