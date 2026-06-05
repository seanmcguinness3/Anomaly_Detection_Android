package com.gaitdetector

import android.content.Context
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Full autoencoder: TFLite encoder (frozen) + Kotlin DecoderNetwork (trainable).
 *
 *   Input  : 384 floats  (128 samples × 3 axes, per-window Z-score normalised)
 *   Encoder: 384 → 192 → 64 → 32   (gait_encoder.tflite, never updated)
 *   Decoder: 32  → 64  → 192 → 384 (DecoderNetwork.kt, fine-tuned at enrolment)
 *   Score  : MSE = mean( (normalised_input − reconstruction)² )
 *
 * Assets required (android/app/src/main/assets/):
 *   gait_encoder.tflite    — encoder-only TFLite model
 *   scaler_params.json     — per-feature Z-score statistics
 *   decoder_weights.json   — base decoder weights (population-level)
 *
 * The decoder is fine-tuned on-device during the enrolment session and
 * persisted to internal storage. On subsequent launches the fine-tuned
 * weights are restored automatically.
 */
class GaitAutoencoder(private val context: Context) {

    private val encoder:   Interpreter
    val         decoder:   DecoderNetwork

    private val scalerMean: FloatArray
    private val scalerStd:  FloatArray
    val featureDim: Int                     // 384

    init {
        encoder = Interpreter(loadModelFile())

        val json    = JSONObject(
            context.assets.open("scaler_params.json").bufferedReader().readText()
        )
        val meanArr = json.getJSONArray("mean")
        val stdArr  = json.getJSONArray("std")
        scalerMean  = FloatArray(meanArr.length()) { meanArr.getDouble(it).toFloat() }
        scalerStd   = FloatArray(stdArr.length())  { stdArr.getDouble(it).toFloat()  }
        featureDim  = scalerMean.size

        decoder = DecoderNetwork(context)
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Full forward pass: normalise → encode → decode → MSE.
     * Used by DetectionActivity every window.
     */
    fun getReconstructionMSE(features: FloatArray): Float {
        val norm  = normalise(features)
        val emb   = encode(norm)
        val recon = decoder.forward(emb)
        return mse(norm, recon)
    }

    /**
     * Returns the normalised feature vector and encoder embedding for one window.
     * Used by EnrollmentActivity to accumulate training data before fine-tuning.
     */
    fun extractForEnrollment(features: FloatArray): EnrollmentSample {
        val norm = normalise(features)
        val emb  = encode(norm)
        return EnrollmentSample(norm, emb)
    }

    /**
     * Fine-tune the decoder on collected enrolment samples.
     * Runs synchronously — call from a background thread.
     *
     * @param samples    Collected during the 30-second walk.
     * @param epochs     Gradient-descent passes over the data.
     * @param lr         SGD learning rate.
     * @param onProgress Callback (epochNumber [1-based], mean MSE) for UI updates.
     */
    fun fineTuneDecoder(
        samples:    List<EnrollmentSample>,
        epochs:     Int   = 50,
        lr:         Float = 1e-3f,
        onProgress: ((epoch: Int, loss: Float) -> Unit)? = null,
    ) {
        decoder.fineTune(
            embeddings  = samples.map { it.embedding },
            targets     = samples.map { it.normalised },
            epochs      = epochs,
            learningRate = lr,
            onProgress  = onProgress,
        )
    }

    /** Persist fine-tuned decoder weights to internal storage. */
    fun saveDecoder() = decoder.saveWeights()

    /** Compute MSE given already-normalised features and a pre-computed embedding. */
    fun mseFromNormalised(normalised: FloatArray, embedding: FloatArray): Float {
        val recon = decoder.forward(embedding)
        return mse(normalised, recon)
    }

    fun close() = encoder.close()

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun normalise(features: FloatArray): FloatArray =
        FloatArray(featureDim) { (features[it] - scalerMean[it]) / scalerStd[it] }

    private fun encode(normalised: FloatArray): FloatArray {
        val embDim    = encoder.getOutputTensor(0).shape()[1]
        val inputBuf  = Array(1) { normalised }
        val outputBuf = Array(1) { FloatArray(embDim) }
        encoder.run(inputBuf, outputBuf)
        return outputBuf[0]
    }

    private fun mse(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) { val d = a[i] - b[i]; sum += d * d }
        return sum / a.size
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fd = context.assets.openFd("gait_encoder.tflite")
        return FileInputStream(fd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
        )
    }
}

/** Holds one window's normalised features and encoder embedding for fine-tuning. */
data class EnrollmentSample(val normalised: FloatArray, val embedding: FloatArray)
