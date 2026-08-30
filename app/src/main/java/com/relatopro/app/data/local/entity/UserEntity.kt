package com.relatopro.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val company: String,
    val email: String,
    val phone: String,
    val syncConfig: String // JSON configuration for sync preferences
)
