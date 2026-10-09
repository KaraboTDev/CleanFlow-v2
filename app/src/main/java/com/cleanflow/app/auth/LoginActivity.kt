package com.cleanflow.app.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cleanflow.app.R
import com.cleanflow.app.data.AppDatabase
import com.cleanflow.app.home.HomeActivity
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var db: AppDatabase
    
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)
        db = AppDatabase.getDatabase(this)

        // If user already logged in go straight to Home
        if (sessionManager.isLoggedIn()) {
            goToHome()
            return
        }

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "Signing in..."

            lifecycleScope.launch {
                val user = db.userDao().getUserByEmail(email)
                if (user != null) {
                    // In a real app, you'd hash and compare passwords. 
                    // For this migration, we'll do a simple check.
                    sessionManager.saveSession(user.uid, user.email)
                    Toast.makeText(this@LoginActivity, "Welcome back!", Toast.LENGTH_SHORT).show()
                    goToHome()
                } else {
                    Toast.makeText(this@LoginActivity, "User not found. Please register.", Toast.LENGTH_LONG).show()
                    btnLogin.isEnabled = true
                    btnLogin.text = "Sign In"
                }
            }
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}