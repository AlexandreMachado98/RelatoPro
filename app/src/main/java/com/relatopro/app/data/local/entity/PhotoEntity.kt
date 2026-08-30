package com.relatopro.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
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
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("reportId"), Index("templateFieldId")]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reportId: Long,
    val templateFieldId: Long?, // Null se for uma foto geral do relatório e não de um item específico
    val localPath: String, // Caminho otimizado/comprimido
    val timestamp: Long,
    val description: String?,
    val lat: Double?,
    val lng: Double?
)
