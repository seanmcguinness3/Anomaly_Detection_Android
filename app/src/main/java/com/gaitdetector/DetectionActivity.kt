package com.gaitdetector

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Real-time gait anomaly detection.
 *
 * Each new feature window is passed through the personalised autoencoder.
 * The reconstruction MSE measures how well the decoder (fine-tuned on the
 * enrolled user's normal gait) can reproduce the observed signal:
 *
 *   MSE ≤ threshold  →  Genuine  (normal gait, decoder reconstructs well)
 *   MSE >  threshold  →  Anomaly  (unusual gait, decoder struggles to reconstruct)
 *
 * The threshold was calibrated during the enrolment session and stored in
 * SharedPreferences as KEY_THRESHOLD.
 */
class DetectionActivity : AppCompatActivity() {

    private lateinit var autoencoder:     GaitAutoencoder
    private lateinit var simulator:       GaitSimulator
    private lateinit var sampleBuffer:    SampleBuffer

    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var tvScore:          TextView
    private lateinit var tvStatus:         TextView
    private lateinit var tvThreshold:      TextView
    private lateinit var progressScore:    ProgressBar
    private lateinit var spinnerGait:      Spinner
    private lateinit var scoreHistoryView: ScoreHistoryView

    private var threshold = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detection)

        tvScore          = findViewById(R.id.tvScore)
        tvStatus         = findViewById(R.id.tvStatus)
        tvThreshold      = findViewById(R.id.tvThreshold)
        progressScore    = findViewById(R.id.progressScore)
        spinnerGait      = findViewById(R.id.spinnerGaitType)
        scoreHistoryView = findViewById(R.id.scoreHistoryView)

        // Load personal threshold from SharedPreferences
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        threshold = prefs.getFloat(MainActivity.KEY_THRESHOLD, 1f)

        tvThreshold.text = "Threshold (MSE): ${"%.6f".format(threshold)}"

        autoencoder = GaitAutoencoder(this)

        // Gait-type spinner — lets the user observe how different gaits score
        val labels = GaitSimulator.GaitType.values().map { it.label }
        spinnerGait.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        spinnerGait.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long
            ) {
                simulator.gaitType = GaitSimulator.GaitType.values()[pos]
                sampleBuffer.reset()
            }
            override fun onNothingSelected(p: AdapterView<*>?) = Unit
        }

        sampleBuffer = SampleBuffer { window ->
            val features = GaitFeatureExtractor.extract(window)
            val mse      = autoencoder.getReconstructionMSE(features)
            mainHandler.post { updateUI(mse) }
        }

        simulator = GaitSimulator { sample -> sampleBuffer.addSample(sample) }
        simulator.start()
    }

    // ── UI update ─────────────────────────────────────────────────────────────

    private fun updateUI(mse: Float) {
        val isAnomaly = mse > threshold
        // Normalise bar: 0 → 0, threshold*3 → 100, so genuine stays in lower third
        val normalised = ((mse / (threshold * 3f)) * 100).toInt().coerceIn(0, 100)

        tvScore.text         = "Reconstruction MSE: ${"%.6f".format(mse)}"
        progressScore.progress = normalised

        if (isAnomaly) {
            tvStatus.text = "⚠  ANOMALY"
            tvStatus.setTextColor(Color.RED)
            progressScore.progressTintList =
                android.content.res.ColorStateList.valueOf(Color.RED)
        } else {
            tvStatus.text = "✓  Normal"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.colorNormal))
            progressScore.progressTintList =
                android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.colorNormal)
                )
        }

        scoreHistoryView.addScore(mse, threshold)
    }

    override fun onDestroy() {
        super.onDestroy()
        simulator.stop()
        autoencoder.close()
    }
}
