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
        Index("companyId"),
        Index("date"),
        Index("status")
    ]
)
data class ReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val templateId: Long,
    val companyId: Long? = null,
    val companyName: String = "Empresa não informada",
    val unit: String = "Matriz",
    val title: String,
    val reportNumber: String,
    val date: Long,
    val responsible: String,
    val location: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val status: String = "DRAFT", // DRAFT, FINALIZED, SENT
    val generalObservations: String? = null,
    val pdfLocalPath: String? = null,
    val syncStatus: String = "PENDING" // PENDING, SYNCED, FAILED
)
