package com.relatopro.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "report_answers",
    foreignKeys = [
        ForeignKey(
            entity = ReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TemplateFieldEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateFieldId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("reportId"), Index("templateFieldId")]
)
data class ReportAnswerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reportId: Long,
    val templateFieldId: Long,
    val answerValue: String?, // Pode ser "C", "NC", "NA", ou qualquer texto de campo customizado
    val observation: String?,
    val status: String // Valid, Invalid, etc.
)
