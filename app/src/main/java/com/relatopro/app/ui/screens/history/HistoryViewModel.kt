package com.relatopro.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.domain.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val reportRepository: ReportRepository
) : ViewModel() {

    val reports: StateFlow<List<ReportEntity>> = reportRepository.getAllReports()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteReport(report: ReportEntity) {
        viewModelScope.launch {
            reportRepository.deleteReport(report)
        }
    }

    fun markAsSent(reportId: Long) {
        viewModelScope.launch {
            val report = reports.value.find { it.id == reportId }
            if (report != null && report.status != "SENT") {
                reportRepository.updateReport(report.copy(status = "SENT"))
            }
        }
    }
}
