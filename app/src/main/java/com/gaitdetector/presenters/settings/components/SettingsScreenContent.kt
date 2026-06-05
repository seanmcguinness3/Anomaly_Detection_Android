package com.gaitdetector.presenters.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gaitdetector.R
import com.gaitdetector.ui.theme.Inter
import com.gaitdetector.ui.theme.selectedBottomBarColor

@Composable
fun SettingsScreenContent(
    modifier: Modifier = Modifier,
    onEnrollClick: () -> Unit = {},
    onDebugClick: () -> Unit = {},
) {
    // Local toggle state — visual only (no Bluetooth in this app)
    var wristPolarVerity     by rememberSaveable { mutableStateOf(false) }
    var anklePolarVerity     by rememberSaveable { mutableStateOf(false) }
    var headPolarVerity      by rememberSaveable { mutableStateOf(false) }
    var chestGarminHRM       by rememberSaveable { mutableStateOf(false) }
    var secureDataConnection by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier            = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text       = stringResource(R.string.wearable_devices_text),
            fontSize   = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.W700,
            fontFamily = Inter, color = Color.White,
        )

        DeviceToggleRow(
            deviceName     = stringResource(R.string.wrist_polar_verity_text),
            deviceId       = stringResource(R.string.wrist_id_text),
            imageRes       = R.drawable.ic_polar_verity_sensor,
            isToggled      = wristPolarVerity,
            modifier       = Modifier.fillMaxWidth(0.9f).padding(top = 36.dp),
        ) { wristPolarVerity = it }

        DeviceToggleRow(
            deviceName = stringResource(R.string.ankle_polar_verity_text),
            deviceId   = stringResource(R.string.ankle_id_text),
            imageRes   = R.drawable.ic_polar_verity_sensor,
            isToggled  = anklePolarVerity,
            modifier   = Modifier.fillMaxWidth(0.9f),
        ) { anklePolarVerity = it }

        DeviceToggleRow(
            deviceName = stringResource(R.string.head_polar_verity_text),
            deviceId   = stringResource(R.string.head_id_text),
            imageRes   = R.drawable.ic_polar_verity_sensor,
            isToggled  = headPolarVerity,
            modifier   = Modifier.fillMaxWidth(0.9f),
        ) { headPolarVerity = it }

        DeviceToggleRow(
            deviceName = stringResource(R.string.chest_garmin_text),
            deviceId   = stringResource(R.string.chest_id_text),
            imageRes   = R.drawable.ic_chest_germin_hrm,
            isToggled  = chestGarminHRM,
            modifier   = Modifier.fillMaxWidth(0.9f),
        ) { chestGarminHRM = it }

        Text(
            text       = stringResource(R.string.connection_guide_text),
            fontSize   = 16.sp, lineHeight = 22.sp, color = Color.White,
            fontWeight = FontWeight.W400, fontFamily = Inter,
            modifier   = Modifier.padding(top = 28.dp),
        )

        DeviceToggleRow(
            deviceName   = stringResource(R.string.secure_data_connection_text),
            deviceId     = stringResource(R.string.data_connection_id_text),
            imageRes     = R.drawable.ic_secure_data_connection,
            isToggled    = secureDataConnection,
            modifier     = Modifier.padding(top = 75.dp).fillMaxWidth(0.9f).align(Alignment.CenterHorizontally),
            textModifier = Modifier.offset(x = (-6).dp),
        ) { secureDataConnection = it }

        Text(
            text       = stringResource(R.string.secure_policy_text),
            fontSize   = 16.sp, lineHeight = 19.sp, color = Color.White,
            fontWeight = FontWeight.W400, fontFamily = Inter,
            modifier   = Modifier.padding(top = 12.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        // ── Enrol button ─────────────────────────────────────────────────────
        OutlinedButton(
            onClick  = onEnrollClick,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .fillMaxWidth(0.6f),
            border   = BorderStroke(1.5.dp, Color.White),
            colors   = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
        ) {
            Text(
                text       = "Enroll New User",
                fontSize   = 14.sp,
                fontWeight = FontWeight.W600,
                fontFamily = Inter,
                color      = Color.White,
            )
        }

        // ── Debug screen button ───────────────────────────────────────────────
        Button(
            onClick  = onDebugClick,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .fillMaxWidth(0.6f),
            colors   = ButtonDefaults.buttonColors(containerColor = selectedBottomBarColor),
        ) {
            Text(
                text       = "Debug Screen",
                fontSize   = 14.sp,
                fontWeight = FontWeight.W600,
                fontFamily = Inter,
                color      = Color.White,
            )
        }

        Text(
            text       = stringResource(R.string.version_text),
            fontSize   = 13.sp, lineHeight = 15.sp, color = Color.White,
            fontWeight = FontWeight.W500, fontFamily = Inter,
            modifier   = Modifier.padding(bottom = 8.dp),
        )
    }
}
