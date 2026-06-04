package com.gaitdetector

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.LinkedList

/**
 * Scrolling line chart that shows recent reconstruction error scores and
 * draws a dashed horizontal line at the current anomaly threshold.
 */
class ScoreHistoryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val scores = LinkedList<Float>()
    private var currentThreshold = 1f
    private val maxPoints = 60

    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2196F3")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val thresholdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

    private val anomalyFillPaint = Paint().apply {
        color = Color.parseColor("#30F44336")
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80AAAAAA")
        strokeWidth = 1f
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = 28f
    }

    fun addScore(score: Float, threshold: Float) {
        currentThreshold = threshold
        scores.addLast(score)
        if (scores.size > maxPoints) scores.removeFirst()
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (scores.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val padLeft = 10f
        val padRight = 10f
        val padTop = 20f
        val padBottom = 30f
        val chartW = w - padLeft - padRight
        val chartH = h - padTop - padBottom

        // Dynamic Y range: 0 to max(scores ∪ threshold) * 1.3
        val maxVal = maxOf(scores.max()!!, currentThreshold) * 1.3f
        fun toY(v: Float) = padTop + chartH * (1f - v / maxVal)
        fun toX(i: Int) = padLeft + i * (chartW / (maxPoints - 1))

        // Axis
        canvas.drawLine(padLeft, padTop, padLeft, h - padBottom, axisPaint)
        canvas.drawLine(padLeft, h - padBottom, w - padRight, h - padBottom, axisPaint)

        // Anomaly fill (above threshold)
        val threshY = toY(currentThreshold)
        canvas.drawRect(padLeft, padTop, w - padRight, threshY, anomalyFillPaint)

        // Threshold line
        canvas.drawLine(padLeft, threshY, w - padRight, threshY, thresholdPaint)
        canvas.drawText("threshold", padLeft + 4f, threshY - 6f, labelPaint)

        // Score line
        val path = android.graphics.Path()
        scores.forEachIndexed { index, score ->
            val x = toX(maxPoints - scores.size + index)
            val y = toY(score)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, scorePaint)
    }
}
