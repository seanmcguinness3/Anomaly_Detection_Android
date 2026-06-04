package com.gaitdetector

import kotlin.math.sqrt

/**
 * Converts a 128-sample accelerometer window into a 96-element FFT feature vector.
 *
 * Processing pipeline (identical to process_window() in generate_gait_data.py):
 *
 *   For each of 3 axes (x, y, z):
 *     1. Z-score normalise the axis window          → scale invariance
 *     2. FFT magnitude spectrum  |FFT(x_norm)|      → phase invariance
 *     3. Resample spectrum so fundamental → K_REF   → frequency invariance
 *
 *   Concatenate 3 × N_OUT_BINS = 96 floats.
 *
 * Constants mirror generate_gait_data.py:
 *   FUND_LO / FUND_HI  : bin range searched for the fundamental peak
 *   K_REF              : target bin for the aligned fundamental (~1.95 Hz)
 *   N_OUT_BINS         : number of tuned-spectrum bins kept per axis
 */
object GaitFeatureExtractor {

    const val NUM_FEATURES = 96   // 3 axes × 32 bins

    private const val FUND_LO    = 2
    private const val FUND_HI    = 20
    private const val K_REF      = 5
    private const val N_OUT_BINS = 32

    /**
     * @param window Flat FloatArray of size WINDOW_SIZE*3,
     *               layout: [s0_x, s0_y, s0_z,  s1_x, s1_y, s1_z, …]
     * @return FloatArray of size NUM_FEATURES (96)
     */
    fun extract(window: FloatArray): FloatArray {
        require(window.size == GaitSimulator.WINDOW_SIZE * 3) {
            "Expected ${GaitSimulator.WINDOW_SIZE * 3} values, got ${window.size}"
        }
        val features = FloatArray(NUM_FEATURES)
        for (axis in 0..2) {
            val col   = FloatArray(GaitSimulator.WINDOW_SIZE) { i -> window[i * 3 + axis] }
            val tuned = processAxis(col)
            val base  = axis * N_OUT_BINS
            tuned.copyInto(features, destinationOffset = base)
        }
        return features
    }

    // ── Per-axis pipeline ─────────────────────────────────────────────────────

    private fun processAxis(col: FloatArray): FloatArray {
        // 1. Z-score normalise
        val mean = col.mean()
        val std  = col.std(mean).coerceAtLeast(1e-8f)
        val norm = FloatArray(col.size) { (col[it] - mean) / std }

        // 2. FFT magnitude spectrum  → bins 0 .. WINDOW_SIZE/2  (65 values)
        val mag = FFT.magnitudeSpectrum(norm)

        // 3. Find fundamental: peak bin in [FUND_LO, FUND_HI]
        var kFund = FUND_LO
        for (k in (FUND_LO + 1)..FUND_HI) {
            if (k < mag.size && mag[k] > mag[kFund]) kFund = k
        }

        // 4. Resample: output[k] ← linear interp of mag at position k / scale
        //    scale = K_REF / kFund  maps  kFund → K_REF  (and all harmonics)
        val scale = K_REF.toFloat() / kFund.toFloat()
        return FloatArray(N_OUT_BINS) { k ->
            val src  = k / scale
            val lo   = src.toInt()
            val hi   = lo + 1
            val frac = src - lo
            val loV  = if (lo < mag.size) mag[lo] else 0f
            val hiV  = if (hi < mag.size) mag[hi] else 0f
            loV * (1f - frac) + hiV * frac
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun FloatArray.mean(): Float = if (isEmpty()) 0f else sum() / size

    private fun FloatArray.std(mean: Float): Float {
        if (size < 2) return 0f
        var acc = 0.0
        for (v in this) acc += (v - mean).toDouble() * (v - mean).toDouble()
        return sqrt((acc / size).toFloat())
    }
}
