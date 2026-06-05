package com.gaitdetector

import android.content.Context
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Trainable dense decoder network: 32 → 64 (ReLU) → 192 (ReLU) → 384 (Linear)
 *
 * Base weights are loaded from assets/decoder_weights.json (produced by
 * train_autoencoder.py). During the enrolment session the decoder is fine-tuned
 * on the enrolled user's normal gait via mini-batch stochastic gradient descent,
 * then saved to internal storage. On subsequent launches the fine-tuned weights
 * are restored automatically.
 *
 * Weight layout (row-major):
 *   w[inIdx * outDim + outIdx] = weight from input neuron inIdx to output neuron outIdx
 *
 * Forward pass for one layer:
 *   out[j] = activation( b[j] + Σ_i  w[i * outDim + j] * x[i] )
 */
class DecoderNetwork(private val context: Context) {

    // ── Architecture ──────────────────────────────────────────────────────────
    private val dimIn  = 32    // bottleneck input  (must match encoder output)
    private val dim1   = 64    // hidden layer 1
    private val dim2   = 192   // hidden layer 2
    private val dimOut = 384   // output  (must match NUM_FEATURES)

    // ── Mutable weights (updated during fine-tuning) ──────────────────────────
    private var w1 = FloatArray(dimIn  * dim1)
    private var b1 = FloatArray(dim1)
    private var w2 = FloatArray(dim1   * dim2)
    private var b2 = FloatArray(dim2)
    private var w3 = FloatArray(dim2   * dimOut)
    private var b3 = FloatArray(dimOut)

    // ── Base weights snapshot (for reset / re-enrolment) ─────────────────────
    private val baseW1: FloatArray
    private val baseB1: FloatArray
    private val baseW2: FloatArray
    private val baseB2: FloatArray
    private val baseW3: FloatArray
    private val baseB3: FloatArray

    var isPersonalised: Boolean = false
        private set

    init {
        // 1. Load base weights from assets
        loadBaseWeights()

        // Snapshot for reset
        baseW1 = w1.copyOf(); baseB1 = b1.copyOf()
        baseW2 = w2.copyOf(); baseB2 = b2.copyOf()
        baseW3 = w3.copyOf(); baseB3 = b3.copyOf()

        // 2. Try to restore fine-tuned weights from previous enrolment
        isPersonalised = loadFineTunedWeights()
    }

    // ── Forward pass ──────────────────────────────────────────────────────────

    /** Run the decoder forward pass. Returns a 384-element reconstruction. */
    fun forward(embedding: FloatArray): FloatArray {
        val h1  = denseRelu(embedding, w1, b1, dimIn, dim1)
        val h2  = denseRelu(h1,        w2, b2, dim1, dim2)
        return   dense     (h2,        w3, b3, dim2, dimOut)
    }

    // ── Fine-tuning ───────────────────────────────────────────────────────────

    /**
     * Fine-tune the decoder using mini-batch SGD.
     *
     * @param embeddings  List of encoder outputs collected during enrolment.
     * @param targets     Corresponding normalised feature vectors (reconstruction targets).
     * @param epochs      Number of passes over the enrolment data.
     * @param learningRate  SGD step size.
     * @param batchSize   Mini-batch size (capped to dataset size automatically).
     * @param onProgress  Optional callback invoked at the end of each epoch with
     *                    (epochNumber [1-based], mean MSE loss).
     */
    fun fineTune(
        embeddings:    List<FloatArray>,
        targets:       List<FloatArray>,
        epochs:        Int   = 50,
        learningRate:  Float = 1e-3f,
        batchSize:     Int   = 16,
        onProgress:    ((epoch: Int, loss: Float) -> Unit)? = null,
    ) {
        val n         = embeddings.size
        val effective = batchSize.coerceIn(1, n)
        val indices   = (0 until n).toMutableList()

        repeat(epochs) { ep ->
            indices.shuffle()
            var epochLoss  = 0f
            var numBatches = 0
            var start      = 0

            while (start < n) {
                val end   = minOf(start + effective, n)
                val batch = indices.subList(start, end)

                // Gradient accumulators
                val dW1 = FloatArray(w1.size)
                val dB1 = FloatArray(b1.size)
                val dW2 = FloatArray(w2.size)
                val dB2 = FloatArray(b2.size)
                val dW3 = FloatArray(w3.size)
                val dB3 = FloatArray(b3.size)
                var batchLoss = 0f

                for (idx in batch) {
                    val x      = embeddings[idx]
                    val target = targets[idx]
                    val cache  = forwardWithCache(x)

                    // MSE loss gradient w.r.t. output:  dL/dout_j = (2/dimOut)*(out_j - target_j)
                    val dOut = FloatArray(dimOut) { j ->
                        (2f / dimOut) * (cache.out[j] - target[j])
                    }
                    batchLoss += FloatArray(dimOut) { j ->
                        val d = cache.out[j] - target[j]; d * d
                    }.sum() / dimOut

                    // ── Layer 3  (h2 → out, linear) ───────────────────────
                    for (i in 0 until dim2) {
                        val h2i = cache.h2[i]
                        val base = i * dimOut
                        for (j in 0 until dimOut) dW3[base + j] += h2i * dOut[j]
                    }
                    for (j in 0 until dimOut) dB3[j] += dOut[j]

                    // dL/dh2  ×  ReLU'(pre2)
                    val dH2 = FloatArray(dim2) { i ->
                        if (cache.pre2[i] <= 0f) return@FloatArray 0f
                        var g = 0f
                        val base = i * dimOut
                        for (j in 0 until dimOut) g += w3[base + j] * dOut[j]
                        g
                    }

                    // ── Layer 2  (h1 → h2, ReLU) ──────────────────────────
                    for (i in 0 until dim1) {
                        val h1i = cache.h1[i]
                        val base = i * dim2
                        for (j in 0 until dim2) dW2[base + j] += h1i * dH2[j]
                    }
                    for (j in 0 until dim2) dB2[j] += dH2[j]

                    // dL/dh1  ×  ReLU'(pre1)
                    val dH1 = FloatArray(dim1) { i ->
                        if (cache.pre1[i] <= 0f) return@FloatArray 0f
                        var g = 0f
                        val base = i * dim2
                        for (j in 0 until dim2) g += w2[base + j] * dH2[j]
                        g
                    }

                    // ── Layer 1  (x → h1, ReLU) ───────────────────────────
                    for (i in 0 until dimIn) {
                        val xi = x[i]
                        val base = i * dim1
                        for (j in 0 until dim1) dW1[base + j] += xi * dH1[j]
                    }
                    for (j in 0 until dim1) dB1[j] += dH1[j]
                }

                // Average gradients over batch and apply SGD update
                val invBatch = learningRate / batch.size.toFloat()
                for (i in w1.indices) w1[i] -= invBatch * dW1[i]
                for (i in b1.indices) b1[i] -= invBatch * dB1[i]
                for (i in w2.indices) w2[i] -= invBatch * dW2[i]
                for (i in b2.indices) b2[i] -= invBatch * dB2[i]
                for (i in w3.indices) w3[i] -= invBatch * dW3[i]
                for (i in b3.indices) b3[i] -= invBatch * dB3[i]

                epochLoss += batchLoss
                numBatches++
                start = end
            }

            onProgress?.invoke(ep + 1, epochLoss / numBatches.coerceAtLeast(1))
        }

        isPersonalised = true
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    /** Save fine-tuned weights to internal storage. */
    fun saveWeights() {
        val file = File(context.filesDir, WEIGHTS_FILE)
        DataOutputStream(BufferedOutputStream(FileOutputStream(file))).use { dos ->
            for (f in w1) dos.writeFloat(f)
            for (f in b1) dos.writeFloat(f)
            for (f in w2) dos.writeFloat(f)
            for (f in b2) dos.writeFloat(f)
            for (f in w3) dos.writeFloat(f)
            for (f in b3) dos.writeFloat(f)
        }
    }

    /** Reload base weights and clear personalisation. */
    fun resetToBase() {
        w1 = baseW1.copyOf(); b1 = baseB1.copyOf()
        w2 = baseW2.copyOf(); b2 = baseB2.copyOf()
        w3 = baseW3.copyOf(); b3 = baseB3.copyOf()
        File(context.filesDir, WEIGHTS_FILE).delete()
        isPersonalised = false
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private data class Cache(
        val pre1: FloatArray, val h1: FloatArray,
        val pre2: FloatArray, val h2: FloatArray,
        val out:  FloatArray,
    )

    /** Forward pass that retains pre-activation values needed for backprop. */
    private fun forwardWithCache(x: FloatArray): Cache {
        val pre1 = FloatArray(dim1)
        val h1   = FloatArray(dim1)
        for (j in 0 until dim1) {
            var s = b1[j]
            val base = j   // w1[i * dim1 + j]
            for (i in 0 until dimIn) s += w1[i * dim1 + base] * x[i]
            pre1[j] = s; h1[j] = maxOf(0f, s)
        }
        val pre2 = FloatArray(dim2)
        val h2   = FloatArray(dim2)
        for (j in 0 until dim2) {
            var s = b2[j]
            for (i in 0 until dim1) s += w2[i * dim2 + j] * h1[i]
            pre2[j] = s; h2[j] = maxOf(0f, s)
        }
        val out = FloatArray(dimOut)
        for (j in 0 until dimOut) {
            var s = b3[j]
            for (i in 0 until dim2) s += w3[i * dimOut + j] * h2[i]
            out[j] = s
        }
        return Cache(pre1, h1, pre2, h2, out)
    }

    private fun denseRelu(
        x: FloatArray, w: FloatArray, b: FloatArray, inSize: Int, outSize: Int
    ): FloatArray = FloatArray(outSize) { j ->
        var s = b[j]
        for (i in 0 until inSize) s += w[i * outSize + j] * x[i]
        maxOf(0f, s)
    }

    private fun dense(
        x: FloatArray, w: FloatArray, b: FloatArray, inSize: Int, outSize: Int
    ): FloatArray = FloatArray(outSize) { j ->
        var s = b[j]
        for (i in 0 until inSize) s += w[i * outSize + j] * x[i]
        s
    }

    private fun loadBaseWeights() {
        val json = JSONObject(
            context.assets.open("decoder_weights.json").bufferedReader().readText()
        )
        fun fill(name: String, w: FloatArray, b: FloatArray) {
            val layer = json.getJSONObject(name)
            val wArr  = layer.getJSONArray("w")
            val bArr  = layer.getJSONArray("b")
            for (i in w.indices) w[i] = wArr.getDouble(i).toFloat()
            for (i in b.indices) b[i] = bArr.getDouble(i).toFloat()
        }
        fill("dec1",           w1, b1)
        fill("dec2",           w2, b2)
        fill("reconstruction", w3, b3)
    }

    private fun loadFineTunedWeights(): Boolean {
        val file = File(context.filesDir, WEIGHTS_FILE)
        if (!file.exists()) return false
        return try {
            DataInputStream(BufferedInputStream(FileInputStream(file))).use { dis ->
                for (i in w1.indices) w1[i] = dis.readFloat()
                for (i in b1.indices) b1[i] = dis.readFloat()
                for (i in w2.indices) w2[i] = dis.readFloat()
                for (i in b2.indices) b2[i] = dis.readFloat()
                for (i in w3.indices) w3[i] = dis.readFloat()
                for (i in b3.indices) b3[i] = dis.readFloat()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val WEIGHTS_FILE = "decoder_finetuned.bin"
    }
}
