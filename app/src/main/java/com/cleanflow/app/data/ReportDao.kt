package com.cleanflow.app.data

import androidx.room.*
import com.cleanflow.app.models.Report

@Dao
interface ReportDao {
    @Upsert
    suspend fun upsertReport(report: Report)

    @Query("SELECT * FROM reports WHERE userId = :userId ORDER BY submittedAt DESC")
    suspend fun getReportsByUserId(userId: String): List<Report>

    @Query("SELECT * FROM reports ORDER BY submittedAt DESC")
    suspend fun getAllReports(): List<Report>

    @Query("SELECT * FROM reports WHERE reportId = :reportId")
    suspend fun getReportById(reportId: String): Report?

    @Update
    suspend fun updateReport(report: Report)

    @Delete
    suspend fun deleteReport(report: Report)
}