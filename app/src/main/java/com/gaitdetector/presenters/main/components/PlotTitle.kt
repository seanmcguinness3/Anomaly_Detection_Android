package com.gaitdetector.presenters.main.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitdetector.ui.theme.Inter

@Composable
fun PlotTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text       = title,
        fontSize   = 16.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.W700,
        textAlign  = TextAlign.Center,
        fontFamily = Inter,
        color      = Color.White,
        modifier   = modifier.padding(top = 12.dp),
    )
}
