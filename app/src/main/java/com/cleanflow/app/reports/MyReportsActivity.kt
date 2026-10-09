package com.cleanflow.app.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cleanflow.app.R
import com.cleanflow.app.auth.SessionManager
import com.cleanflow.app.data.AppDatabase
import com.cleanflow.app.models.Report
import kotlinx.coroutines.launch

class MyReportsActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var db: AppDatabase
    private lateinit var rvMyReports: RecyclerView
    private lateinit var tvReportCount: TextView
    private lateinit var layoutEmpty: LinearLayout

    private var allReports = listOf<Report>()
    private var currentFilter = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_reports)

        sessionManager = SessionManager(this)
        db = AppDatabase.getDatabase(this)

        rvMyReports = findViewById(R.id.rvMyReports)
        tvReportCount = findViewById(R.id.tvReportCount)
        layoutEmpty = findViewById(R.id.layoutEmpty)

        rvMyReports.layoutManager = LinearLayoutManager(this)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // Filter buttons
        setupFilterButtons()

        // Load reports
        loadMyReports()
    }

    private fun setupFilterButtons() {
        val btnAll = findViewById<Button>(R.id.btnFilterAll)
        val btnPending = findViewById<Button>(R.id.btnFilterPending)
        val btnAssigned = findViewById<Button>(R.id.btnFilterAssigned)
        val btnResolved = findViewById<Button>(R.id.btnFilterResolved)

        btnAll.setOnClickListener { setFilter("All", btnAll, btnPending, btnAssigned, btnResolved) }
        btnPending.setOnClickListener { setFilter("Pending", btnAll, btnPending, btnAssigned, btnResolved) }
        btnAssigned.setOnClickListener { setFilter("Assigned", btnAll, btnPending, btnAssigned, btnResolved) }
        btnResolved.setOnClickListener { setFilter("Resolved", btnAll, btnPending, btnAssigned, btnResolved) }
    }

    private fun setFilter(filter: String, vararg buttons: Button) {
        currentFilter = filter
        buttons.forEach { btn ->
            val isActive = btn.text.toString() == filter
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(if (isActive) "#1F3864" else "#DDDDDD")
            )
            btn.setTextColor(
                android.graphics.Color.parseColor(if (isActive) "#FFFFFF" else "#333333")
            )
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
            rvMyReports.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
            tvReportCount.text = "No reports found"
        } else {
            rvMyReports.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
            tvReportCount.text = "${reports.size} report(s) found"
            rvMyReports.adapter = MyReportsAdapter(reports)
        }
    }

    private fun loadMyReports() {
        val userId = sessionManager.getUserId() ?: return
        tvReportCount.text = "Loading reports..."

        lifecycleScope.launch {
            allReports = db.reportDao().getReportsByUserId(userId)
            applyFilter()
        }
    }
}

class MyReportsAdapter(private val reports: List<Report>) :
    RecyclerView.Adapter<MyReportsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIssueType: TextView = view.findViewById(R.id.tvIssueType)
        val tvCommunity: TextView = view.findViewById(R.id.tvCommunity)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val report = reports[position]
        holder.tvIssueType.text = report.issueType
        holder.tvCommunity.text = "${report.communityName} • ${report.severity} severity"
        holder.tvStatus.text = report.status

        val badgeRes = when (report.status) {
            "Assigned" -> R.drawable.badge_assigned
            "Resolved" -> R.drawable.badge_resolved
            else -> R.drawable.badge_pending
        }
        holder.tvStatus.setBackgroundResource(badgeRes)
    }

    override fun getItemCount() = reports.size
}