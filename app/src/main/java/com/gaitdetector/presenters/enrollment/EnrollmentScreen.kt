package com.gaitdetector.presenters.enrollment

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitdetector.EnrollmentSample
import com.gaitdetector.GaitAutoencoder
import com.gaitdetector.GaitFeatureExtractor
import com.gaitdetector.GaitSimulator
import com.gaitdetector.MainActivity
import com.gaitdetector.SampleBuffer
import com.gaitdetector.ui.theme.Inter
import com.gaitdetector.ui.theme.selectedBottomBarColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

// ── Constants (mirror EnrollmentActivity) ────────────────────────────────────
private const val ENROLLMENT_SECONDS  = 30
private val   ENROLLMENT_WINDOWS      = (ENROLLMENT_SECONDS * GaitSimulator.FS) / GaitSimulator.STRIDE
private const val FINETUNE_EPOCHS     = 50
private const val FINETUNE_LR         = 1e-3f
private const val SIGMA_MULTIPLIER    = 3f

// ── Phase state machine ───────────────────────────────────────────────────────
private enum class Phase { READY, COLLECTING, FINE_TUNING, COMPLETE }

/**
 * Compose enrollment screen — same three-phase logic as the original
 * EnrollmentActivity, re-implemented with coroutines and Compose state so that
 * the app's TopBar and BottomBar remain visible throughout.
 *
 * Phase 1 · Collect  (30 s)   — GaitSimulator feeds SampleBuffer; windows collected.
 * Phase 2 · Fine-tune          — Decoder fine-tuned on background dispatcher.
 * Phase 3 · Calibrate          — Threshold = mean(MSE) + 3σ; persisted.
 */
@Composable
fun EnrollmentScreen(modifier: Modifier = Modifier, onDone: () -> Unit = {}) {

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // ── Retained objects ──────────────────────────────────────────────────────
    val autoencoder      = remember { GaitAutoencoder(context) }
    val enrollmentSamples = remember { mutableListOf<EnrollmentSample>() }

    // SampleBuffer callback runs on the simulator thread; posts to state safely
    // via the mutable refs below.
    var windowCount by remember { mutableIntStateOf(0) }

    val sampleBuffer = remember {
        SampleBuffer { window ->
            val features = GaitFeatureExtractor.extract(window)
            val sample   = autoencoder.extractForEnrollment(features)
            synchronized(enrollmentSamples) { enrollmentSamples.add(sample) }
            windowCount++   // Compose reads this on the main thread (safe: atomic Int write)
        }
    }

    val simulator = remember {
        GaitSimulator { sample -> sampleBuffer.addSample(sample) }.also {
            it.gaitType = GaitSimulator.GaitType.NORMAL
        }
    }

    // Clean up simulator + autoencoder when the screen leaves composition
    DisposableEffect(Unit) {
        onDispose {
            simulator.stop()
            autoencoder.close()
        }
    }

    // ── UI state ──────────────────────────────────────────────────────────────
    var phase         by remember { mutableStateOf(Phase.READY) }
    var phaseLabel    by remember { mutableStateOf("Ready to enrol") }
    var statsText     by remember { mutableStateOf(
        "Walk normally for ${ENROLLMENT_SECONDS}s.\n" +
        "The app will personalise the decoder to your gait on-device."
    ) }
    var progress      by remember { mutableFloatStateOf(0f) }
    var btnLabel      by remember { mutableStateOf("Start Enrolment") }
    var btnEnabled    by remember { mutableStateOf(true) }

    // Animate the progress bar smoothly
    val animatedProgress by animateFloatAsState(
        targetValue  = progress,
        animationSpec = tween(durationMillis = 300),
        label        = "enrollProgress",
    )

    // ── Phase helpers (declared in call order: callee before caller) ─────────

    fun calibrateThreshold(samples: List<EnrollmentSample>) {
        phase      = Phase.COMPLETE
        phaseLabel = "Phase 3 / 3 — Calibrating threshold…"

        val mseScores = FloatArray(samples.size) { i ->
            autoencoder.mseFromNormalised(samples[i].normalised, samples[i].embedding)
        }
        val meanMse   = mseScores.average().toFloat()
        val stdMse    = run {
            var acc = 0.0
            mseScores.forEach { v -> val d = v - meanMse; acc += d * d }
            sqrt(acc / mseScores.size).toFloat()
        }
        val threshold = meanMse + SIGMA_MULTIPLIER * stdMse

        autoencoder.saveDecoder()
        context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(MainActivity.KEY_ENROLLED,  true)
            .putFloat(  MainActivity.KEY_THRESHOLD, threshold)
            .apply()

        progress   = 1f
        phaseLabel = "Enrolment complete ✓"
        statsText  = buildString {
            appendLine("Windows collected      : ${samples.size}")
            appendLine("Fine-tune epochs       : $FINETUNE_EPOCHS  (LR = $FINETUNE_LR)")
            appendLine("Mean reconstruction MSE: ${"%.6f".format(meanMse)}")
            appendLine("Std MSE                : ${"%.6f".format(stdMse)}")
            append    ("Personal threshold     : ${"%.6f".format(threshold)}")
        }
        btnLabel   = "Done"
        btnEnabled = true
    }

    fun startFineTuning() {
        val snapshot = synchronized(enrollmentSamples) { enrollmentSamples.toList() }
        if (snapshot.isEmpty()) {
            phaseLabel = "Enrolment failed — no data collected."
            btnLabel   = "Retry"
            btnEnabled = true
            return
        }

        phase      = Phase.FINE_TUNING
        phaseLabel = "Phase 2 / 3 — Fine-tuning decoder…"
        statsText  = "Epoch 0 / $FINETUNE_EPOCHS   loss: —"
        progress   = 0f

        scope.launch {
            withContext(Dispatchers.Default) {
                autoencoder.decoder.resetToBase()
                autoencoder.fineTuneDecoder(
                    samples = snapshot,
                    epochs  = FINETUNE_EPOCHS,
                    lr      = FINETUNE_LR,
                ) { epoch, loss ->
                    scope.launch(Dispatchers.Main) {
                        progress  = epoch.toFloat() / FINETUNE_EPOCHS
                        statsText = "Epoch $epoch / $FINETUNE_EPOCHS   " +
                                    "loss: ${"%.6f".format(loss)}"
                    }
                }
            }
            calibrateThreshold(snapshot)
        }
    }

    fun startCollection() {
        enrollmentSamples.clear()
        windowCount  = 0
        phase        = Phase.COLLECTING
        phaseLabel   = "Phase 1 / 3 — Collecting gait data…"
        statsText    = "Windows: 0 / $ENROLLMENT_WINDOWS"
        progress     = 0f
        btnEnabled   = false

        btnLabel   = "Enrolling…"

        simulator.start()

        scope.launch {
            repeat(ENROLLMENT_SECONDS) {
                delay(1_000)
                val n = windowCount.coerceAtMost(ENROLLMENT_WINDOWS)
                statsText = "Windows: $n / $ENROLLMENT_WINDOWS"
                progress  = n.toFloat() / ENROLLMENT_WINDOWS
            }
            simulator.stop()
            startFineTuning()
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    Column(
        modifier            = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {

        // Phase label
        Text(
            text       = phaseLabel,
            fontSize   = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.W700,
            fontFamily = Inter,
            color      = Color.White,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth(),
        )

        // Progress bar
        LinearProgressIndicator(
            progress       = { animatedProgress },
            modifier       = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color          = selectedBottomBarColor,
            trackColor     = Color(0xFF2A2A2A),
            strokeCap      = StrokeCap.Round,
        )

        // Stats / details box
        Text(
            text       = statsText,
            fontSize   = 14.sp,
            lineHeight = 22.sp,
            fontFamily = Inter,
            color      = Color.White,
            modifier   = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111))
                .padding(16.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        // Action button
        Button(
            onClick  = {
                when (phase) {
                    Phase.READY    -> startCollection()
                    Phase.COMPLETE -> onDone()
                    else           -> { /* disabled during active phases */ }
                }
            },
            enabled  = btnEnabled,
            modifier = Modifier.fillMaxWidth(0.65f),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = selectedBottomBarColor,
                disabledContainerColor = Color(0xFF333333),
            ),
        ) {
            Text(
                text       = btnLabel,
                fontSize   = 16.sp,
                fontWeight = FontWeight.W600,
                fontFamily = Inter,
                color      = Color.White,
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
private fun FloatArray.average(): Double = if (isEmpty()) 0.0 else sum().toDouble() / size
