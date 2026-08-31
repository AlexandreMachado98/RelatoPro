package com.relatopro.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "companies",
    indices = [
        Index("name"),
        Index("cnpj")
    ]
)
data class CompanyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val tradeName: String = "",
    val cnpj: String = "",
    val segment: String = "",
    val units: String = "Matriz", // Separated by comma or JSON: e.g. "Matriz, Unidade Brasilândia, Galpão 2"
    val contactName: String = "",
    val contactEmail: String = "",
    val contactPhone: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
