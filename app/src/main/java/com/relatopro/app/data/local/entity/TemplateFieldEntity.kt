package com.relatopro.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "template_fields",
    foreignKeys = [
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("templateId")]
)
data class TemplateFieldEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val templateId: Long,
    val category: String, // e.g., "Proteção contra incêndio"
    val label: String, // O que será exibido (ex: "Extintores dentro da validade")
    val type: String, // TEXT, NUMBER, C_NC_NA, CHECKBOX, PHOTO, SIGNATURE, etc.
    val orderIndex: Int,
    val isRequired: Boolean,
    val requireObservationOnNC: Boolean = false,
    val requirePhotoOnNC: Boolean = false,
    val maxPhotos: Int = 0,
    val extraConfig: String? = null // JSON para mais detalhes (instruções, referência normativa)
)
