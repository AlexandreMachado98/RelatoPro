package com.relatopro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
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

    @Query("SELECT * FROM reports WHERE id = :reportId")
    suspend fun getReportById(reportId: Long): ReportEntity?

    @Query("SELECT * FROM report_answers WHERE reportId = :reportId")
    fun getReportAnswers(reportId: Long): Flow<List<ReportAnswerEntity>>

    @Query("SELECT * FROM report_answers WHERE reportId = :reportId")
    suspend fun getReportAnswersSync(reportId: Long): List<ReportAnswerEntity>

    @Query("SELECT * FROM report_answers")
    fun getAllAnswers(): Flow<List<ReportAnswerEntity>>

    @Query("SELECT * FROM report_answers")
    suspend fun getAllAnswersSync(): List<ReportAnswerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity): Long

    @Delete
    suspend fun deletePhoto(photo: PhotoEntity)

    @Query("SELECT * FROM photos WHERE reportId = :reportId")
    fun getReportPhotos(reportId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<PhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignature(signature: SignatureEntity): Long

    @Query("SELECT * FROM signatures WHERE reportId = :reportId LIMIT 1")
    suspend fun getSignature(reportId: Long): SignatureEntity?

    @Query("SELECT * FROM signatures WHERE reportId = :reportId ORDER BY id ASC")
    suspend fun getSignatures(reportId: Long): List<SignatureEntity>

    @Query("DELETE FROM signatures WHERE reportId = :reportId AND (role LIKE :role || '%' OR role = :role)")
    suspend fun deleteSignatureByRole(reportId: Long, role: String)

    @androidx.room.Delete
    suspend fun deleteReport(report: ReportEntity)
}
