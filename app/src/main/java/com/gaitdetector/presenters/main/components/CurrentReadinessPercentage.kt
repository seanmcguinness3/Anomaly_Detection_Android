package com.gaitdetector.presenters.main.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitdetector.R
import com.gaitdetector.ui.theme.Inter
import com.gaitdetector.ui.theme.percentageColor

var readinessText      = mutableStateOf("Fair")
var readinessArrowLoc  = mutableIntStateOf(-30)

@Composable
fun CurrentReadinessPercentage(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(0.81f)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().offset(x = 0.dp, y = 2.dp)
        ) {
            Text(
                text       = stringResource(R.string.zero_percentage_text),
                color      = percentageColor,
                fontSize   = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.W400,
                textAlign  = TextAlign.Center,
                fontFamily = Inter,
            )
            Text(
                text       = stringResource(R.string.hundred_percentage_text),
                color      = percentageColor,
                fontSize   = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.W400,
                textAlign  = TextAlign.Center,
                fontFamily = Inter,
            )
        }
        Column(
            modifier              = Modifier.fillMaxWidth().offset(x = 0.dp, y = (-4).dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector        = Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.percentage_arrow_content_description_text),
                modifier           = Modifier
                    .size(23.dp)
                    .rotate(-90f)
                    .offset(y = readinessArrowLoc.value.dp),
                tint = percentageColor,
            )
            Text(
                text       = readinessText.value,
                fontSize   = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.W700,
                textAlign  = TextAlign.Center,
                fontFamily = Inter,
                color      = Color.White,
            )
        }
    }
}
