package com.relatopro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.relatopro.app.data.local.entity.PhotoEntity
import com.relatopro.app.data.local.entity.ReportAnswerEntity
import com.relatopro.app.data.local.entity.ReportEntity
import com.relatopro.app.data.local.entity.SignatureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity): Long

    @Update
    suspend fun updateReport(report: ReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswer(answer: ReportAnswerEntity): Long

    @Query("SELECT * FROM reports ORDER BY date DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM report_answers WHERE reportId = :reportId")
    fun getReportAnswers(reportId: Long): Flow<List<ReportAnswerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity): Long

    @Query("SELECT * FROM photos WHERE reportId = :reportId")
    fun getReportPhotos(reportId: Long): Flow<List<PhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignature(signature: SignatureEntity): Long

    @Query("SELECT * FROM signatures WHERE reportId = :reportId LIMIT 1")
    suspend fun getSignature(reportId: Long): SignatureEntity?

    @androidx.room.Delete
    suspend fun deleteReport(report: ReportEntity)
}
