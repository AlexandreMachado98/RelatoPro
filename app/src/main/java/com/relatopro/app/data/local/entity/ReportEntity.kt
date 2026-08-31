package com.relatopro.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reports",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("templateId"),
        Index("date"),
        Index("status")
    ]
)
data class ReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val templateId: Long,
    val title: String,
    val reportNumber: String,
    val date: Long,
    val responsible: String,
    val location: String,
    val lat: Double?,
    val lng: Double?,
    val status: String, // DRAFT, FINALIZED, SYNCED
    val generalObservations: String?,
    val pdfLocalPath: String?,
    val syncStatus: String // PENDING, SYNCED, FAILED
)
