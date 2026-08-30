package com.relatopro.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.relatopro.app.data.local.dao.ReportDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.seconds

/**
 * Worker responsável por sincronizar relatórios em segundo plano.
 * O WorkManager garante que isso só será executado quando houver conexão
 * com a internet, sem que o app precise estar aberto.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reportDao: ReportDao,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Busca todos os relatórios que estão finalizados, mas não sincronizados
            val allReports = reportDao.getAllReports().first()
            val pendingReports = allReports.filter { (it.status == "FINALIZED") && (it.syncStatus != "SYNCED") }

            if (pendingReports.isEmpty()) {
                return Result.success()
            }

            for (report in pendingReports) {
                // Aqui entraria a chamada real para o SDK do Google Drive 
                // Ex: googleDriveApi.uploadFile(File(report.pdfLocalPath))
                
                // Simulação de upload (Opcional: Requisito #22)
                val isUploadSuccessful = simulateCloudUpload(report.id)

                if (isUploadSuccessful) {
                    // Atualiza o banco local informando que foi sincronizado
                    val syncedReport = report.copy(syncStatus = "SYNCED")
                    reportDao.updateReport(syncedReport)
                } else {
                    // Falhou temporariamente (Drive sem espaço, timeout)
                    val failedReport = report.copy(syncStatus = "FAILED")
                    reportDao.updateReport(failedReport)
                    return Result.retry()
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Se der erro de rede, o WorkManager reagenda automaticamente
            Result.retry()
        }
    }

    private suspend fun simulateCloudUpload(@Suppress("UNUSED_PARAMETER") reportId: Long): Boolean {
        // Simula o tempo de upload do PDF e fotos
        delay(2.seconds)
        return true
    }
}
