package com.relatopro.app.domain.repository

import com.relatopro.app.data.local.entity.PhotoEntity
import com.relatopro.app.data.local.entity.ReportAnswerEntity
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.data.local.entity.SignatureEntity
import kotlinx.coroutines.flow.Flow

interface ReportRepository {
    fun getAllReports(): Flow<List<ReportEntity>>
    suspend fun getReportById(reportId: Long): ReportEntity?
    suspend fun createReport(report: ReportEntity): Long
    suspend fun updateReport(report: ReportEntity)
    suspend fun saveAnswer(answer: ReportAnswerEntity): Long
    fun getReportAnswers(reportId: Long): Flow<List<ReportAnswerEntity>>
    suspend fun savePhoto(photo: PhotoEntity): Long
    fun getReportPhotos(reportId: Long): Flow<List<PhotoEntity>>
    fun getAllPhotos(): Flow<List<PhotoEntity>>
    suspend fun saveSignature(signature: SignatureEntity): Long
    suspend fun deleteSignatureByRole(reportId: Long, role: String)
    suspend fun getSignature(reportId: Long): SignatureEntity?
    suspend fun getSignatures(reportId: Long): List<SignatureEntity>
    suspend fun deleteReport(report: ReportEntity)
}
