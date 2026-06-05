package com.gaitdetector.presenters.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitdetector.R
import com.gaitdetector.ui.theme.Inter
import com.gaitdetector.ui.theme.activeColor
import com.gaitdetector.ui.theme.sedentaryColor

@Composable
fun EnergyExpenditurePlotColorsAndMeanings(modifier: Modifier = Modifier) {
    val items = listOf(
        Pair(sedentaryColor, stringResource(R.string.sedentary_text)),
        Pair(activeColor,    stringResource(R.string.active_text)),
        Pair(Color.White,    stringResource(R.string.readiness_text)),
    )
    Row(
        modifier              = modifier.fillMaxWidth().padding(horizontal = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        items.forEach { (color, label) ->
            if (color == Color.White) {
                Spacer(modifier = Modifier.weight(1f))
            }
            EnergyExpenditurePlotColorsAndMeaningItem(boxColor = color, colorMeaning = label)
        }
    }
}

@Composable
fun EnergyExpenditurePlotColorsAndMeaningItem(
    modifier: Modifier = Modifier,
    boxColor: Color,
    colorMeaning: String,
) {
    Row(
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier             = modifier,
    ) {
        Box(modifier = Modifier.background(boxColor, CircleShape).size(16.dp))
        Text(
            text       = colorMeaning,
            fontSize   = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.W400,
            fontFamily = Inter,
            color      = Color.White,
        )
    }
}
