package com.gaitdetector.presenters.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gaitdetector.ui.theme.firstBoxColor
import com.gaitdetector.ui.theme.fourthBoxColor
import com.gaitdetector.ui.theme.secondBoxColor
import com.gaitdetector.ui.theme.thirdBoxColor

@Composable
fun CurrentReadinessBoxes(modifier: Modifier = Modifier) {
    val boxData = listOf(
        Pair(0.15f, firstBoxColor),
        Pair(0.25f, secondBoxColor),
        Pair(0.35f, thirdBoxColor),
        Pair(0.45f, fourthBoxColor),
    )
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        boxData.forEach { CurrentReadinessBoxItem(boxWidth = it.first, boxColor = it.second) }
    }
}

@Composable
fun CurrentReadinessBoxItem(modifier: Modifier = Modifier, boxWidth: Float, boxColor: Color) {
    Box(
        modifier = modifier
            .height(20.dp)
            .fillMaxWidth(boxWidth)
            .background(boxColor)
    )
}
