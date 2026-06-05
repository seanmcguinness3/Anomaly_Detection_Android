package com.gaitdetector.presenters.main.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitdetector.R
import com.gaitdetector.ui.theme.FourWeekBottomColor
import com.gaitdetector.ui.theme.FourWeekLineColor
import com.gaitdetector.ui.theme.FourWeekTopColor

@Composable
fun ReadinessPlot(
    data: List<Pair<Float, Float>>,
    modifier: Modifier = Modifier,
) {
    val barWidth      = 29f
    val circleRadius  = 14.5f
    val numberOfGridLines = 4

    Row(
        modifier = modifier.padding(vertical = dimensionResource(R.dimen.four_week_trend_chart_vertical_padding))
    ) {
        Column(
            modifier            = Modifier.fillMaxHeight().padding(end = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.hundred_text),      color = Color.Gray, fontSize = 12.sp)
            Text(stringResource(R.string.seventy_five_text), color = Color.Gray, fontSize = 12.sp)
            Text(stringResource(R.string.fifty_text),        color = Color.Gray, fontSize = 12.sp)
            Text(stringResource(R.string.twenty_five_text),  color = Color.Gray, fontSize = 12.sp)
            Text(stringResource(R.string.zero_text),         color = Color.Gray, fontSize = 12.sp)
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val chartHeight = size.height
            val chartWidth  = size.width
            val spacing     = (chartWidth - barWidth * data.size) / (data.size + 3)
            val gridSpacing = chartHeight / numberOfGridLines

            for (i in 0..numberOfGridLines) {
                drawLine(Color.Gray, Offset(0f, i * gridSpacing), Offset(chartWidth, i * gridSpacing), 2f)
            }
            data.forEachIndexed { index, (minValue, maxValue) ->
                if (minValue == 0f) return@forEachIndexed
                val xOffset   = spacing + index * (barWidth + spacing)
                val yMin      = chartHeight - (minValue / 100f * chartHeight)
                val yMax      = chartHeight - (maxValue / 100f * chartHeight)
                drawRect(FourWeekLineColor, topLeft = Offset(xOffset, yMax), size = Size(barWidth, yMin - yMax))
                drawCircle(FourWeekTopColor,    circleRadius, Offset(xOffset + barWidth / 2, yMax))
                drawCircle(FourWeekBottomColor, circleRadius, Offset(xOffset + barWidth / 2, yMin))
            }
        }
    }
}
