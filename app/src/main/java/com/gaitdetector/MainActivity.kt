package com.gaitdetector

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var tvStatus: TextView
    private lateinit var btnEnroll: Button
    private lateinit var btnDetect: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        tvStatus  = findViewById(R.id.tvEnrollmentStatus)
        btnEnroll = findViewById(R.id.btnEnroll)
        btnDetect = findViewById(R.id.btnDetect)

        btnEnroll.setOnClickListener {
            startActivity(Intent(this, EnrollmentActivity::class.java))
        }

        btnDetect.setOnClickListener {
            startActivity(Intent(this, DetectionActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val enrolled = prefs.getBoolean(KEY_ENROLLED, false)
        val threshold = prefs.getFloat(KEY_THRESHOLD, -1f)
        if (enrolled && threshold > 0) {
            tvStatus.text = "Enrolled ✓\nPersonal threshold: ${"%.4f".format(threshold)}"
            btnDetect.isEnabled = true
        } else {
            tvStatus.text = "Not yet enrolled.\nComplete an enrolment session to personalise detection."
            btnDetect.isEnabled = false
        }
    }

    companion object {
        const val PREFS_NAME     = "gait_prefs"
        const val KEY_ENROLLED   = "enrolled"
        const val KEY_THRESHOLD  = "threshold"
    }
}
