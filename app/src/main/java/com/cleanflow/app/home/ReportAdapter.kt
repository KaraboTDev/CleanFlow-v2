package com.cleanflow.app.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cleanflow.app.R
import com.cleanflow.app.models.Report
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportAdapter(private val reports: List<Report>) :
    RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    class ReportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIssueType: TextView = view.findViewById(R.id.tvIssueType)
        val tvCommunity: TextView = view.findViewById(R.id.tvCommunity)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]
        holder.tvIssueType.text = report.issueType
        holder.tvCommunity.text = "${timeAgo(report.submittedAt)} • ${report.communityName}"
        holder.tvStatus.text = report.status

        val badgeRes = when (report.status) {
            "Assigned" -> R.drawable.badge_assigned
            "Resolved" -> R.drawable.badge_resolved
            else -> R.drawable.badge_pending
        }
        holder.tvStatus.setBackgroundResource(badgeRes)
    }

    override fun getItemCount() = reports.size

    private fun timeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val hours = diff / (1000 * 60 * 60)
        val days = hours / 24
        return when {
            hours < 1 -> "Just now"
            hours < 24 -> "$hours hours ago"
            else -> "$days days ago"
        }
    }
}