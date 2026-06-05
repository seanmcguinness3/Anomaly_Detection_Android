package com.gaitdetector.presenters.debug

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gaitdetector.DetectionActivity
import com.gaitdetector.MainActivity
import com.gaitdetector.ui.theme.Inter
import com.gaitdetector.ui.theme.selectedBottomBarColor

/**
 * Debug / gait-detector screen.
 *
 * Replicates the original MainActivity XML UI entirely inside Compose so that
 * it lives as a navigation destination within the same Activity rather than
 * requiring a separate XML Activity launch.
 *
 * The enrolment status and MSE threshold are re-read from SharedPreferences
 * every time this screen becomes RESUMED (matches the original onResume logic).
 */
@Composable
fun DebugScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State mirroring the original MainActivity fields
    var statusText    by remember { mutableStateOf("Loading…") }
    var detectEnabled by remember { mutableStateOf(false) }

    // Re-read SharedPreferences each time the screen resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val prefs     = context.getSharedPreferences(MainActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                val enrolled  = prefs.getBoolean(MainActivity.KEY_ENROLLED,  false)
                val threshold = prefs.getFloat(  MainActivity.KEY_THRESHOLD, -1f)
                if (enrolled && threshold > 0f) {
                    statusText    = "Enrolled ✓\nPersonal MSE threshold: ${"%.6f".format(threshold)}"
                    detectEnabled = true
                } else {
                    statusText    = "Not yet enrolled.\nComplete an enrolment session to build your gait template."
                    detectEnabled = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier            = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Back link
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) {
                Text("← Settings", color = selectedBottomBarColor, fontSize = 14.sp, fontFamily = Inter)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text       = "Gait Anomaly Detector",
            fontSize   = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Inter,
            color      = Color.White,
            textAlign  = TextAlign.Center,
        )

        Text(
            text       = statusText,
            fontSize   = 14.sp,
            lineHeight = 20.sp,
            fontFamily = Inter,
            color      = Color.White,
            textAlign  = TextAlign.Center,
            modifier   = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A))
                .padding(16.dp),
        )

        Button(
            onClick  = { context.startActivity(Intent(context, DetectionActivity::class.java)) },
            enabled  = detectEnabled,
            modifier = Modifier.fillMaxWidth(0.75f),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = selectedBottomBarColor,
                disabledContainerColor = Color(0xFF333333),
            ),
        ) {
            Text("Detect", fontSize = 16.sp, fontWeight = FontWeight.W600, fontFamily = Inter, color = Color.White)
        }
    }
}
