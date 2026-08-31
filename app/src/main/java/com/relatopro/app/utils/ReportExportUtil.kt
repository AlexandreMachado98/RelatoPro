package com.relatopro.app.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.relatopro.app.data.local.entity.ReportAnswerEntity
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.data.local.entity.TemplateFieldEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExportUtil {

    suspend fun generateReportsCsv(
        context: Context,
        reports: List<ReportEntity>,
        answers: List<ReportAnswerEntity>,
        fieldsMap: Map<Long, TemplateFieldEntity> = emptyMap()
    ): File? = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(exportDir, "relatopro_relatorios_$timestamp.csv")

            FileOutputStream(file).use { fos ->
                // Write UTF-8 BOM so Excel opens with correct accents
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    // Header
                    writer.append("ID;Numero_Laudo;Titulo;Data;Responsavel;Local;Status;Total_Itens;Conformes_C;NaoConformes_NC;NaoAplicaveis_NA;Conformidade_Pct\n")

                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val answersByReport = answers.groupBy { it.reportId }

                    for (report in reports) {
                        val reportAnswers = answersByReport[report.id] ?: emptyList()
                        var c = 0
                        var nc = 0
                        var na = 0

                        for (ans in reportAnswers) {
                            when (ans.answerValue?.trim()?.uppercase()) {
                                "C", "CONFORME", "TRUE", "SIM" -> c++
                                "NC", "NÃO CONFORME", "NAO CONFORME", "FALSE", "NÃO", "NAO" -> nc++
                                else -> na++
                            }
                        }

                        val total = c + nc + na
                        val app = c + nc
                        val compPct = if (app > 0) String.format(Locale.getDefault(), "%.1f%%", (c.toFloat() / app.toFloat() * 100f)) else "N/A"
                        val dateStr = sdf.format(Date(report.date))
                        val cleanTitle = report.title.replace(";", ",")
                        val cleanResp = report.responsible.replace(";", ",")
                        val cleanLoc = report.location.replace(";", ",")

                        writer.append("${report.id};")
                        writer.append("${report.reportNumber.ifBlank { "LAUDO-${report.id}" }};")
                        writer.append("\"$cleanTitle\";")
                        writer.append("\"$dateStr\";")
                        writer.append("\"$cleanResp\";")
                        writer.append("\"$cleanLoc\";")
                        writer.append("${report.status};")
                        writer.append("$total;")
                        writer.append("$c;")
                        writer.append("$nc;")
                        writer.append("$na;")
                        writer.append("$compPct\n")
                    }
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareCsvFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Relatório Consolidado Relato Pro")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Exportar Planilha de Relatórios"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
