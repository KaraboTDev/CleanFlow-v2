package com.cleanflow.app.home

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cleanflow.app.R
import com.cleanflow.app.auth.SessionManager
import com.cleanflow.app.data.AppDatabase
import com.cleanflow.app.profile.ProfileActivity
import com.cleanflow.app.reports.MyReportsActivity
import com.cleanflow.app.reports.ReportIssueActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var db: AppDatabase
    private lateinit var rvReports: RecyclerView
    private lateinit var tvGreeting: TextView
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        sessionManager = SessionManager(this)
        db = AppDatabase.getDatabase(this)

        tvGreeting = findViewById(R.id.tvGreeting)
        rvReports = findViewById(R.id.rvReports)
        bottomNav = findViewById(R.id.bottomNav)

        rvReports.layoutManager = LinearLayoutManager(this)

        // Load user name
        val userId = sessionManager.getUserId()
        if (userId != null) {
            lifecycleScope.launch {
                val user = db.userDao().getUserById(userId)
                if (user != null) {
                    tvGreeting.text = "Hello, ${user.fullName}"
                }
            }
        }

        // Load recent reports
        loadReports()

        // Bottom nav
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_report -> {
                    startActivity(Intent(this, ReportIssueActivity::class.java))
                    true
                }
                R.id.nav_my_reports -> {
                    startActivity(Intent(this, MyReportsActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Report card click
        findViewById<androidx.cardview.widget.CardView>(R.id.cardReportIssue).setOnClickListener {
            startActivity(Intent(this, ReportIssueActivity::class.java))
        }

        // View all click
        findViewById<TextView>(R.id.tvViewAll).setOnClickListener {
            startActivity(Intent(this, MyReportsActivity::class.java))
        }

        // Profile icon click
        findViewById<android.widget.ImageView>(R.id.ivProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun loadReports() {
        val userId = sessionManager.getUserId() ?: return

        lifecycleScope.launch {
            val reports = db.reportDao().getReportsByUserId(userId)
            // Limit to 5 for "recent"
            val recentReports = if (reports.size > 5) reports.take(5) else reports
            rvReports.adapter = ReportAdapter(recentReports)
        }
    }

    override fun onResume() {
        super.onResume()
        loadReports()
        bottomNav.selectedItemId = R.id.nav_home
    }
}