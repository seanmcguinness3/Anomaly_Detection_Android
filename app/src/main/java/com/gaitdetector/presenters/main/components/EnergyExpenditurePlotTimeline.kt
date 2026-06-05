package com.gaitdetector.presenters.main.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.gaitdetector.R

@Composable
fun EnergyExpenditurePlotTimeline(modifier: Modifier = Modifier) {
    val items = listOf(
        Pair("9", "AM"), Pair("11", "AM"), Pair("1", "PM"),
        Pair("3", "PM"), Pair("5", "PM"), Pair("7", "PM"), Pair("9", "PM"),
    )
    Column(
        modifier              = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(start = 30.dp, end = 8.dp),
        verticalArrangement   = Arrangement.spacedBy(dimensionResource(R.dimen.box_vertical_arrangement_spacing)),
    ) {
        CircleRow()
        TimeTextRow(items)
    }
}

fun Painter.toImageBitmap(size: Size, density: Density, layoutDirection: LayoutDirection): ImageBitmap {
    val bitmap = ImageBitmap(size.width.toInt(), size.height.toInt())
    val canvas = androidx.compose.ui.graphics.Canvas(bitmap)
    CanvasDrawScope().draw(density, layoutDirection, canvas, size) { draw(size) }
    return bitmap
}

@Composable
fun CircleRow() {
    val imagePainter = painterResource(R.drawable.ic_run)
    val image = imagePainter.toImageBitmap(Size(50f, 50f), Density(1f), LayoutDirection.Ltr)

    Row(
        modifier              = Modifier.fillMaxWidth().background(Color.Black),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        repeat(6) {
            DrawCircle(dimensionResource(R.dimen.twenty_four_hour_chart_big_circle_size))
            repeat(3) { DrawCircle(dimensionResource(R.dimen.twenty_four_hour_chart_small_circle_size)) }
        }
        DrawCircle(dimensionResource(R.dimen.twenty_four_hour_chart_big_circle_size))
    }

    Canvas(modifier = Modifier.fillMaxWidth()) {
        val xPos   = 650f
        val length = 170f
        val y      = -65f
        drawLine(
            start       = Offset(xPos, y + 25),
            end         = Offset(xPos + length, y + 25),
            color       = Color.White,
            strokeWidth = 5.dp.toPx(),
            cap         = StrokeCap.Round,
        )
        drawImage(image = image, topLeft = Offset(xPos - image.width / 2f, y))
    }
}

@Composable
fun TimeTextRow(items: List<Pair<String, String>>) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier              = Modifier.fillMaxWidth(),
    ) {
        items.forEach { (time, period) ->
            Text(
                text     = stringResource(R.string.time_text, time, period),
                color    = Color.DarkGray,
                style    = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.box_horizontal_padding)),
            )
        }
    }
}

@Composable
fun DrawCircle(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        drawCircle(color = Color.DarkGray, radius = size.toPx() / 2)
    }
}
