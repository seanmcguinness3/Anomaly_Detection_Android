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
 * Enrolment session — three sequential phases:
 *
 *   Phase 1 · Collect  (30 s)
 *     Walk normally while the app accumulates ~46 windows of normalised features
 *     and their encoder embeddings. The encoder is frozen; no weights are changed.
 *
 *   Phase 2 · Fine-tune  (background thread)
 *     The decoder is fine-tuned on the collected windows using mini-batch SGD.
 *     Only decoder weights are updated — the encoder TFLite model is untouched.
 *     Progress (epoch / loss) is streamed to the UI in real time.
 *
 *   Phase 3 · Calibrate
 *     Reconstruction MSE is recomputed on every enrolment window using the
 *     personalised decoder. The personal threshold is set to:
 *         threshold = mean(MSE) + SIGMA_MULTIPLIER × std(MSE)
 *     Decoder weights and the threshold are persisted.
 */
class EnrollmentActivity : AppCompatActivity() {

    private lateinit var autoencoder:  GaitAutoencoder
    private lateinit var simulator:    GaitSimulator
    private lateinit var sampleBuffer: SampleBuffer

    private val enrollmentSamples = mutableListOf<EnrollmentSample>()
    private val mainHandler        = Handler(Looper.getMainLooper())

    private lateinit var tvPhase:     TextView
    private lateinit var tvStats:     TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnAction:   Button

    private var windowCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enrollment)

        tvPhase     = findViewById(R.id.tvPhase)
        tvStats     = findViewById(R.id.tvStats)
        progressBar = findViewById(R.id.progressBar)
        btnAction   = findViewById(R.id.btnAction)

        autoencoder = GaitAutoencoder(this)

        sampleBuffer = SampleBuffer { window ->
            val features = GaitFeatureExtractor.extract(window)
            val sample   = autoencoder.extractForEnrollment(features)
            synchronized(enrollmentSamples) { enrollmentSamples.add(sample) }
            windowCount++
            mainHandler.post { onWindowCollected() }
        }

        simulator = GaitSimulator { sample -> sampleBuffer.addSample(sample) }
        simulator.gaitType = GaitSimulator.GaitType.NORMAL

        btnAction.setOnClickListener { startCollection() }

        showReady()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Phase 1 — Collect
    // ══════════════════════════════════════════════════════════════════════════

    private fun showReady() {
        progressBar.max      = ENROLLMENT_WINDOWS
        progressBar.progress = 0
        tvPhase.text  = "Ready to enrol"
        tvStats.text  = "Walk normally for ${ENROLLMENT_SECONDS}s.\n" +
                "The app will personalise the decoder to your gait on-device."
        btnAction.text    = "Start Enrolment"
        btnAction.isEnabled = true
    }

    private fun startCollection() {
        enrollmentSamples.clear()
        windowCount     = 0
        btnAction.isEnabled = false
        tvPhase.text    = "Phase 1 / 3 — Collecting gait data…"
        tvStats.text    = "Windows: 0 / $ENROLLMENT_WINDOWS"
        progressBar.max = ENROLLMENT_WINDOWS
        progressBar.progress = 0

        simulator.start()

        mainHandler.postDelayed({
            simulator.stop()
            startFineTuning()
        }, ENROLLMENT_SECONDS * 1000L)
    }

    private fun onWindowCollected() {
        val n = windowCount.coerceAtMost(ENROLLMENT_WINDOWS)
        progressBar.progress = n
        tvStats.text = "Windows: $n / $ENROLLMENT_WINDOWS"
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Phase 2 — Fine-tune
    // ══════════════════════════════════════════════════════════════════════════

    private fun startFineTuning() {
        val snapshot = synchronized(enrollmentSamples) { enrollmentSamples.toList() }
        if (snapshot.isEmpty()) {
            tvPhase.text = "Enrolment failed — no data collected."
            btnAction.text = "Retry"; btnAction.isEnabled = true; return
        }

        tvPhase.text    = "Phase 2 / 3 — Fine-tuning decoder…"
        tvStats.text    = "Epoch 0 / $FINETUNE_EPOCHS   loss: —"
        progressBar.max = FINETUNE_EPOCHS
        progressBar.progress = 0

        // Fine-tuning is CPU-intensive — run on a background thread
        Thread {
            autoencoder.decoder.resetToBase()   // start from base weights each enrolment

            autoencoder.fineTuneDecoder(
                samples    = snapshot,
                epochs     = FINETUNE_EPOCHS,
                lr         = FINETUNE_LR,
            ) { epoch, loss ->
                mainHandler.post {
                    progressBar.progress = epoch
                    tvStats.text = "Epoch $epoch / $FINETUNE_EPOCHS   " +
                            "loss: ${"%.6f".format(loss)}"
                }
            }

            mainHandler.post { calibrateThreshold(snapshot) }
        }.start()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Phase 3 — Calibrate threshold
    // ══════════════════════════════════════════════════════════════════════════

    private fun calibrateThreshold(samples: List<EnrollmentSample>) {
        tvPhase.text = "Phase 3 / 3 — Calibrating threshold…"

        // Recompute MSE on all enrolment windows using the personalised decoder
        val mseScores = FloatArray(samples.size) { i ->
            autoencoder.mseFromNormalised(samples[i].normalised, samples[i].embedding)
        }

        val meanMse   = mseScores.mean()
        val stdMse    = mseScores.std(meanMse)
        val threshold = meanMse + SIGMA_MULTIPLIER * stdMse

        // Persist decoder weights and threshold
        autoencoder.saveDecoder()
        getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE).edit()
            .putBoolean(MainActivity.KEY_ENROLLED,  true)
            .putFloat(  MainActivity.KEY_THRESHOLD, threshold)
            .apply()

        progressBar.progress = FINETUNE_EPOCHS   // fill bar
        tvPhase.text = "Enrolment complete ✓"
        tvStats.text = buildString {
            appendLine("Windows collected     : ${samples.size}")
            appendLine("Fine-tune epochs      : $FINETUNE_EPOCHS  (LR = $FINETUNE_LR)")
            appendLine("Mean reconstruction MSE : ${"%.6f".format(meanMse)}")
            appendLine("Std MSE               : ${"%.6f".format(stdMse)}")
            appendLine("Personal threshold    : ${"%.6f".format(threshold)}")
            append    ("  ( mean + ${SIGMA_MULTIPLIER}σ )")
        }
        btnAction.text      = "Continue"
        btnAction.isEnabled = true
        btnAction.setOnClickListener { finish() }
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
        val   ENROLLMENT_WINDOWS     = (ENROLLMENT_SECONDS * GaitSimulator.FS) / GaitSimulator.STRIDE
        const val FINETUNE_EPOCHS    = 50
        const val FINETUNE_LR        = 1e-3f
        const val SIGMA_MULTIPLIER   = 3f
    }
}
