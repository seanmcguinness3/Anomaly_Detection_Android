package com.gaitdetector

/**
 * Thread-safe circular buffer that accumulates accelerometer samples and
 * fires a callback with a flat FloatArray of size WINDOW_SIZE×3 every STRIDE samples.
 */
class SampleBuffer(private val onWindow: (FloatArray) -> Unit) {

    private val buf = FloatArray(GaitSimulator.WINDOW_SIZE * 3)
    private var count = 0   // total samples received since last stride reset
    private var head = 0    // next write position (circular)
    private var sinceLastWindow = 0

    @Synchronized
    fun addSample(sample: FloatArray) {
        // Write sample (3 floats) at current head
        val base = head * 3
        buf[base]     = sample[0]
        buf[base + 1] = sample[1]
        buf[base + 2] = sample[2]
        head = (head + 1) % GaitSimulator.WINDOW_SIZE
        if (count < GaitSimulator.WINDOW_SIZE) count++
        sinceLastWindow++

        if (count == GaitSimulator.WINDOW_SIZE && sinceLastWindow >= GaitSimulator.STRIDE) {
            sinceLastWindow = 0
            onWindow(copyWindow())
        }
    }

    fun reset() {
        synchronized(this) {
            count = 0
            head = 0
            sinceLastWindow = 0
        }
    }

    // Flatten in chronological order (oldest first)
    private fun copyWindow(): FloatArray {
        val out = FloatArray(GaitSimulator.WINDOW_SIZE * 3)
        val oldest = head  // head points to the oldest sample in a full ring
        for (i in 0 until GaitSimulator.WINDOW_SIZE) {
            val src = ((oldest + i) % GaitSimulator.WINDOW_SIZE) * 3
            val dst = i * 3
            out[dst]     = buf[src]
            out[dst + 1] = buf[src + 1]
            out[dst + 2] = buf[src + 2]
        }
        return out
    }
}
