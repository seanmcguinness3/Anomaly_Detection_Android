package com.gaitdetector.presenters.healthstats.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MeasuredMetricRow(
    label: String,
    value: String,
    unit: String = "",
    modifier: Modifier = Modifier,
) {
    Row(
        modifier          = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Normal, color = Color.White, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        Row {
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = unit, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
