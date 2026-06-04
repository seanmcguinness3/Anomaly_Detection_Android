package com.gaitdetector

import kotlin.math.*

/**
 * Pure-Kotlin Cooley-Tukey radix-2 FFT for real-valued input.
 *
 * Only magnitudeSpectrum() is used externally; it returns the N/2+1
 * non-redundant magnitude bins (DC to Nyquist) for a real input of
 * length N (N must be a power of 2).
 */
object FFT {

    /**
     * Returns magnitude spectrum of length N/2 + 1.
     *   bin k  ↔  frequency  k * Fs / N
     * Input length must be a power of 2.
     */
    fun magnitudeSpectrum(x: FloatArray): FloatArray {
        val n    = x.size
        val re   = DoubleArray(n) { x[it].toDouble() }
        val im   = DoubleArray(n)                       // imaginary part = 0 for real input
        computeFFT(re, im)
        // Only bins 0 .. N/2 are unique for a real-valued input
        return FloatArray(n / 2 + 1) { k ->
            sqrt(re[k] * re[k] + im[k] * im[k]).toFloat()
        }
    }

    // ── In-place iterative Cooley-Tukey DIT FFT ────────────────────────────

    private fun computeFFT(re: DoubleArray, im: DoubleArray) {
        val n = re.size

        // Bit-reversal permutation
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                    t = im[i]; im[i] = im[j]; im[j] = t
            }
        }

        // Butterfly passes
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wRe = cos(ang)
            val wIm = sin(ang)
            var i = 0
            while (i < n) {
                var curRe = 1.0
                var curIm = 0.0
                for (jj in 0 until len / 2) {
                    val uRe = re[i + jj]
                    val uIm = im[i + jj]
                    val vRe = re[i + jj + len / 2] * curRe - im[i + jj + len / 2] * curIm
                    val vIm = re[i + jj + len / 2] * curIm + im[i + jj + len / 2] * curRe
                    re[i + jj]          = uRe + vRe
                    im[i + jj]          = uIm + vIm
                    re[i + jj + len / 2] = uRe - vRe
                    im[i + jj + len / 2] = uIm - vIm
                    val newRe = curRe * wRe - curIm * wIm
                    curIm     = curRe * wIm + curIm * wRe
                    curRe     = newRe
                }
                i += len
            }
            len = len shl 1
        }
    }
}
