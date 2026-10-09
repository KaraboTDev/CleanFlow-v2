package com.cleanflow.app.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cleanflow.app.R
import com.cleanflow.app.auth.LoginActivity
import com.cleanflow.app.auth.SessionManager
import com.cleanflow.app.data.AppDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var db: AppDatabase

    private lateinit var tvAvatar: TextView
    private lateinit var tvFullName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvRole: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvMemberSince: TextView
    private lateinit var tvTotalReports: TextView
    private lateinit var tvResolvedCount: TextView
    private lateinit var tvPendingCount: TextView
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        sessionManager = SessionManager(this)
        db = AppDatabase.getDatabase(this)

        tvAvatar = findViewById(R.id.tvAvatar)
        tvFullName = findViewById(R.id.tvFullName)
        tvEmail = findViewById(R.id.tvEmail)
        tvRole = findViewById(R.id.tvRole)
        tvPhone = findViewById(R.id.tvPhone)
        tvMemberSince = findViewById(R.id.tvMemberSince)
        tvTotalReports = findViewById(R.id.tvTotalReports)
        tvResolvedCount = findViewById(R.id.tvResolvedCount)
        tvPendingCount = findViewById(R.id.tvPendingCount)
        btnLogout = findViewById(R.id.btnLogout)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        loadProfile()
        loadStats()

        btnLogout.setOnClickListener {
            sessionManager.logout()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadProfile() {
        val userId = sessionManager.getUserId() ?: return
        val email = sessionManager.getUserEmail() ?: ""
        tvEmail.text = email

        lifecycleScope.launch {
            val user = db.userDao().getUserById(userId)
            if (user != null) {
                tvFullName.text = user.fullName
                tvPhone.text = if (user.phone.isEmpty()) "Not provided" else user.phone
                tvRole.text = user.role.replaceFirstChar { it.uppercase() }
                tvAvatar.text = if (user.fullName.isNotEmpty()) user.fullName.first().uppercase() else "?"

                val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                tvMemberSince.text = sdf.format(Date(user.createdAt))
            } else {
                Toast.makeText(this@ProfileActivity, "Could not load profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadStats() {
        val userId = sessionManager.getUserId() ?: return

        lifecycleScope.launch {
            val reports = db.reportDao().getReportsByUserId(userId)
            val total = reports.size
            val resolved = reports.count { it.status == "Resolved" }
            val pending = reports.count { it.status == "Pending" }

            tvTotalReports.text = total.toString()
            tvResolvedCount.text = resolved.toString()
            tvPendingCount.text = pending.toString()
        }
    }
}