package com.gaitdetector

import kotlin.math.sqrt

/**
 * Extracts an 18-element feature vector from a 128-sample accelerometer window.
 *
 * Features (identical to extract_features() in generate_gait_data.py):
 *   For each of the 3 axes (x, y, z):
 *     [0] mean
 *     [1] std
 *     [2] min
 *     [3] max
 *     [4] rms
 *     [5] zero-crossing rate (relative to axis mean)
 *   Total = 6 × 3 = 18 features
 */
object GaitFeatureExtractor {

    const val NUM_FEATURES = 18

    /**
     * @param window  Ring buffer contents as FloatArray of size WINDOW_SIZE × 3,
     *                laid out as [sample0_x, sample0_y, sample0_z, sample1_x, …]
     */
    fun extract(window: FloatArray): FloatArray {
        require(window.size == GaitSimulator.WINDOW_SIZE * 3)
        val features = FloatArray(NUM_FEATURES)
        for (axis in 0..2) {
            val col = FloatArray(GaitSimulator.WINDOW_SIZE) { i -> window[i * 3 + axis] }
            val mean = col.mean()
            val variance = col.sumOf { v -> ((v - mean) * (v - mean)).toDouble() }.toFloat() / col.size
            val std = sqrt(variance)
            val min = col.min()
            val max = col.max()
            val rms = sqrt(col.sumOf { v -> (v * v).toDouble() }.toFloat() / col.size)

            // Zero-crossings relative to axis mean
            var zcr = 0
            for (i in 0 until col.size - 1) {
                val a = col[i] - mean
                val b = col[i + 1] - mean
                if ((a >= 0f && b < 0f) || (a < 0f && b >= 0f)) zcr++
            }

            val base = axis * 6
            features[base + 0] = mean
            features[base + 1] = std
            features[base + 2] = min
            features[base + 3] = max
            features[base + 4] = rms
            features[base + 5] = zcr.toFloat() / GaitSimulator.WINDOW_SIZE
        }
        return features
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun FloatArray.mean(): Float = if (isEmpty()) 0f else sum() / size
    private fun FloatArray.min(): Float { var m = Float.MAX_VALUE; for (v in this) if (v < m) m = v; return m }
    private fun FloatArray.max(): Float { var m = -Float.MAX_VALUE; for (v in this) if (v > m) m = v; return m }
    private fun FloatArray.sumOf(f: (Float) -> Double): Float = fold(0.0) { acc, v -> acc + f(v) }.toFloat()
}
