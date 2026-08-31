package com.relatopro.app.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.relatopro.app.data.local.entity.ReportAnswerEntity
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.domain.repository.ReportRepository
import com.relatopro.app.domain.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class CategoryIndicator(
    val categoryName: String,
    val totalItems: Int,
    val conformeCount: Int,
    val naoConformeCount: Int,
    val naCount: Int,
    val compliancePercent: Float
)

data class NonConformityItem(
    val title: String,
    val category: String,
    val count: Int,
    val percentOfTotalNC: Float
)

data class TimePoint(
    val label: String,
    val dateMillis: Long,
    val compliancePercent: Float,
    val totalReports: Int
)

data class IndicatorsUiState(
    val isLoading: Boolean = true,
    val selectedPeriod: String = "30_DIAS", // HOJE, 7_DIAS, 30_DIAS, ESTE_MES, TODOS
    val totalReports: Int = 0,
    val completedReports: Int = 0,
    val draftReports: Int = 0,
    val totalEvaluatedItems: Int = 0,
    val totalConforme: Int = 0,
    val totalNaoConforme: Int = 0,
    val totalNA: Int = 0,
    val compliancePercent: Float? = null,
    val nonCompliancePercent: Float? = null,
    val naPercent: Float? = null,
    val generalStatus: String = "Sem Dados", // Excelente, Bom, Atenção, Crítico
    val categories: List<CategoryIndicator> = emptyList(),
    val topNonConformities: List<NonConformityItem> = emptyList(),
    val timePoints: List<TimePoint> = emptyList()
)

@HiltViewModel
class IndicatorsViewModel @Inject constructor(
    application: Application,
    private val reportRepository: ReportRepository,
    private val templateRepository: TemplateRepository
) : AndroidViewModel(application) {

    private val _selectedPeriod = MutableStateFlow("30_DIAS")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _uiState = MutableStateFlow(IndicatorsUiState())
    val uiState: StateFlow<IndicatorsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                reportRepository.getAllReports(),
                reportRepository.getAllAnswers(),
                _selectedPeriod
            ) { reports, answers, period ->
                calculateIndicators(reports, answers, period)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setPeriod(period: String) {
        _selectedPeriod.value = period
    }

    private suspend fun calculateIndicators(
        allReports: List<ReportEntity>,
        allAnswers: List<ReportAnswerEntity>,
        period: String
    ): IndicatorsUiState {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        val minTime = when (period) {
            "HOJE" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            "7_DIAS" -> now - (7L * 24 * 60 * 60 * 1000)
            "30_DIAS" -> now - (30L * 24 * 60 * 60 * 1000)
            "ESTE_MES" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            else -> 0L
        }

        val filteredReports = allReports.filter { it.date >= minTime }
        val filteredReportIds = filteredReports.map { it.id }.toSet()
        val filteredAnswers = allAnswers.filter { it.reportId in filteredReportIds }

        val totalReports = filteredReports.size
        val completedReports = filteredReports.count { it.status == "COMPLETED" || it.status == "FINALIZED" }
        val draftReports = filteredReports.count { it.status == "DRAFT" || it.status == "PENDING" }

        var cCount = 0
        var ncCount = 0
        var naCount = 0

        val categoryStats = mutableMapOf<String, Triple<Int, Int, Int>>() // Cat -> (C, NC, NA)
        val ncItemCounts = mutableMapOf<String, Pair<String, Int>>() // ItemLabel -> (Category, Count)

        // Cache template fields for fast lookup
        val allFieldsList = mutableMapOf<Long, com.relatopro.app.data.local.entity.TemplateFieldEntity>()
        val templateIds = filteredReports.map { it.templateId }.distinct()
        for (tId in templateIds) {
            templateRepository.getTemplateFieldsList(tId).forEach { f ->
                allFieldsList[f.id] = f
            }
        }

        for (ans in filteredAnswers) {
            val field = allFieldsList[ans.templateFieldId]
            val cat = field?.category?.ifBlank { "Geral" } ?: "Geral"
            val label = field?.label ?: "Item #${ans.templateFieldId}"
            val currentCat = categoryStats[cat] ?: Triple(0, 0, 0)

            val norm = when (ans.answerValue?.trim()?.uppercase()) {
                "C", "CONFORME", "TRUE", "SIM" -> "C"
                "NC", "NÃO CONFORME", "NAO CONFORME", "FALSE", "NÃO", "NAO" -> "NC"
                else -> "NA"
            }

            when (norm) {
                "C" -> {
                    cCount++
                    categoryStats[cat] = currentCat.copy(first = currentCat.first + 1)
                }
                "NC" -> {
                    ncCount++
                    categoryStats[cat] = currentCat.copy(second = currentCat.second + 1)
                    val prevItem = ncItemCounts[label] ?: Pair(cat, 0)
                    ncItemCounts[label] = Pair(cat, prevItem.second + 1)
                }
                else -> {
                    naCount++
                    categoryStats[cat] = currentCat.copy(third = currentCat.third + 1)
                }
            }
        }

        val totalEvaluated = cCount + ncCount + naCount
        val applicable = cCount + ncCount

        val compliancePercent = if (applicable > 0) (cCount.toFloat() / applicable.toFloat() * 100f) else null
        val ncPercent = if (applicable > 0) (ncCount.toFloat() / applicable.toFloat() * 100f) else null
        val naPercent = if (totalEvaluated > 0) (naCount.toFloat() / totalEvaluated.toFloat() * 100f) else null

        val generalStatus = when {
            compliancePercent == null -> "Sem Dados"
            compliancePercent >= 90f -> "Excelente"
            compliancePercent >= 80f -> "Bom"
            compliancePercent >= 70f -> "Atenção"
            else -> "Crítico"
        }

        // Category breakdown
        val categoryList = categoryStats.map { (cat, counts) ->
            val catApp = counts.first + counts.second
            val catComp = if (catApp > 0) (counts.first.toFloat() / catApp.toFloat() * 100f) else 100f
            CategoryIndicator(
                categoryName = cat,
                totalItems = counts.first + counts.second + counts.third,
                conformeCount = counts.first,
                naoConformeCount = counts.second,
                naCount = counts.third,
                compliancePercent = catComp
            )
        }.sortedByDescending { it.naoConformeCount }

        // Top Non-Conformities Ranking
        val topNCList = ncItemCounts.map { (label, pair) ->
            val count = pair.second
            val pct = if (ncCount > 0) (count.toFloat() / ncCount.toFloat() * 100f) else 0f
            NonConformityItem(
                title = label,
                category = pair.first,
                count = count,
                percentOfTotalNC = pct
            )
        }.sortedByDescending { it.count }.take(5)

        // Time Series (Evolution)
        val df = SimpleDateFormat("dd/MM", Locale.getDefault())
        val groupedByDate = filteredReports.groupBy { df.format(Date(it.date)) }
        val timePoints = groupedByDate.map { (dateLabel, reps) ->
            val repIds = reps.map { it.id }.toSet()
            val repAnswers = filteredAnswers.filter { it.reportId in repIds }
            var rC = 0
            var rNC = 0
            repAnswers.forEach {
                when (it.answerValue?.trim()?.uppercase()) {
                    "C", "CONFORME", "TRUE", "SIM" -> rC++
                    "NC", "NÃO CONFORME", "NAO CONFORME", "FALSE", "NÃO", "NAO" -> rNC++
                }
            }
            val rApp = rC + rNC
            val comp = if (rApp > 0) (rC.toFloat() / rApp.toFloat() * 100f) else 100f
            TimePoint(
                label = dateLabel,
                dateMillis = reps.firstOrNull()?.date ?: 0L,
                compliancePercent = comp,
                totalReports = reps.size
            )
        }.sortedBy { it.dateMillis }

        return IndicatorsUiState(
            isLoading = false,
            selectedPeriod = period,
            totalReports = totalReports,
            completedReports = completedReports,
            draftReports = draftReports,
            totalEvaluatedItems = totalEvaluated,
            totalConforme = cCount,
            totalNaoConforme = ncCount,
            totalNA = naCount,
            compliancePercent = compliancePercent,
            nonCompliancePercent = ncPercent,
            naPercent = naPercent,
            generalStatus = generalStatus,
            categories = categoryList,
            topNonConformities = topNCList,
            timePoints = timePoints
        )
    }
}
