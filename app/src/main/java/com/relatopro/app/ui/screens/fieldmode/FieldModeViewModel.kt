package com.relatopro.app.ui.screens.fieldmode

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relatopro.app.data.local.entity.CompanyEntity
import com.relatopro.app.data.local.entity.PhotoEntity
import com.relatopro.app.data.local.entity.ReportAnswerEntity
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.data.local.entity.SignatureEntity
import com.relatopro.app.data.local.entity.TemplateFieldEntity
import com.relatopro.app.domain.repository.CompanyRepository
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
    private val companyRepository: CompanyRepository,
    private val pdfGenerator: PdfGenerator
) : ViewModel() {

    val companies: StateFlow<List<CompanyEntity>> = companyRepository.getAllCompanies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private val _isAutoSaving = MutableStateFlow(false)
    val isAutoSaving: StateFlow<Boolean> = _isAutoSaving.asStateFlow()

    private val _lastSavedTime = MutableStateFlow<Long>(0L)
    val lastSavedTime: StateFlow<Long> = _lastSavedTime.asStateFlow()

    /**
     * Carrega um rascunho existente para continuar a edição exatamente de onde parou,
     * sem duplicar registros no banco.
     */
    fun loadExistingReport(reportId: Long) {
        viewModelScope.launch {
            val report = reportRepository.getReportById(reportId) ?: return@launch
            _currentReport.value = report

            val templateFields = templateRepository.getTemplateFieldsList(report.templateId)
            _fields.value = templateFields

            // Load existing answers
            val answersList = reportRepository.getReportAnswersSync(reportId)
            _answers.value = answersList.associateBy { it.templateFieldId }

            // Load existing signatures
            val signatures = reportRepository.getSignatures(reportId)
            _inspectorSignature.value = signatures.find {
                it.role == "RESPONSAVEL_RELATORIO" || it.role.startsWith("RESPONSAVEL")
            }
            _operationSignature.value = signatures.find {
                it.role == "PRESENTE_OPERACAO" || it.role.startsWith("PRESENTE")
            }

            // Observe photos for this report
            reportRepository.getReportPhotos(reportId).collect { photoList ->
                _photos.value = photoList
            }
        }
    }

    /**
     * Inicializa um novo relatório caso não esteja editando um rascunho existente.
     */
    fun initializeReportFromTemplate(templateId: Long, userCompany: String, responsible: String) {
        if (_currentReport.value != null && _currentReport.value?.templateId == templateId) {
            return // Already initialized
        }

        viewModelScope.launch {
            val template = templateRepository.getTemplateById(templateId)
            val templateFields = templateRepository.getTemplateFieldsList(templateId)
            _fields.value = templateFields

            val companyList = companyRepository.getAllCompaniesList()
            val firstCompany = companyList.firstOrNull()

            val templateName = template?.name ?: "Vistoria Técnica"
            val newReport = ReportEntity(
                templateId = templateId,
                companyId = firstCompany?.id,
                companyName = firstCompany?.name ?: "Empresa não informada",
                unit = firstCompany?.units?.split(",")?.firstOrNull()?.trim() ?: "Matriz",
                title = "$templateName - " + SimpleDateFormatUtil.currentDateFormatted(),
                reportNumber = "REP-${System.currentTimeMillis().toString().takeLast(6)}",
                date = System.currentTimeMillis(),
                responsible = responsible.ifBlank { "Alexandre Machado" },
                location = "Setor de Produção / Operação",
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

    private fun triggerAutoSaveFeedback() {
        _isAutoSaving.value = true
        _lastSavedTime.value = System.currentTimeMillis()
        viewModelScope.launch {
            kotlinx.coroutines.delay(600)
            _isAutoSaving.value = false
        }
    }

    fun updateReportCompanyAndLocation(
        companyId: Long?,
        companyName: String,
        unit: String,
        location: String,
        responsible: String,
        title: String
    ) {
        val report = _currentReport.value ?: return
        val updated = report.copy(
            companyId = companyId,
            companyName = companyName,
            unit = unit,
            location = location,
            responsible = responsible,
            title = title
        )
        _currentReport.value = updated
        viewModelScope.launch {
            reportRepository.updateReport(updated)
            triggerAutoSaveFeedback()
        }
    }

    fun quickCreateCompany(name: String, unit: String, onCreated: (CompanyEntity) -> Unit) {
        viewModelScope.launch {
            val newCompany = CompanyEntity(
                name = name.trim(),
                units = unit.ifBlank { "Matriz" }
            )
            val id = companyRepository.createCompany(newCompany)
            val created = newCompany.copy(id = id)
            onCreated(created)
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
            triggerAutoSaveFeedback()
        }
    }

    fun updateGeneralObservations(observations: String) {
        val report = _currentReport.value ?: return
        val updated = report.copy(generalObservations = observations)
        _currentReport.value = updated
        viewModelScope.launch {
            reportRepository.updateReport(updated)
            triggerAutoSaveFeedback()
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
            triggerAutoSaveFeedback()
        }
    }

    fun markAllConforme() {
        val reportId = _currentReport.value?.id ?: return
        viewModelScope.launch {
            val currentFields = _fields.value
            val updatedMap = _answers.value.toMutableMap()
            for (field in currentFields) {
                val existing = updatedMap[field.id]
                val newAnswer = ReportAnswerEntity(
                    id = existing?.id ?: 0,
                    reportId = reportId,
                    templateFieldId = field.id,
                    answerValue = "C",
                    observation = existing?.observation ?: "",
                    status = "VALID"
                )
                reportRepository.saveAnswer(newAnswer)
                updatedMap[field.id] = newAnswer
            }
            _answers.value = updatedMap
            triggerAutoSaveFeedback()
        }
    }

    fun savePhoto(templateFieldId: Long?, localPath: String) {
        val reportId = _currentReport.value?.id ?: return
        viewModelScope.launch {
            val photo = PhotoEntity(
                reportId = reportId,
                templateFieldId = templateFieldId,
                localPath = localPath,
                timestamp = System.currentTimeMillis(),
                description = null,
                lat = null,
                lng = null
            )
            reportRepository.savePhoto(photo)
            triggerAutoSaveFeedback()
        }
    }

    fun saveSignature(
        bitmap: Bitmap,
        context: Context,
        signerName: String,
        roleTag: String,
        signerRole: String
    ) {
        val reportId = _currentReport.value?.id ?: return
        viewModelScope.launch {
            val file = File(context.filesDir, "sig_${roleTag}_${reportId}.png")
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()

            val entity = SignatureEntity(
                reportId = reportId,
                name = signerName,
                role = roleTag,
                localPath = file.absolutePath,
                timestamp = System.currentTimeMillis()
            )
            reportRepository.saveSignature(entity)
            if (roleTag == "RESPONSAVEL_RELATORIO") {
                _inspectorSignature.value = entity
            } else {
                _operationSignature.value = entity
            }
            triggerAutoSaveFeedback()
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
            triggerAutoSaveFeedback()
        }
    }

    fun finalizeReport(
        onProgress: ((current: Int, total: Int, stage: String) -> Unit)? = null,
        onPdfGenerated: (PdfGenerator.PdfGenerationResult?) -> Unit
    ) {
        val report = _currentReport.value ?: return
        viewModelScope.launch {
            val photosList = _photos.value
            val photosMap = photosList.groupBy { it.templateFieldId ?: 0L }
                .mapValues { entry -> entry.value.map { it.localPath } }

            val signaturesList = listOfNotNull(
                _inspectorSignature.value,
                _operationSignature.value
            )

            // Previous Report for Delta comparison
            val allReports = reportRepository.getAllReports().first()
            val allCompanyReports = if (report.companyId != null && report.companyId > 0) {
                allReports.filter {
                    it.companyId == report.companyId && it.id != report.id && it.status == "FINALIZED"
                }.sortedByDescending { it.date }
            } else {
                emptyList()
            }

            val previousReport = allCompanyReports.firstOrNull()
            val previousAnswers = if (previousReport != null) {
                reportRepository.getReportAnswersSync(previousReport.id)
            } else {
                emptyList()
            }

            val reportToGenerate = report.copy(status = "FINALIZED")
            val pdfResult = try {
                pdfGenerator.generateReportPdf(
                    report = reportToGenerate,
                    fields = _fields.value,
                    answers = _answers.value.values.toList(),
                    photos = photosMap,
                    signatures = signaturesList,
                    photoEntities = photosList,
                    previousReport = previousReport,
                    previousAnswers = previousAnswers,
                    onProgress = onProgress
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            val finalized = reportToGenerate.copy(
                pdfLocalPath = pdfResult?.file?.absolutePath
            )
            reportRepository.updateReport(finalized)
            _currentReport.value = finalized

            onPdfGenerated(pdfResult)
        }
    }
}

object SimpleDateFormatUtil {
    fun currentDateFormatted(): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
}
