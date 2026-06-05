package com.gaitdetector.presenters.main.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitdetector.R
import com.gaitdetector.ui.theme.Inter

@Composable
fun EnergyExpenditureText(modifier: Modifier = Modifier) {
    Text(
        text       = stringResource(R.string.energy_expenditure_text),
        fontSize   = 12.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.W400,
        fontFamily = Inter,
        color      = Color.White,
        modifier   = modifier.padding(vertical = 4.dp),
    )
}
