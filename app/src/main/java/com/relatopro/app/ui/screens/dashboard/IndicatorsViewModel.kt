package com.relatopro.app.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.relatopro.app.data.local.entity.CompanyEntity
import com.relatopro.app.data.local.entity.CorrectiveActionEntity
import com.relatopro.app.data.local.entity.ReportAnswerEntity
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.domain.repository.CompanyRepository
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

data class CompanyRankingItem(
    val companyId: Long?,
    val companyName: String,
    val unit: String,
    val totalReports: Int,
    val totalItems: Int,
    val conformeCount: Int,
    val naoConformeCount: Int,
    val compliancePercent: Float,
    val ncPercent: Float,
    val prevCompliancePercent: Float?,
    val variationPp: Float?,
    val trend: String, // "Melhorando", "Estável", "Piorando", "Dados Insuficientes"
    val statusLevel: String // "Excelente", "Bom", "Atenção", "Crítico"
)

data class RecurrentNcItem(
    val itemLabel: String,
    val category: String,
    val companyName: String,
    val recurrenceCount: Int,
    val affectedReports: List<String>
)

data class IndicatorsUiState(
    val isLoading: Boolean = true,
    val selectedPeriod: String = "30_DIAS", // HOJE, 7_DIAS, 30_DIAS, ESTE_MES, TODOS
    val selectedCompanyId: Long? = null,
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
    
    // Executive BI
    val totalInspectedCompanies: Int = 0,
    val companiesImprovingCount: Int = 0,
    val companiesStableCount: Int = 0,
    val companiesWorseningCount: Int = 0,
    val companiesAttentionCount: Int = 0,
    val companyRankings: List<CompanyRankingItem> = emptyList(),
    val topImprovingCompanies: List<CompanyRankingItem> = emptyList(),
    val topWorseningCompanies: List<CompanyRankingItem> = emptyList(),
    val recurrentNonConformities: List<RecurrentNcItem> = emptyList(),

    // Actions
    val totalActions: Int = 0,
    val pendingActions: Int = 0,
    val resolvedActions: Int = 0,
    val actionEffectivenessPercent: Float = 0f,

    // Charts & breakdowns
    val categories: List<CategoryIndicator> = emptyList(),
    val topNonConformities: List<NonConformityItem> = emptyList(),
    val timePoints: List<TimePoint> = emptyList(),
    val availableCompanies: List<CompanyEntity> = emptyList()
)

@HiltViewModel
class IndicatorsViewModel @Inject constructor(
    application: Application,
    private val reportRepository: ReportRepository,
    private val templateRepository: TemplateRepository,
    private val companyRepository: CompanyRepository
) : AndroidViewModel(application) {

    private val _selectedPeriod = MutableStateFlow("30_DIAS")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _selectedCompanyId = MutableStateFlow<Long?>(null)
    val selectedCompanyId: StateFlow<Long?> = _selectedCompanyId.asStateFlow()

    private val _uiState = MutableStateFlow(IndicatorsUiState())
    val uiState: StateFlow<IndicatorsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                reportRepository.getAllReports(),
                reportRepository.getAllAnswers(),
                companyRepository.getAllCompanies(),
                companyRepository.getAllActions(),
                _selectedPeriod,
                _selectedCompanyId
            ) { args: Array<Any?> ->
                @Suppress("UNCHECKED_CAST")
                val reports = args[0] as List<ReportEntity>
                @Suppress("UNCHECKED_CAST")
                val answers = args[1] as List<ReportAnswerEntity>
                @Suppress("UNCHECKED_CAST")
                val companies = args[2] as List<CompanyEntity>
                @Suppress("UNCHECKED_CAST")
                val actions = args[3] as List<CorrectiveActionEntity>
                val period = args[4] as String
                val compId = args[5] as Long?
                calculateIndicators(reports, answers, companies, actions, period, compId)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setPeriod(period: String) {
        _selectedPeriod.value = period
    }

    fun setCompanyFilter(companyId: Long?) {
        _selectedCompanyId.value = companyId
    }

    private suspend fun calculateIndicators(
        allReports: List<ReportEntity>,
        allAnswers: List<ReportAnswerEntity>,
        allCompanies: List<CompanyEntity>,
        allActions: List<CorrectiveActionEntity>,
        period: String,
        filterCompanyId: Long?
    ): IndicatorsUiState {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        val periodDurationMillis = when (period) {
            "HOJE" -> 24L * 60 * 60 * 1000
            "7_DIAS" -> 7L * 24 * 60 * 60 * 1000
            "30_DIAS" -> 30L * 24 * 60 * 60 * 1000
            "ESTE_MES" -> 30L * 24 * 60 * 60 * 1000
            else -> 365L * 24 * 60 * 60 * 1000
        }

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

        val previousPeriodMinTime = minTime - periodDurationMillis

        // Base company filter
        val baseFilteredReports = if (filterCompanyId != null) {
            allReports.filter { it.companyId == filterCompanyId }
        } else {
            allReports
        }

        val currentPeriodReports = baseFilteredReports.filter { it.date >= minTime }
        val previousPeriodReports = baseFilteredReports.filter { it.date in previousPeriodMinTime until minTime }

        val currentReportIds = currentPeriodReports.map { it.id }.toSet()
        val currentAnswers = allAnswers.filter { it.reportId in currentReportIds }

        val totalReports = currentPeriodReports.size
        val completedReports = currentPeriodReports.count { it.status == "COMPLETED" || it.status == "FINALIZED" }
        val draftReports = currentPeriodReports.count { it.status == "DRAFT" || it.status == "PENDING" }

        var cCount = 0
        var ncCount = 0
        var naCount = 0

        val categoryStats = mutableMapOf<String, Triple<Int, Int, Int>>() // Cat -> (C, NC, NA)
        val ncItemCounts = mutableMapOf<String, Pair<String, Int>>() // ItemLabel -> (Category, Count)

        // Cache template fields for fast lookup
        val allFieldsList = mutableMapOf<Long, com.relatopro.app.data.local.entity.TemplateFieldEntity>()
        val templateIds = allReports.map { it.templateId }.distinct()
        for (tId in templateIds) {
            templateRepository.getTemplateFieldsList(tId).forEach { f ->
                allFieldsList[f.id] = f
            }
        }

        for (ans in currentAnswers) {
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

        // Standard Aggregation: C / (C + NC) * 100
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

        // ============================================================
        // COMPANY RANKING & HISTORICAL COMPARISON
        // ============================================================
        val groupedByCompany = baseFilteredReports.groupBy { it.companyId to it.companyName }
        val rankingList = mutableListOf<CompanyRankingItem>()

        var improvingCount = 0
        var stableCount = 0
        var worseningCount = 0
        var attentionCount = 0

        for ((compKey, companyReports) in groupedByCompany) {
            val compId = compKey.first
            val compName = compKey.second.ifBlank { "Empresa não informada" }
            val unit = companyReports.firstOrNull()?.unit ?: "Matriz"

            val currCompReports = companyReports.filter { it.date >= minTime }
            val prevCompReports = companyReports.filter { it.date in previousPeriodMinTime until minTime }

            // Current metrics
            val currIds = currCompReports.map { it.id }.toSet()
            val currAns = allAnswers.filter { it.reportId in currIds }
            var compC = 0
            var compNC = 0
            var compNA = 0
            currAns.forEach {
                when (it.answerValue?.trim()?.uppercase()) {
                    "C", "CONFORME", "TRUE", "SIM" -> compC++
                    "NC", "NÃO CONFORME", "NAO CONFORME", "FALSE", "NÃO", "NAO" -> compNC++
                    else -> compNA++
                }
            }
            val compApp = compC + compNC
            val compCompPercent = if (compApp > 0) (compC.toFloat() / compApp.toFloat() * 100f) else 100f
            val compNcPercent = if (compApp > 0) (compNC.toFloat() / compApp.toFloat() * 100f) else 0f

            // Previous metrics
            val prevIds = prevCompReports.map { it.id }.toSet()
            val prevAns = allAnswers.filter { it.reportId in prevIds }
            var pC = 0
            var pNC = 0
            prevAns.forEach {
                when (it.answerValue?.trim()?.uppercase()) {
                    "C", "CONFORME", "TRUE", "SIM" -> pC++
                    "NC", "NÃO CONFORME", "NAO CONFORME", "FALSE", "NÃO", "NAO" -> pNC++
                }
            }
            val pApp = pC + pNC
            val prevCompPercent = if (pApp > 0) (pC.toFloat() / pApp.toFloat() * 100f) else null

            val variationPp = if (prevCompPercent != null) compCompPercent - prevCompPercent else null

            val trend = when {
                variationPp == null && companyReports.size < 2 -> "Dados Insuficientes"
                variationPp != null && variationPp >= 2.0f -> {
                    improvingCount++
                    "Melhorando"
                }
                variationPp != null && variationPp <= -2.0f -> {
                    worseningCount++
                    "Piorando"
                }
                else -> {
                    stableCount++
                    "Estável"
                }
            }

            val statusLevel = when {
                compCompPercent >= 90f -> "Excelente"
                compCompPercent >= 80f -> "Bom"
                compCompPercent >= 70f -> {
                    attentionCount++
                    "Atenção"
                }
                else -> {
                    attentionCount++
                    "Crítico"
                }
            }

            rankingList.add(
                CompanyRankingItem(
                    companyId = compId,
                    companyName = compName,
                    unit = unit,
                    totalReports = currCompReports.size,
                    totalItems = compC + compNC + compNA,
                    conformeCount = compC,
                    naoConformeCount = compNC,
                    compliancePercent = compCompPercent,
                    ncPercent = compNcPercent,
                    prevCompliancePercent = prevCompPercent,
                    variationPp = variationPp,
                    trend = trend,
                    statusLevel = statusLevel
                )
            )
        }

        val sortedRankings = rankingList.sortedByDescending { it.compliancePercent }
        val topImproving = sortedRankings.filter { it.variationPp != null && it.variationPp > 0 }.sortedByDescending { it.variationPp }
        val topWorsening = sortedRankings.filter { it.variationPp != null && it.variationPp < 0 }.sortedBy { it.variationPp }

        // ============================================================
        // RECURRENT NON-CONFORMITIES DETECTION (Same company, multiple reports)
        // ============================================================
        val recurrentNcMap = mutableMapOf<Pair<String, Long?>, MutableList<String>>() // (ItemLabel, CompanyId) -> List of Report Numbers
        
        for (rep in currentPeriodReports) {
            val repAnswers = allAnswers.filter { it.reportId == rep.id }
            for (ans in repAnswers) {
                val isNC = when (ans.answerValue?.trim()?.uppercase()) {
                    "NC", "NÃO CONFORME", "NAO CONFORME", "FALSE", "NÃO", "NAO" -> true
                    else -> false
                }
                if (isNC) {
                    val field = allFieldsList[ans.templateFieldId]
                    val label = field?.label ?: "Item #${ans.templateFieldId}"
                    val key = Pair(label, rep.companyId)
                    val list = recurrentNcMap.getOrPut(key) { mutableListOf() }
                    val reportTag = rep.reportNumber.ifBlank { "#${rep.id}" }
                    if (!list.contains(reportTag)) {
                        list.add(reportTag)
                    }
                }
            }
        }

        val recurrentNcList = recurrentNcMap
            .filter { it.value.size >= 2 }
            .map { (key, repList) ->
                val (label, compId) = key
                val compName = allCompanies.find { it.id == compId }?.name ?: "Empresa não informada"
                val cat = allFieldsList.values.find { it.label == label }?.category?.ifBlank { "Geral" } ?: "Geral"
                RecurrentNcItem(
                    itemLabel = label,
                    category = cat,
                    companyName = compName,
                    recurrenceCount = repList.size,
                    affectedReports = repList
                )
            }.sortedByDescending { it.recurrenceCount }

        // Corrective actions
        val filteredActions = if (filterCompanyId != null) {
            allActions.filter { it.companyId == filterCompanyId }
        } else {
            allActions
        }
        val totalActs = filteredActions.size
        val pendingActs = filteredActions.count { it.status == "PENDING" || it.status == "IN_PROGRESS" }
        val resolvedActs = filteredActions.count { it.status == "RESOLVED" }
        val actEffectiveness = if (totalActs > 0) (resolvedActs.toFloat() / totalActs.toFloat() * 100f) else 100f

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
        val groupedByDate = currentPeriodReports.groupBy { df.format(Date(it.date)) }
        val timePoints = groupedByDate.map { (dateLabel, reps) ->
            val repIds = reps.map { it.id }.toSet()
            val repAnswers = currentAnswers.filter { it.reportId in repIds }
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
            selectedCompanyId = filterCompanyId,
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
            totalInspectedCompanies = groupedByCompany.size,
            companiesImprovingCount = improvingCount,
            companiesStableCount = stableCount,
            companiesWorseningCount = worseningCount,
            companiesAttentionCount = attentionCount,
            companyRankings = sortedRankings,
            topImprovingCompanies = topImproving,
            topWorseningCompanies = topWorsening,
            recurrentNonConformities = recurrentNcList,
            totalActions = totalActs,
            pendingActions = pendingActs,
            resolvedActions = resolvedActs,
            actionEffectivenessPercent = actEffectiveness,
            categories = categoryList,
            topNonConformities = topNCList,
            timePoints = timePoints,
            availableCompanies = allCompanies
        )
    }
}
