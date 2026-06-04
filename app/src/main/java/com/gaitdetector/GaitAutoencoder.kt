package com.gaitdetector

import android.content.Context
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.pow

/**
 * Wraps the TFLite autoencoder model and Z-score scaler.
 *
 * Assets required (copy from python/models/ after training):
 *   gait_autoencoder.tflite
 *   scaler_params.json
 *
 * Anomaly score = mean-squared reconstruction error in normalised feature space.
 * Higher score → more anomalous.
 */
class GaitAutoencoder(context: Context) {

    private val interpreter: Interpreter
    private val scalerMean: FloatArray
    private val scalerStd: FloatArray
    val suggestedBaseThreshold: Float

    init {
        interpreter = Interpreter(loadModelFile(context))

        val json = JSONObject(context.assets.open("scaler_params.json").bufferedReader().readText())
        val meanArr = json.getJSONArray("mean")
        val stdArr  = json.getJSONArray("std")

        scalerMean = FloatArray(meanArr.length()) { meanArr.getDouble(it).toFloat() }
        scalerStd  = FloatArray(stdArr.length())  { stdArr.getDouble(it).toFloat() }
        suggestedBaseThreshold = json.optDouble("suggested_threshold", 1.0).toFloat()
    }

    /**
     * Returns the MSE reconstruction error for the given raw feature vector.
     * Features must be in the same order produced by GaitFeatureExtractor.
     */
    fun reconstructionError(features: FloatArray): Float {
        require(features.size == GaitFeatureExtractor.NUM_FEATURES)

        val normalised = FloatArray(features.size) {
            (features[it] - scalerMean[it]) / scalerStd[it]
        }

        val inputBuffer  = Array(1) { normalised }
        val outputBuffer = Array(1) { FloatArray(features.size) }
        interpreter.run(inputBuffer, outputBuffer)

        val reconstruction = outputBuffer[0]
        return normalised.indices.sumOf {
            (normalised[it] - reconstruction[it]).toDouble().pow(2)
        }.toFloat() / features.size
    }

    fun close() = interpreter.close()

    // -------------------------------------------------------------------------

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fd = context.assets.openFd("gait_autoencoder.tflite")
        return FileInputStream(fd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
        )
    }
}
