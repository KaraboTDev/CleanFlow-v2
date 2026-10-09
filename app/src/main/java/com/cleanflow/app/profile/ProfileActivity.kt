package com.cleanflow.app.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cleanflow.app.auth.LoginActivity
import com.cleanflow.app.auth.SessionManager
import com.cleanflow.app.data.AppDatabase
import com.cleanflow.app.databinding.ActivityProfileBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        db = AppDatabase.getDatabase(this)

        binding.btnBack.setOnClickListener { finish() }

        loadProfile()
        loadStats()

        binding.btnLogout.setOnClickListener {
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
        binding.tvEmail.text = email

        lifecycleScope.launch {
            val user = db.userDao().getUserById(userId)
            if (user != null) {
                binding.tvFullName.text = user.fullName
                binding.tvPhone.text = if (user.phone.isEmpty()) "Not provided" else user.phone
                binding.tvRole.text = user.role.replaceFirstChar { it.uppercase() }
                binding.tvAvatar.text = if (user.fullName.isNotEmpty()) user.fullName.first().uppercase() else "?"

                val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                binding.tvMemberSince.text = sdf.format(Date(user.createdAt))
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

            binding.tvTotalReports.text = total.toString()
            binding.tvResolvedCount.text = resolved.toString()
            binding.tvPendingCount.text = pending.toString()
        }
    }
}
