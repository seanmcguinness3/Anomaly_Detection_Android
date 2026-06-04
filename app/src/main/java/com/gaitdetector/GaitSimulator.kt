package com.gaitdetector

import kotlin.math.*

/**
 * Produces synthetic accelerometer samples at 50 Hz on a background thread.
 *
 * The gait model mirrors generate_gait_data.py so that reconstruction errors
 * experienced in the app match what was evaluated during Python training.
 *
 * Axes convention:
 *   [0] acc_x  forward/backward
 *   [1] acc_y  lateral
 *   [2] acc_z  vertical (dominant walking oscillation)
 */
class GaitSimulator(private val onSample: (FloatArray) -> Unit) {

    enum class GaitType(val label: String) {
        NORMAL("Normal Walking"),
        LIMPING("Limping"),
        SHUFFLING("Shuffling"),
        RUNNING("Running"),
        ATAXIC("Ataxic")
    }

    @Volatile var gaitType: GaitType = GaitType.NORMAL

    private val random = java.util.Random()
    private var thread: Thread? = null
    @Volatile private var running = false
    private var t = 0.0
    private val dt = 1.0 / FS.toDouble()

    fun start() {
        if (running) return
        running = true
        t = 0.0
        thread = Thread {
            while (running) {
                onSample(nextSample())
                t += dt
                try { Thread.sleep(PERIOD_MS) } catch (_: InterruptedException) { break }
            }
        }.apply {
            name = "GaitSimulator"
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running = false
        thread?.interrupt()
        thread?.join(500)
        thread = null
    }

    private fun nextSample(): FloatArray = when (gaitType) {
        GaitType.NORMAL    -> normalSample()
        GaitType.LIMPING   -> limpingSample()
        GaitType.SHUFFLING -> shufflingSample()
        GaitType.RUNNING   -> runningSample()
        GaitType.ATAXIC    -> ataxicSample()
    }

    // -------------------------------------------------------------------------
    // Individual gait models
    // -------------------------------------------------------------------------

    private fun normalSample(): FloatArray {
        val f = 1.9
        return floatArrayOf(
            (0.10 * sin(2 * PI * f * t + PI / 4) + noise(0.030)).toFloat(),
            (0.15 * sin(2 * PI * f * t + PI / 2) + noise(0.030)).toFloat(),
            (1.00 + 0.70 * sin(2 * PI * f * t) + 0.30 * sin(4 * PI * f * t) + noise(0.050)).toFloat()
        )
    }

    private fun limpingSample(): FloatArray {
        val f = 1.7
        val limpMod = 1.0 + 0.5 * sign(sin(PI * f * t))
        return floatArrayOf(
            (limpMod * 0.20 * sin(2 * PI * f * t + PI / 4) + noise(0.030)).toFloat(),
            (0.30 * sin(2 * PI * f * t + PI / 2) + noise(0.030)).toFloat(),
            (1.00 + limpMod * 0.40 * sin(2 * PI * f * t) + noise(0.050)).toFloat()
        )
    }

    private fun shufflingSample(): FloatArray {
        val f = 1.4
        return floatArrayOf(
            (0.04 * sin(2 * PI * f * t + PI / 4) + noise(0.030)).toFloat(),
            (0.06 * sin(2 * PI * f * t + PI / 2) + noise(0.030)).toFloat(),
            (1.00 + 0.15 * sin(2 * PI * f * t) + noise(0.050)).toFloat()
        )
    }

    private fun runningSample(): FloatArray {
        val f = 2.8
        return floatArrayOf(
            (0.30 * sin(2 * PI * f * t + PI / 4) + noise(0.050)).toFloat(),
            (0.25 * sin(2 * PI * f * t + PI / 2) + noise(0.050)).toFloat(),
            (2.00 + 1.50 * sin(2 * PI * f * t) + 0.50 * sin(4 * PI * f * t) + noise(0.100)).toFloat()
        )
    }

    // Ataxic: phase-jittered, variable amplitude
    private var ataxicPhase = 0.0
    private val ataxicAmpTarget = doubleArrayOf(1.0)
    private var ataxicAmp = 1.0

    private fun ataxicSample(): FloatArray {
        val baseFreq = 1.8
        ataxicPhase += 2 * PI * baseFreq * dt + noise(0.3) * dt
        ataxicAmp += (random.nextGaussian() * 0.05)
        ataxicAmp = ataxicAmp.coerceIn(0.2, 2.0)
        return floatArrayOf(
            (ataxicAmp * 0.15 * sin(ataxicPhase + PI / 4) + noise(0.040)).toFloat(),
            (ataxicAmp * 0.20 * sin(ataxicPhase + PI / 2) + noise(0.040)).toFloat(),
            (1.00 + ataxicAmp * 0.60 * sin(ataxicPhase) + 0.20 * sin(2 * ataxicPhase) + noise(0.060)).toFloat()
        )
    }

    private fun noise(sigma: Double) = random.nextGaussian() * sigma

    companion object {
        const val FS = 50
        const val PERIOD_MS = 20L
        const val WINDOW_SIZE = 128
        const val STRIDE = 64
    }
}
