package com.gaitdetector

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Real-time gait anomaly detection using simulated sensor data.
 *
 * The user can switch between gait types via a spinner to observe how the
 * anomaly score changes relative to their personal threshold.
 *
 * UI updates at the window rate (~every 1.28 s with 50% overlap at 50 Hz).
 */
class DetectionActivity : AppCompatActivity() {

    private lateinit var autoencoder: GaitAutoencoder
    private lateinit var simulator: GaitSimulator
    private lateinit var sampleBuffer: SampleBuffer

    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var tvScore: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvThreshold: TextView
    private lateinit var progressScore: ProgressBar
    private lateinit var spinnerGait: Spinner
    private lateinit var scoreHistoryView: ScoreHistoryView

    private var threshold = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detection)

        tvScore        = findViewById(R.id.tvScore)
        tvStatus       = findViewById(R.id.tvStatus)
        tvThreshold    = findViewById(R.id.tvThreshold)
        progressScore  = findViewById(R.id.progressScore)
        spinnerGait    = findViewById(R.id.spinnerGaitType)
        scoreHistoryView = findViewById(R.id.scoreHistoryView)

        threshold = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
            .getFloat(MainActivity.KEY_THRESHOLD, 1f)
        tvThreshold.text = "Threshold: ${"%.5f".format(threshold)}"

        autoencoder = GaitAutoencoder(this)

        // Spinner for gait type selection
        val gaitLabels = GaitSimulator.GaitType.values().map { it.label }
        spinnerGait.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, gaitLabels)
        spinnerGait.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) {
                simulator.gaitType = GaitSimulator.GaitType.values()[pos]
                sampleBuffer.reset()
            }
            override fun onNothingSelected(p: AdapterView<*>?) = Unit
        }

        sampleBuffer = SampleBuffer { window ->
            val features = GaitFeatureExtractor.extract(window)
            val score = autoencoder.reconstructionError(features)
            mainHandler.post { updateUI(score) }
        }

        simulator = GaitSimulator { sample -> sampleBuffer.addSample(sample) }
        simulator.start()
    }

    private fun updateUI(score: Float) {
        val isAnomaly = score > threshold
        val normalised = ((score / (threshold * 3f)) * 100).toInt().coerceIn(0, 100)

        tvScore.text   = "Score: ${"%.5f".format(score)}"
        progressScore.progress = normalised

        if (isAnomaly) {
            tvStatus.text = "⚠ ANOMALY DETECTED"
            tvStatus.setTextColor(Color.RED)
            progressScore.progressTintList =
                android.content.res.ColorStateList.valueOf(Color.RED)
        } else {
            tvStatus.text = "✓ Normal"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.colorNormal))
            progressScore.progressTintList =
                android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.colorNormal)
                )
        }

        scoreHistoryView.addScore(score, threshold)
    }

    override fun onDestroy() {
        super.onDestroy()
        simulator.stop()
        autoencoder.close()
    }
}
