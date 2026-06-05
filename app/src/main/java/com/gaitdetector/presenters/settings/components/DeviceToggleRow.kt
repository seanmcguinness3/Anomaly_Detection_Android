package com.gaitdetector.presenters.settings.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitdetector.ui.theme.Inter

@Composable
fun DeviceToggleRow(
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    deviceName: String,
    deviceId: String,
    imageRes: Int,
    isToggled: Boolean,
    onToggleChange: (Boolean) -> Unit,
) {
    Row(
        modifier              = modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Image(painter = painterResource(imageRes), contentDescription = null, modifier = Modifier.size(35.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(
                    text       = deviceName,
                    fontSize   = 16.sp, fontWeight = FontWeight.W700, lineHeight = 20.sp,
                    color      = Color.White, fontFamily = Inter, modifier = textModifier,
                )
                Text(
                    text       = "ID: $deviceId",
                    fontSize   = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.W400,
                    color      = Color.White, fontFamily = Inter,
                )
            }
        }
        Switch(
            checked         = isToggled,
            onCheckedChange = onToggleChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor   = Color.Black,
                uncheckedThumbColor = Color.Gray,
                checkedTrackColor   = Color.White,
                uncheckedTrackColor = Color.Black,
            ),
        )
    }
}
