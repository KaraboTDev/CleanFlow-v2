package com.cleanflow.app.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cleanflow.app.R
import com.cleanflow.app.auth.SessionManager
import com.cleanflow.app.data.AppDatabase
import com.cleanflow.app.databinding.ActivityHomeBinding
import com.cleanflow.app.profile.ProfileActivity
import com.cleanflow.app.reports.MyReportsActivity
import com.cleanflow.app.reports.ReportIssueActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class WaterTip(
    val category: String,
    val title: String,
    val description: String
)

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var db: AppDatabase

    private var currentTipIndex = 0
    private var tipAutoRotateJob: Job? = null

    private val waterTips = listOf(
        WaterTip(
            category = "CONSERVATION",
            title = "Report Pipe Leaks Early",
            description = "A single dripping tap or broken pipe can waste over 100 liters of water daily. Reporting leaks early saves vital water for your entire community."
        ),
        WaterTip(
            category = "HYGIENE",
            title = "Cover Household Water Containers",
            description = "Always store collected drinking water in clean, covered vessels to prevent contamination and discourage mosquito breeding near home."
        ),
        WaterTip(
            category = "MAINTENANCE",
            title = "Protect Borehole Perimeter",
            description = "Keep livestock and waste disposal at least 30 meters away from borehole wells to maintain clean, safe groundwater for everyone."
        ),
        WaterTip(
            category = "HEALTH",
            title = "Purify Unclear Water",
            description = "If water clarity changes after heavy rains, boil it for at least 1 minute or treat it before drinking to safeguard your family's health."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        db = AppDatabase.getDatabase(this)

        binding.rvReports.layoutManager = LinearLayoutManager(this)

        // Setup Water Tips
        setupWaterTips()

        // Load user name
        val userId = sessionManager.getUserId()
        if (userId != null) {
            lifecycleScope.launch {
                val user = db.userDao().getUserById(userId)
                if (user != null) {
                    binding.tvGreeting.text = "Hello, ${user.fullName}"
                }
            }
        }

        // Load recent reports
        loadReports()

        // Bottom nav
        binding.bottomNav.selectedItemId = R.id.nav_home
        binding.bottomNav.setOnItemSelectedListener { item ->
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
        binding.cardReportIssue.setOnClickListener {
            startActivity(Intent(this, ReportIssueActivity::class.java))
        }

        // View all click
        binding.tvViewAll.setOnClickListener {
            startActivity(Intent(this, MyReportsActivity::class.java))
        }

        // Profile icon click
        binding.ivProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun setupWaterTips() {
        displayTip(currentTipIndex)
        binding.btnNextTip.setOnClickListener {
            advanceTip()
            startTipAutoRotation() // Reset 10-second timer when manually clicked
        }
    }

    private fun advanceTip() {
        currentTipIndex = (currentTipIndex + 1) % waterTips.size
        displayTip(currentTipIndex)
    }

    private fun startTipAutoRotation() {
        tipAutoRotateJob?.cancel()
        tipAutoRotateJob = lifecycleScope.launch {
            while (true) {
                delay(5_000L) // 5 seconds for testing
                advanceTip()
            }
        }
    }

    private fun displayTip(index: Int) {
        val tip = waterTips[index]
        binding.tvTipCategory.text = tip.category
        binding.tvTipTitle.text = tip.title
        binding.tvTipDescription.text = tip.description
        binding.tvTipCounter.text = "${index + 1} of ${waterTips.size}"
    }

    private fun loadReports() {
        val userId = sessionManager.getUserId() ?: return

        lifecycleScope.launch {
            val reports = db.reportDao().getReportsByUserId(userId)
            if (reports.isEmpty()) {
                binding.rvReports.visibility = View.GONE
                binding.layoutEmptyReports.visibility = View.VISIBLE
            } else {
                binding.rvReports.visibility = View.VISIBLE
                binding.layoutEmptyReports.visibility = View.GONE
                val recentReports = if (reports.size > 5) reports.take(5) else reports
                binding.rvReports.adapter = ReportAdapter(recentReports)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadReports()
        binding.bottomNav.selectedItemId = R.id.nav_home
        startTipAutoRotation()
    }

    override fun onPause() {
        super.onPause()
        tipAutoRotateJob?.cancel()
    }
}
