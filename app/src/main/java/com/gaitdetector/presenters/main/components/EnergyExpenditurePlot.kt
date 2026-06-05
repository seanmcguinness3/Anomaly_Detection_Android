package com.gaitdetector.presenters.main.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitdetector.R

@Composable
fun EnergyExpenditurePlot(
    dataList: List<Pair<Int, Color>>,
    lineData: List<Int>,
    modifier: Modifier = Modifier,
) {
    val numberOfGridLines = 4
    Column {
        Row(
            modifier = modifier
                .background(Color.Black)
                .padding(vertical = dimensionResource(R.dimen.energy_chart_row_vertical_padding))
        ) {
            Column(
                modifier            = Modifier.height(208.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = stringResource(R.string.hundred_text),      color = Color.Gray, fontSize = 12.sp)
                Text(text = stringResource(R.string.seventy_five_text), color = Color.Gray, fontSize = 12.sp)
                Text(text = stringResource(R.string.fifty_text),        color = Color.Gray, fontSize = 12.sp)
                Text(text = stringResource(R.string.twenty_five_text),  color = Color.Gray, fontSize = 12.sp)
                Text(text = stringResource(R.string.zero_text),         color = Color.Gray, fontSize = 12.sp)
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(194.dp)
                    .background(Color.Black)
                    .padding(top = 6.dp, start = 2.dp)
            ) {
                val totalBars   = dataList.size
                val barWidth    = size.width / totalBars.toFloat()
                val chartHeight = size.height
                val chartWidth  = size.width
                val barSpace       = 0.54f
                val actualBarWidth = barWidth * barSpace
                val gridLineSpacing = chartHeight / numberOfGridLines

                for (i in 0..numberOfGridLines) {
                    drawLine(
                        color       = Color.Gray,
                        start       = Offset(0f, i * gridLineSpacing),
                        end         = Offset(chartWidth, i * gridLineSpacing),
                        strokeWidth = 2f,
                    )
                }

                var currentX = 0f
                dataList.forEach { (value, color) ->
                    drawRect(
                        color    = color,
                        topLeft  = Offset(currentX, size.height - (value * size.height / 100)),
                        size     = Size(actualBarWidth, value * size.height / 100f),
                    )
                    currentX += barWidth
                }

                val path = Path().apply {
                    lineData.forEachIndexed { index, value ->
                        if (index == 0) moveTo(0f, chartHeight - (value * chartHeight / 100))
                        else lineTo(index * barWidth, chartHeight - (value * chartHeight / 100))
                    }
                }
                val fillPath = Path().apply {
                    moveTo(0f, chartHeight)
                    lineData.forEachIndexed { index, value ->
                        if (index == 0) moveTo(0f, chartHeight - (value * chartHeight / 2000))
                        else lineTo(index * barWidth, chartHeight - (value * chartHeight / 100))
                    }
                    lineTo(chartWidth, chartHeight); close()
                }

                drawPath(path = fillPath, color = Color.White.copy(alpha = 0.21f), style = Fill)
                drawPath(path = path,     color = Color.White,                     style = Stroke(width = 4f))
            }
        }
        EnergyExpenditurePlotTimeline(modifier = Modifier)
    }
}
