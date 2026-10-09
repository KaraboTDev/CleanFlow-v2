package com.cleanflow.app.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cleanflow.app.R
import com.cleanflow.app.auth.SessionManager
import com.cleanflow.app.data.AppDatabase
import com.cleanflow.app.databinding.ActivityMyReportsBinding
import com.cleanflow.app.databinding.ItemReportDetailBinding
import com.cleanflow.app.models.Report
import kotlinx.coroutines.launch

class MyReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyReportsBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var db: AppDatabase

    private var allReports = listOf<Report>()
    private var currentFilter = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        db = AppDatabase.getDatabase(this)

        binding.rvMyReports.layoutManager = LinearLayoutManager(this)

        binding.btnBack.setOnClickListener { finish() }

        // Filter buttons
        setupFilterButtons()

        // Load reports
        loadMyReports()
    }

    private fun setupFilterButtons() {
        binding.btnFilterAll.setOnClickListener { setFilter("All", binding.btnFilterAll, binding.btnFilterPending, binding.btnFilterAssigned, binding.btnFilterResolved) }
        binding.btnFilterPending.setOnClickListener { setFilter("Pending", binding.btnFilterAll, binding.btnFilterPending, binding.btnFilterAssigned, binding.btnFilterResolved) }
        binding.btnFilterAssigned.setOnClickListener { setFilter("Assigned", binding.btnFilterAll, binding.btnFilterPending, binding.btnFilterAssigned, binding.btnFilterResolved) }
        binding.btnFilterResolved.setOnClickListener { setFilter("Resolved", binding.btnFilterAll, binding.btnFilterPending, binding.btnFilterAssigned, binding.btnFilterResolved) }
    }

    private fun setFilter(filter: String, vararg buttons: Button) {
        currentFilter = filter
        val activeColor = ContextCompat.getColor(this, R.color.navy_primary)
        val inactiveColor = ContextCompat.getColor(this, R.color.card_stroke)
        val activeTextColor = ContextCompat.getColor(this, R.color.white)
        val inactiveTextColor = ContextCompat.getColor(this, R.color.text_secondary)

        buttons.forEach { btn ->
            val isActive = btn.text.toString() == filter
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (isActive) activeColor else inactiveColor
            )
            btn.setTextColor(if (isActive) activeTextColor else inactiveTextColor)
        }
        applyFilter()
    }

    private fun applyFilter() {
        val filtered = if (currentFilter == "All") {
            allReports
        } else {
            allReports.filter { it.status == currentFilter }
        }
        updateList(filtered)
    }

    private fun updateList(reports: List<Report>) {
        if (reports.isEmpty()) {
            binding.rvMyReports.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.tvReportCount.text = "No reports found"
        } else {
            binding.rvMyReports.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
            binding.tvReportCount.text = "${reports.size} report(s) found"
            binding.rvMyReports.adapter = MyReportsAdapter(reports)
        }
    }

    private fun loadMyReports() {
        val userId = sessionManager.getUserId() ?: return
        binding.tvReportCount.text = "Loading reports..."

        lifecycleScope.launch {
            allReports = db.reportDao().getReportsByUserId(userId)
            applyFilter()
        }
    }
}

class MyReportsAdapter(private val reports: List<Report>) :
    RecyclerView.Adapter<MyReportsAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemReportDetailBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReportDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val report = reports[position]
        holder.binding.tvIssueType.text = report.issueType
        holder.binding.tvCommunity.text = "${report.communityName} • ${report.severity} severity"
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
}
