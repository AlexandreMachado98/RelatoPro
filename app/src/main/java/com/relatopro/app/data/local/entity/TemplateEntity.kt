package com.relatopro.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val category: String,
    val version: Int = 1,
    val createdAt: Long,
    val updatedAt: Long,
    val status: String, // e.g., ACTIVE, ARCHIVED
    val visualConfig: String // JSON string for header/footer config, colors, etc.
)
