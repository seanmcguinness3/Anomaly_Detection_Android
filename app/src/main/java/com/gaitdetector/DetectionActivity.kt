package com.gaitdetector

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.json.JSONArray

/**
 * Real-time gait identity detection.
 *
 * Each new feature window is encoded to a 12-dim embedding. The L2 distance
 * from the enrolled template is compared to the personal threshold:
 *   L2 ≤ threshold  →  Genuine
 *   L2 >  threshold  →  Imposter
 *
 * The user can switch gait type via the spinner to observe the score change.
 */
class DetectionActivity : AppCompatActivity() {

    private lateinit var autoencoder:     GaitAutoencoder
    private lateinit var simulator:       GaitSimulator
    private lateinit var sampleBuffer:    SampleBuffer

    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var tvScore:         TextView
    private lateinit var tvStatus:        TextView
    private lateinit var tvThreshold:     TextView
    private lateinit var progressScore:   ProgressBar
    private lateinit var spinnerGait:     Spinner
    private lateinit var scoreHistoryView: ScoreHistoryView

    private var threshold = 1f
    private lateinit var template: FloatArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detection)

        tvScore          = findViewById(R.id.tvScore)
        tvStatus         = findViewById(R.id.tvStatus)
        tvThreshold      = findViewById(R.id.tvThreshold)
        progressScore    = findViewById(R.id.progressScore)
        spinnerGait      = findViewById(R.id.spinnerGaitType)
        scoreHistoryView = findViewById(R.id.scoreHistoryView)

        // Load personal template + threshold from SharedPreferences
        val prefs        = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        threshold        = prefs.getFloat(MainActivity.KEY_THRESHOLD, 1f)
        val jsonTemplate = JSONArray(prefs.getString(MainActivity.KEY_TEMPLATE, "[]")!!)
        template         = FloatArray(jsonTemplate.length()) { jsonTemplate.getDouble(it).toFloat() }

        tvThreshold.text = "Threshold (L2): ${"%.4f".format(threshold)}"

        autoencoder = GaitAutoencoder(this)

        // Gait-type spinner
        val labels = GaitSimulator.GaitType.values().map { it.label }
        spinnerGait.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        spinnerGait.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                simulator.gaitType = GaitSimulator.GaitType.values()[pos]
                sampleBuffer.reset()
            }
            override fun onNothingSelected(p: AdapterView<*>?) = Unit
        }

        sampleBuffer = SampleBuffer { window ->
            val features  = GaitFeatureExtractor.extract(window)
            val embedding = autoencoder.getEmbedding(features)
            val l2        = GaitAutoencoder.l2(embedding, template)
            mainHandler.post { updateUI(l2) }
        }

        simulator = GaitSimulator { sample -> sampleBuffer.addSample(sample) }
        simulator.start()
    }

    // ── UI update ─────────────────────────────────────────────────────────────

    private fun updateUI(l2: Float) {
        val isImposter  = l2 > threshold
        // Normalise bar: 0 at 0, 100 at 3× threshold so genuine stays in lower third
        val normalised  = ((l2 / (threshold * 3f)) * 100).toInt().coerceIn(0, 100)

        tvScore.text    = "L2 distance: ${"%.4f".format(l2)}"
        progressScore.progress = normalised

        if (isImposter) {
            tvStatus.text = "⚠  IMPOSTER"
            tvStatus.setTextColor(Color.RED)
            progressScore.progressTintList =
                android.content.res.ColorStateList.valueOf(Color.RED)
        } else {
            tvStatus.text = "✓  Genuine"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.colorNormal))
            progressScore.progressTintList =
                android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.colorNormal)
                )
        }

        scoreHistoryView.addScore(l2, threshold)
    }

    override fun onDestroy() {
        super.onDestroy()
        simulator.stop()
        autoencoder.close()
    }
}
