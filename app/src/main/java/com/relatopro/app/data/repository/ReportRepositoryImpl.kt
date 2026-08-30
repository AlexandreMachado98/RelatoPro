package com.relatopro.app.data.repository

import com.relatopro.app.data.local.dao.ReportDao
import com.relatopro.app.data.local.entity.PhotoEntity
import com.relatopro.app.data.local.entity.ReportAnswerEntity
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.data.local.entity.SignatureEntity
import com.relatopro.app.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ReportRepositoryImpl @Inject constructor(
    private val dao: ReportDao,
) : ReportRepository {
    override fun getAllReports(): Flow<List<ReportEntity>> = dao.getAllReports()
    
    override suspend fun createReport(report: ReportEntity): Long = dao.insertReport(report)
    
    override suspend fun updateReport(report: ReportEntity) = dao.updateReport(report)
    
    override suspend fun saveAnswer(answer: ReportAnswerEntity): Long = dao.insertAnswer(answer)
    
    override fun getReportAnswers(reportId: Long): Flow<List<ReportAnswerEntity>> = dao.getReportAnswers(reportId)
    
    override suspend fun savePhoto(photo: PhotoEntity): Long = dao.insertPhoto(photo)
    
    override fun getReportPhotos(reportId: Long): Flow<List<PhotoEntity>> = dao.getReportPhotos(reportId)
    
    override suspend fun saveSignature(signature: SignatureEntity): Long = dao.insertSignature(signature)
}
