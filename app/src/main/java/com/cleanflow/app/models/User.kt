package com.cleanflow.app.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val phone: String = "",
    val role: String = "community",
    val createdAt: Long = System.currentTimeMillis()
)