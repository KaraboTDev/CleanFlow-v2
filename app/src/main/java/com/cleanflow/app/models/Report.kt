package com.cleanflow.app.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey
    val reportId: String = "",
    val userId: String = "",
    val issueType: String = "",
    val description: String = "",
    val severity: String = "",
    val communityName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imageUrl: String = "",
    val status: String = "Pending",
    val submittedAt: Long = System.currentTimeMillis()
)