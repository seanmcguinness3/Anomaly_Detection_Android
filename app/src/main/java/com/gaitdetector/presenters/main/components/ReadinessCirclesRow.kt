package com.gaitdetector.presenters.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import com.gaitdetector.R

@Composable
fun ReadinessCirclesRow(modifier: Modifier = Modifier) {
    Row(
        modifier              = modifier.fillMaxWidth().background(Color.Black),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        DrawCircle(dimensionResource(R.dimen.twenty_four_hour_chart_big_circle_size))
        repeat(26) { DrawCircle(dimensionResource(R.dimen.twenty_four_hour_chart_small_circle_size)) }
        DrawCircle(dimensionResource(R.dimen.twenty_four_hour_chart_big_circle_size))
    }
}
