package com.gaitdetector

import kotlin.math.sqrt

/**
 * Converts a raw 128-sample accelerometer window into a 384-element feature vector.
 *
 * Processing pipeline (identical to process_window() in generate_gait_data.py):
 *
 *   For each of 3 axes (x, y, z):
 *     1. Z-score normalise the axis window independently
 *        → removes DC offset and per-session amplitude bias
 *
 *   Concatenate 3 × WINDOW_SIZE = 384 floats:
 *     [ z_x[0..127],  z_y[0..127],  z_z[0..127] ]
 *
 * The autoencoder learns directly from the normalised time-domain waveform.
 * No FFT is applied — frequency and phase information is preserved in the
 * raw signal and the decoder learns to reconstruct the user's specific
 * waveform shape during personalisation.
 */
object GaitFeatureExtractor {

    /** 128 samples × 3 axes. Must match generate_gait_data.NUM_FEATURES. */
    const val NUM_FEATURES = GaitSimulator.WINDOW_SIZE * 3   // 384

    /**
     * @param window Flat FloatArray of size WINDOW_SIZE × 3,
     *               interleaved layout: [s0_x, s0_y, s0_z,  s1_x, s1_y, s1_z, …]
     * @return FloatArray of size NUM_FEATURES (384) — Z-score normalised per axis
     */
    fun extract(window: FloatArray): FloatArray {
        require(window.size == GaitSimulator.WINDOW_SIZE * 3) {
            "Expected ${GaitSimulator.WINDOW_SIZE * 3} values, got ${window.size}"
        }
        val features = FloatArray(NUM_FEATURES)
        for (axis in 0..2) {
            // De-interleave one axis
            val col = FloatArray(GaitSimulator.WINDOW_SIZE) { i -> window[i * 3 + axis] }

            // Z-score normalise
            val mean = col.mean()
            val std  = col.std(mean).coerceAtLeast(1e-8f)

            // Write normalised samples into the output block for this axis
            val base = axis * GaitSimulator.WINDOW_SIZE
            for (i in col.indices) {
                features[base + i] = (col[i] - mean) / std
            }
        }
        return features
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun FloatArray.mean(): Float = if (isEmpty()) 0f else sum() / size

    private fun FloatArray.std(mean: Float): Float {
        if (size < 2) return 0f
        var acc = 0.0
        for (v in this) { val d = v - mean; acc += d * d }
        return sqrt((acc / size).toFloat())
    }
}
