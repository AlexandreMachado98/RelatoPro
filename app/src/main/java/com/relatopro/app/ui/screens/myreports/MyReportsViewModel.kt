package com.relatopro.app.ui.screens.myreports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.domain.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyReportsViewModel @Inject constructor(
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _reports = MutableStateFlow<List<ReportEntity>>(emptyList())
    val reports: StateFlow<List<ReportEntity>> = _reports.asStateFlow()

    init {
        loadReports()
    }

    private fun loadReports() {
        viewModelScope.launch {
            reportRepository.getAllReports().collect { reportList ->
                _reports.value = reportList
            }
        }
    }

    fun deleteReport(report: ReportEntity) {
        viewModelScope.launch {
            reportRepository.deleteReport(report)
        }
    }

    fun markAsSent(reportId: Long) {
        viewModelScope.launch {
            val report = _reports.value.find { it.id == reportId }
            if (report != null && report.status != "SENT") {
                val updated = report.copy(status = "SENT")
                reportRepository.updateReport(updated)
                // The flow from repository will automatically update _reports
            }
        }
    }

    fun exportReportsCsv(context: android.content.Context, onComplete: (java.io.File?) -> Unit) {
        viewModelScope.launch {
            val allAnswers = reportRepository.getAllAnswersSync()
            val file = com.relatopro.app.utils.ReportExportUtil.generateReportsCsv(context, _reports.value, allAnswers)
            onComplete(file)
        }
    }

    fun importParsedReports(reportsToImport: List<ReportEntity>, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            var count = 0
            for (r in reportsToImport) {
                reportRepository.createReport(r)
                count++
            }
            onComplete(count)
        }
    }
}
