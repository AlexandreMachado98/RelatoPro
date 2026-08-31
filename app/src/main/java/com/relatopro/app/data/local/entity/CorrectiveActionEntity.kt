package com.relatopro.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "corrective_actions",
    foreignKeys = [
        ForeignKey(
            entity = ReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("reportId"),
        Index("companyId"),
        Index("status")
    ]
)
data class CorrectiveActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reportId: Long,
    val companyId: Long? = null,
    val templateFieldId: Long? = null,
    val nonConformityTitle: String,
    val actionDescription: String,
    val responsible: String = "",
    val deadlineDate: Long = 0L,
    val status: String = "PENDING", // PENDING, IN_PROGRESS, RESOLVED, EXPIRED
    val resolutionNotes: String = "",
    val resolvedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
