package com.gaitdetector

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Shared ViewModel that owns the live gait-anomaly detection pipeline.
 *
 * The simulator starts immediately in [init] so detection runs in the background
 * from the moment the app launches — regardless of which Compose screen is showing.
 *
 * Consumers:
 *  · [uiState]       — text labels, status, latest MSE, enrolled flag, threshold,
 *                       injecting flag, and current gait type
 *  · [scoreEvents]   — raw (mse, threshold) pairs, one per window → ScoreHistoryView
 *  · [anomalyAlerts] — fires once per rising-edge anomaly event (normal → anomaly);
 *                       collected by GaitApp to show the global "Anomaly Detected" dialog
 */
class DetectionViewModel(application: Application) : AndroidViewModel(application) {

    // ── UI state ──────────────────────────────────────────────────────────────

    data class UiState(
        val enrolled:    Boolean                = false,
        val threshold:   Float                  = 1f,
        val latestMse:   Float                  = 0f,
        val isAnomaly:   Boolean                = false,
        val gaitType:    GaitSimulator.GaitType  = GaitSimulator.GaitType.NORMAL,
        val isInjecting: Boolean                = false,   // true while inject sequence runs
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** One (mse, threshold) pair per analysed window — drives ScoreHistoryView. */
    private val _scoreEvents = MutableSharedFlow<Pair<Float, Float>>(extraBufferCapacity = 64)
    val scoreEvents: SharedFlow<Pair<Float, Float>> = _scoreEvents.asSharedFlow()

    /**
     * Fires once each time the anomaly state transitions from false → true.
     * Collected in [GaitApp] to show the global alert dialog.
     * replay = 0 so a dismissed alert never re-fires on resubscription.
     */
    private val _anomalyAlerts = MutableSharedFlow<Unit>(extraBufferCapacity = 1, replay = 0)
    val anomalyAlerts: SharedFlow<Unit> = _anomalyAlerts.asSharedFlow()

    // Rolling window of the last 5 MSE values used to debounce the alert.
    // Sorted descending; median (index 2) must exceed threshold before an
    // alert fires, so a single spike never triggers the dialog.
    private val mseWindow = ArrayDeque<Float>(5)

    // Track the previous smoothed-anomaly state to detect rising edges
    private var prevIsAnomaly = false

    // ── Detection objects ─────────────────────────────────────────────────────

    private val autoencoder:  GaitAutoencoder
    private val sampleBuffer: SampleBuffer
    private val simulator:    GaitSimulator

    init {
        val prefs     = application.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val enrolled  = prefs.getBoolean(MainActivity.KEY_ENROLLED,  false)
        val threshold = prefs.getFloat(  MainActivity.KEY_THRESHOLD, 1f)
        _uiState.update { it.copy(enrolled = enrolled, threshold = threshold) }

        autoencoder = GaitAutoencoder(application)

        sampleBuffer = SampleBuffer { window ->
            val features  = GaitFeatureExtractor.extract(window)
            val mse       = autoencoder.getReconstructionMSE(features)
            val thr       = _uiState.value.threshold
            val isAnomaly = mse > thr

            _uiState.update { state ->
                state.copy(latestMse = mse, isAnomaly = isAnomaly)
            }
            _scoreEvents.tryEmit(Pair(mse, thr))

            // Debounced alert: maintain a 5-deep queue, sort descending,
            // take the median (index 2). Only fire on a rising edge where
            // the median itself exceeds the threshold.
            if (mseWindow.size == 5) mseWindow.removeFirst()
            mseWindow.addLast(mse)
            val smoothedAnomaly = if (mseWindow.size == 5) {
                val sorted = mseWindow.sortedDescending()
                sorted[2] > thr   // median of sorted-descending window
            } else {
                false             // don't alert until the window is full
            }

            if (smoothedAnomaly && !prevIsAnomaly) {
                _anomalyAlerts.tryEmit(Unit)
            }
            prevIsAnomaly = smoothedAnomaly
        }

        simulator = GaitSimulator { sample -> sampleBuffer.addSample(sample) }.also {
            it.gaitType = GaitSimulator.GaitType.NORMAL
            it.start()
        }
    }

    // ── Public actions ────────────────────────────────────────────────────────

    /** Switch the simulated gait pattern (called from the DebugScreen dropdown). */
    fun setGaitType(type: GaitSimulator.GaitType) {
        simulator.gaitType = type
        sampleBuffer.reset()
        _uiState.update { it.copy(gaitType = type) }
    }

    /**
     * Inject a synthetic anomaly:
     *  1. Wait 10 s  (countdown visible via [isInjecting])
     *  2. Switch to SHUFFLE for 5 s  → anomaly alert fires naturally
     *  3. Restore the original gait type
     *
     * The dropdown on DebugScreen reflects these changes automatically because
     * [setGaitType] updates [uiState.gaitType].
     */
    fun injectAnomaly() {
        if (_uiState.value.isInjecting) return
        val originalType = _uiState.value.gaitType
        _uiState.update { it.copy(isInjecting = true) }

        viewModelScope.launch {
            delay(10_000L)                                    // 10 s pre-injection delay
            setGaitType(GaitSimulator.GaitType.SHUFFLING)     // switch → anomalous gait
            delay(15_000L)                                     // 5 s anomaly window
            setGaitType(originalType)                         // restore
            _uiState.update { it.copy(isInjecting = false) }
        }
    }

    /**
     * Re-read enrollment data from SharedPreferences.
     * Call on ON_RESUME so DebugScreen picks up a fresh threshold after enrollment.
     */
    fun refreshEnrollment() {
        val prefs     = getApplication<Application>()
            .getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val enrolled  = prefs.getBoolean(MainActivity.KEY_ENROLLED,  false)
        val threshold = prefs.getFloat(  MainActivity.KEY_THRESHOLD, 1f)
        _uiState.update { it.copy(enrolled = enrolled, threshold = threshold) }
    }

    override fun onCleared() {
        simulator.stop()
        autoencoder.close()
    }
}
