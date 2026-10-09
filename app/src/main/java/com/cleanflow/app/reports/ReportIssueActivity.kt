package com.cleanflow.app.reports

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.cleanflow.app.R
import com.cleanflow.app.auth.SessionManager
import com.cleanflow.app.data.AppDatabase
import com.cleanflow.app.models.Report
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.UUID

class ReportIssueActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var db: AppDatabase
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var spinnerIssueType: Spinner
    private lateinit var etCommunityName: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etGpsLocation: TextInputEditText
    private lateinit var btnLow: Button
    private lateinit var btnMedium: Button
    private lateinit var btnHigh: Button
    private lateinit var btnCaptureGps: Button
    private lateinit var btnSubmitReport: Button
    private lateinit var btnCancel: Button
    private lateinit var ivPreview: ImageView

    private var selectedSeverity = "Low"
    private var selectedImageUri: Uri? = null
    private var capturedLat = 0.0
    private var capturedLng = 0.0

    private val IMAGE_PICK_CODE = 1001
    private val LOCATION_PERMISSION_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_issue)

        sessionManager = SessionManager(this)
        db = AppDatabase.getDatabase(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        spinnerIssueType = findViewById(R.id.spinnerIssueType)
        etCommunityName = findViewById(R.id.etCommunityName)
        etDescription = findViewById(R.id.etDescription)
        etGpsLocation = findViewById(R.id.etGpsLocation)
        btnLow = findViewById(R.id.btnLow)
        btnMedium = findViewById(R.id.btnMedium)
        btnHigh = findViewById(R.id.btnHigh)
        btnCaptureGps = findViewById(R.id.btnCaptureGps)
        btnSubmitReport = findViewById(R.id.btnSubmitReport)
        btnCancel = findViewById(R.id.btnCancel)
        ivPreview = findViewById(R.id.ivPreview)

        // Spinner setup
        val issueTypes = listOf(
            "Select infrastructure type",
            "Borehole",
            "Pipe Leak",
            "Sanitation",
            "Water Tank",
            "Pump Station",
            "Other"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, issueTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerIssueType.adapter = adapter

        // Severity buttons
        setSeverity("Low")
        btnLow.setOnClickListener { setSeverity("Low") }
        btnMedium.setOnClickListener { setSeverity("Medium") }
        btnHigh.setOnClickListener { setSeverity("High") }

        // Image upload
        findViewById<android.widget.LinearLayout>(R.id.layoutUpload).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_PICK_CODE)
        }

        // GPS capture
        btnCaptureGps.setOnClickListener {
            captureGps()
        }

        // Submit
        btnSubmitReport.setOnClickListener {
            submitReport()
        }

        // Cancel
        btnCancel.setOnClickListener { finish() }

        // Back
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun setSeverity(level: String) {
        selectedSeverity = level
        val active = "#1F3864"
        val inactive = "#DDDDDD"
        btnLow.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor(if (level == "Low") active else inactive))
        btnMedium.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor(if (level == "Medium") active else inactive))
        btnHigh.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor(if (level == "High") active else inactive))
        btnLow.setTextColor(android.graphics.Color.parseColor(if (level == "Low") "#FFFFFF" else "#333333"))
        btnMedium.setTextColor(android.graphics.Color.parseColor(if (level == "Medium") "#FFFFFF" else "#333333"))
        btnHigh.setTextColor(android.graphics.Color.parseColor(if (level == "High") "#FFFFFF" else "#333333"))
    }

    private fun captureGps() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
            return
        }

        btnCaptureGps.text = "Capturing..."
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                capturedLat = location.latitude
                capturedLng = location.longitude
                etGpsLocation.setText("${String.format("%.4f", capturedLat)}° S, ${String.format("%.4f", capturedLng)}° E")
                btnCaptureGps.text = "Captured ✓"
                btnCaptureGps.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#00AA55"))
            } else {
                Toast.makeText(this, "Could not get location. Try again.", Toast.LENGTH_SHORT).show()
                btnCaptureGps.text = "Capture GPS"
            }
        }
    }

    private fun submitReport() {
        val issueType = spinnerIssueType.selectedItem.toString()
        val community = etCommunityName.text.toString().trim()
        val description = etDescription.text.toString().trim()

        if (issueType == "Select infrastructure type") {
            Toast.makeText(this, "Please select an infrastructure type", Toast.LENGTH_SHORT).show()
            return
        }
        if (community.isEmpty()) {
            Toast.makeText(this, "Please enter community name", Toast.LENGTH_SHORT).show()
            return
        }
        if (description.isEmpty()) {
            Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show()
            return
        }
        if (capturedLat == 0.0 && capturedLng == 0.0) {
            Toast.makeText(this, "Please capture GPS location", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmitReport.isEnabled = false
        btnSubmitReport.text = "Submitting..."

        val userId = sessionManager.getUserId() ?: return
        val reportId = UUID.randomUUID().toString()

        lifecycleScope.launch {
            val report = Report(
                reportId = reportId,
                userId = userId,
                issueType = issueType,
                description = description,
                severity = selectedSeverity,
                communityName = community,
                latitude = capturedLat,
                longitude = capturedLng,
                imageUrl = selectedImageUri?.toString() ?: "",
                status = "Pending"
            )

            db.reportDao().upsertReport(report)
            Toast.makeText(this@ReportIssueActivity, "Report submitted successfully! ✓", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            ivPreview.load(selectedImageUri) {
                crossfade(true)
            }
            findViewById<android.widget.TextView>(R.id.tvUploadText).text = "Image selected ✓"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            captureGps()
        }
    }
}