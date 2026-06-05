package com.gaitdetector.presenters.healthstats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitdetector.R
import com.gaitdetector.ui.theme.textFieldBackgroundColor

@Composable
fun HealthDataInputRow(
    label: String,
    value: String,
    isValueExist: Boolean,
    onValueChange: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 30.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
        BasicTextField(
            value         = value,
            onValueChange = onValueChange,
            modifier      = Modifier
                .fillMaxWidth(0.33f)
                .background(textFieldBackgroundColor, shape = RoundedCornerShape(50.dp))
                .padding(8.dp)
                .weight(1f),
            textStyle     = TextStyle(color = Color.White, textAlign = TextAlign.Start),
            decorationBox = { innerTextField ->
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (value.isEmpty() && !isFocused && isValueExist) {
                        Text(
                            text      = stringResource(R.string.text_field_placeholder_text),
                            style     = TextStyle(color = Color.White),
                            textAlign = TextAlign.Start,
                        )
                    }
                    innerTextField()
                }
            },
            cursorBrush       = SolidColor(Color.White),
            interactionSource = interactionSource,
        )
    }
}
