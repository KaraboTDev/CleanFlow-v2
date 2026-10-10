package com.cleanflow.app.reports

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.cleanflow.app.R
import com.cleanflow.app.auth.SessionManager
import com.cleanflow.app.data.AppDatabase
import com.cleanflow.app.databinding.ActivityReportIssueBinding
import com.cleanflow.app.models.Report
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.util.UUID

class ReportIssueActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportIssueBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var db: AppDatabase
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var selectedSeverity = "Low"
    private var selectedImageUri: Uri? = null
    private var capturedLat = 0.0
    private var capturedLng = 0.0

    private val IMAGE_PICK_CODE = 1001
    private val LOCATION_PERMISSION_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportIssueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        db = AppDatabase.getDatabase(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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
        binding.spinnerIssueType.adapter = adapter

        // Severity buttons
        setSeverity("Low")
        binding.btnLow.setOnClickListener { setSeverity("Low") }
        binding.btnMedium.setOnClickListener { setSeverity("Medium") }
        binding.btnHigh.setOnClickListener { setSeverity("High") }

        // Image upload
        binding.layoutUpload.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_PICK_CODE)
        }

        // GPS capture
        binding.btnCaptureGps.setOnClickListener {
            captureGps()
        }

        // Submit
        binding.btnSubmitReport.setOnClickListener {
            submitReport()
        }

        // Cancel
        binding.btnCancel.setOnClickListener { finish() }

        // Back
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setSeverity(level: String) {
        selectedSeverity = level

        val lowColor = ContextCompat.getColor(this, R.color.severity_low)
        val mediumColor = ContextCompat.getColor(this, R.color.severity_medium)
        val highColor = ContextCompat.getColor(this, R.color.severity_high)
        val inactiveColor = ContextCompat.getColor(this, R.color.card_stroke)

        val activeTextColor = ContextCompat.getColor(this, R.color.white)
        val inactiveTextColor = ContextCompat.getColor(this, R.color.text_secondary)

        binding.btnLow.backgroundTintList = android.content.res.ColorStateList.valueOf(if (level == "Low") lowColor else inactiveColor)
        binding.btnMedium.backgroundTintList = android.content.res.ColorStateList.valueOf(if (level == "Medium") mediumColor else inactiveColor)
        binding.btnHigh.backgroundTintList = android.content.res.ColorStateList.valueOf(if (level == "High") highColor else inactiveColor)

        binding.btnLow.setTextColor(if (level == "Low") activeTextColor else inactiveTextColor)
        binding.btnMedium.setTextColor(if (level == "Medium") activeTextColor else inactiveTextColor)
        binding.btnHigh.setTextColor(if (level == "High") activeTextColor else inactiveTextColor)
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

        binding.btnCaptureGps.text = "Locating..."
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                capturedLat = location.latitude
                capturedLng = location.longitude
                binding.etGpsLocation.setText("${String.format("%.4f", capturedLat)}° S, ${String.format("%.4f", capturedLng)}° E")
                binding.btnCaptureGps.text = "Located ✓"
                val greenColor = ContextCompat.getColor(this, R.color.severity_low)
                binding.btnCaptureGps.backgroundTintList = android.content.res.ColorStateList.valueOf(greenColor)
            } else {
                Toast.makeText(this, "Could not get location. Try again.", Toast.LENGTH_SHORT).show()
                binding.btnCaptureGps.text = "Locate GPS"
            }
        }
    }

    private fun submitReport() {
        val issueType = binding.spinnerIssueType.selectedItem.toString()
        val community = binding.etCommunityName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

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

        binding.btnSubmitReport.isEnabled = false
        binding.btnSubmitReport.text = "Submitting..."

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
            binding.ivPreview.load(selectedImageUri) {
                crossfade(true)
            }
            binding.tvUploadText.text = "Photo attached ✓"
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
