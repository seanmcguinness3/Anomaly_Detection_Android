package com.gaitdetector

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.sqrt

/**
 * Enrolment session — 30 seconds of simulated normal walking.
 *
 * The autoencoder is already trained offline (base model).  Here we collect
 * reconstruction errors on the individual user's "normal" gait and fit a
 * personal threshold:  threshold = mean_error + 3 × std_error.
 *
 * This threshold is persisted to SharedPreferences and used by DetectionActivity.
 */
class EnrollmentActivity : AppCompatActivity() {

    private lateinit var autoencoder: GaitAutoencoder
    private lateinit var simulator: GaitSimulator
    private lateinit var sampleBuffer: SampleBuffer

    private val errors = mutableListOf<Float>()
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var tvPhase: TextView
    private lateinit var tvStats: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnAction: Button

    private var enrollmentComplete = false
    private var windowCount = 0
    private val targetWindows = ENROLLMENT_WINDOWS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enrollment)

        tvPhase     = findViewById(R.id.tvPhase)
        tvStats     = findViewById(R.id.tvStats)
        progressBar = findViewById(R.id.progressBar)
        btnAction   = findViewById(R.id.btnAction)

        progressBar.max = targetWindows

        autoencoder = GaitAutoencoder(this)

        sampleBuffer = SampleBuffer { window ->
            val features = GaitFeatureExtractor.extract(window)
            val error = autoencoder.reconstructionError(features)
            synchronized(errors) { errors.add(error) }
            windowCount++
            mainHandler.post { updateProgress() }
        }

        simulator = GaitSimulator { sample -> sampleBuffer.addSample(sample) }
        simulator.gaitType = GaitSimulator.GaitType.NORMAL

        btnAction.setOnClickListener {
            if (!enrollmentComplete) {
                startEnrollment()
            } else {
                finish()
            }
        }

        tvPhase.text = "Ready to enrol"
        tvStats.text = "Walk normally for ${ENROLLMENT_SECONDS}s to set your personal baseline."
        btnAction.text = "Start Enrolment"
    }

    private fun startEnrollment() {
        errors.clear()
        windowCount = 0
        enrollmentComplete = false
        btnAction.isEnabled = false
        tvPhase.text = "Collecting baseline data…"
        simulator.start()

        mainHandler.postDelayed({
            simulator.stop()
            finaliseEnrollment()
        }, ENROLLMENT_SECONDS * 1000L)
    }

    private fun updateProgress() {
        progressBar.progress = windowCount.coerceAtMost(targetWindows)
        val pct = (windowCount * 100 / targetWindows).coerceAtMost(100)
        tvStats.text = "Windows collected: $windowCount / $targetWindows  ($pct%)"
    }

    private fun finaliseEnrollment() {
        val snapshot = synchronized(errors) { errors.toFloatArray() }
        if (snapshot.isEmpty()) {
            tvPhase.text = "Enrolment failed — no data collected."
            btnAction.text = "Retry"
            btnAction.isEnabled = true
            return
        }

        val mean = snapshot.mean()
        val std = snapshot.std(mean)
        val threshold = mean + SIGMA_MULTIPLIER * std

        getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE).edit()
            .putBoolean(MainActivity.KEY_ENROLLED, true)
            .putFloat(MainActivity.KEY_THRESHOLD, threshold)
            .apply()

        enrollmentComplete = true
        progressBar.progress = targetWindows
        tvPhase.text = "Enrolment complete ✓"
        tvStats.text = """
            Windows analysed : ${snapshot.size}
            Mean error       : ${"%.5f".format(mean)}
            Std error        : ${"%.5f".format(std)}
            Personal threshold: ${"%.5f".format(threshold)}

            (threshold = mean + ${SIGMA_MULTIPLIER}σ)
        """.trimIndent()
        btnAction.text = "Continue"
        btnAction.isEnabled = true
    }

    override fun onDestroy() {
        super.onDestroy()
        simulator.stop()
        autoencoder.close()
    }

    // -------------------------------------------------------------------------

    private fun FloatArray.mean(): Float = if (isEmpty()) 0f else sum() / size
    private fun FloatArray.std(mean: Float): Float {
        if (size < 2) return 0f
        val variance = sumOf { ((it - mean) * (it - mean)).toDouble() }.toFloat() / size
        return sqrt(variance)
    }

    companion object {
        const val ENROLLMENT_SECONDS = 30
        const val ENROLLMENT_WINDOWS = (ENROLLMENT_SECONDS * GaitSimulator.FS) / GaitSimulator.STRIDE
        const val SIGMA_MULTIPLIER = 3f
    }
}
