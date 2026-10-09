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
import com.cleanflow.app.models.User
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.UUID

class RegisterActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager

    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnRegister: Button
    private lateinit var btnBackToLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)

        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)

        btnRegister.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 8) {
                Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnRegister.isEnabled = false
            btnRegister.text = "Creating account..."

            lifecycleScope.launch {
                val existingUser = db.userDao().getUserByEmail(email)
                if (existingUser != null) {
                    Toast.makeText(this@RegisterActivity, "Email already registered", Toast.LENGTH_LONG).show()
                    btnRegister.isEnabled = true
                    btnRegister.text = "Create Account"
                    return@launch
                }

                val uid = UUID.randomUUID().toString()
                val user = User(
                    uid = uid,
                    fullName = fullName,
                    email = email,
                    phone = phone,
                    role = "community"
                )

                db.userDao().upsertUser(user)
                sessionManager.saveSession(uid, email)
                
                Toast.makeText(this@RegisterActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@RegisterActivity, HomeActivity::class.java))
                finish()
            }
        }

        btnBackToLogin.setOnClickListener {
            finish()
        }
    }
}