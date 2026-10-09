package com.cleanflow.app.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cleanflow.app.R
import com.cleanflow.app.databinding.ItemReportBinding
import com.cleanflow.app.models.Report

class ReportAdapter(private val reports: List<Report>) :
    RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    class ReportViewHolder(val binding: ItemReportBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReportBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]
        holder.binding.tvIssueType.text = report.issueType
        holder.binding.tvCommunity.text = "${timeAgo(report.submittedAt)} • ${report.communityName}"
        holder.binding.tvStatus.text = report.status

        val (badgeRes, textColorRes) = when (report.status) {
            "Assigned" -> Pair(R.drawable.badge_assigned, R.color.status_assigned_text)
            "Resolved" -> Pair(R.drawable.badge_resolved, R.color.status_resolved_text)
            else -> Pair(R.drawable.badge_pending, R.color.status_pending_text)
        }
        holder.binding.tvStatus.setBackgroundResource(badgeRes)
        holder.binding.tvStatus.setTextColor(ContextCompat.getColor(holder.binding.root.context, textColorRes))
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
