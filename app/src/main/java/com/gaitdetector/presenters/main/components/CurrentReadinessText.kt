package com.gaitdetector.presenters.main.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.gaitdetector.R
import com.gaitdetector.ui.theme.Inter

@Composable
fun CurrentReadinessText(modifier: Modifier = Modifier) {
    Text(
        text       = stringResource(R.string.current_readiness_text),
        color      = Color.White,
        fontSize   = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.W700,
        textAlign  = TextAlign.Center,
        modifier   = modifier,
        fontFamily = Inter,
    )
}
