package com.gaitdetector

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import kotlin.math.sqrt

/**
 * Enrolment session — collects encoder embeddings for the user's normal gait,
 * builds a template by vector-averaging all embeddings, then calibrates a
 * personal L2 threshold.
 *
 * Multiple enrolment sessions: each run completely replaces the stored template.
 * To merge multiple sessions the template JSON could be accumulated externally,
 * but here we keep it simple — one 30-second session per enrolment.
 *
 * Stored in SharedPreferences:
 *   KEY_TEMPLATE  : JSON array of floats  (the mean embedding vector)
 *   KEY_THRESHOLD : Float  (mean L2 + SIGMA_MULTIPLIER × std L2)
 */
class EnrollmentActivity : AppCompatActivity() {

    private lateinit var autoencoder: GaitAutoencoder
    private lateinit var simulator:   GaitSimulator
    private lateinit var sampleBuffer: SampleBuffer

    private val embeddings  = mutableListOf<FloatArray>()
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var tvPhase:     TextView
    private lateinit var tvStats:     TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnAction:   Button

    private var enrollmentComplete = false
    private var windowCount        = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enrollment)

        tvPhase     = findViewById(R.id.tvPhase)
        tvStats     = findViewById(R.id.tvStats)
        progressBar = findViewById(R.id.progressBar)
        btnAction   = findViewById(R.id.btnAction)

        progressBar.max = ENROLLMENT_WINDOWS
        autoencoder = GaitAutoencoder(this)

        sampleBuffer = SampleBuffer { window ->
            // FFT preprocessing + encoder forward pass
            val features  = GaitFeatureExtractor.extract(window)
            val embedding = autoencoder.getEmbedding(features)
            synchronized(embeddings) { embeddings.add(embedding) }
            windowCount++
            mainHandler.post { updateProgress() }
        }

        simulator = GaitSimulator { sample -> sampleBuffer.addSample(sample) }
        simulator.gaitType = GaitSimulator.GaitType.NORMAL

        btnAction.setOnClickListener {
            if (!enrollmentComplete) startEnrollment() else finish()
        }

        tvPhase.text = "Ready to enrol"
        tvStats.text = "Walk normally for ${ENROLLMENT_SECONDS}s.\n" +
                       "The encoder will capture your personal gait embedding."
        btnAction.text = "Start Enrolment"
    }

    // ── Session control ───────────────────────────────────────────────────────

    private fun startEnrollment() {
        embeddings.clear()
        windowCount        = 0
        enrollmentComplete = false
        btnAction.isEnabled = false
        tvPhase.text = "Recording gait embeddings…"
        simulator.start()

        mainHandler.postDelayed({
            simulator.stop()
            finaliseEnrollment()
        }, ENROLLMENT_SECONDS * 1000L)
    }

    private fun updateProgress() {
        progressBar.progress = windowCount.coerceAtMost(ENROLLMENT_WINDOWS)
        tvStats.text = "Windows processed: $windowCount / $ENROLLMENT_WINDOWS"
    }

    // ── Template computation ──────────────────────────────────────────────────

    private fun finaliseEnrollment() {
        val snapshot = synchronized(embeddings) { embeddings.toList() }
        if (snapshot.isEmpty()) {
            tvPhase.text = "Enrolment failed — no data collected."
            btnAction.text = "Retry"; btnAction.isEnabled = true; return
        }

        val dim = snapshot[0].size

        // Template = vector mean of all collected embeddings
        val template = FloatArray(dim) { d ->
            snapshot.fold(0f) { acc, emb -> acc + emb[d] } / snapshot.size
        }

        // L2 distances from each enrollment embedding to the template
        val distances = FloatArray(snapshot.size) { i ->
            GaitAutoencoder.l2(snapshot[i], template)
        }
        val meanDist  = distances.mean()
        val stdDist   = distances.std(meanDist)
        val threshold = meanDist + SIGMA_MULTIPLIER * stdDist

        // Persist
        val jsonTemplate = JSONArray().apply { template.forEach { put(it.toDouble()) } }
        getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE).edit()
            .putBoolean(MainActivity.KEY_ENROLLED,  true)
            .putFloat(  MainActivity.KEY_THRESHOLD, threshold)
            .putString( MainActivity.KEY_TEMPLATE,  jsonTemplate.toString())
            .apply()

        enrollmentComplete  = true
        progressBar.progress = ENROLLMENT_WINDOWS
        tvPhase.text = "Enrolment complete ✓"
        tvStats.text = buildString {
            appendLine("Embeddings collected  : ${snapshot.size}")
            appendLine("Embedding dimension   : $dim")
            appendLine("Mean L2 to template   : ${"%.4f".format(meanDist)}")
            appendLine("Std L2                : ${"%.4f".format(stdDist)}")
            appendLine("Personal threshold    : ${"%.4f".format(threshold)}")
            append    ("  ( mean + ${SIGMA_MULTIPLIER}σ )")
        }
        btnAction.text      = "Continue"
        btnAction.isEnabled = true
    }

    override fun onDestroy() {
        super.onDestroy()
        simulator.stop()
        autoencoder.close()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun FloatArray.mean(): Float = if (isEmpty()) 0f else sum() / size

    private fun FloatArray.std(mean: Float): Float {
        if (size < 2) return 0f
        var acc = 0.0
        for (v in this) { val d = v - mean; acc += d * d }
        return sqrt((acc / size).toFloat())
    }

    companion object {
        const val ENROLLMENT_SECONDS = 30
        val ENROLLMENT_WINDOWS = (ENROLLMENT_SECONDS * GaitSimulator.FS) / GaitSimulator.STRIDE
        const val SIGMA_MULTIPLIER = 3f
    }
}
