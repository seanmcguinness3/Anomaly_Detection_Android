package com.gaitdetector.presenters.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gaitdetector.DetectionViewModel
import com.gaitdetector.GaitSimulator
import com.gaitdetector.ScoreHistoryView
import com.gaitdetector.ui.theme.Inter
import com.gaitdetector.ui.theme.selectedBottomBarColor

/**
 * Debug screen — shows the live gait-anomaly detection feed.
 *
 * The [DetectionViewModel] (shared with the whole app) already has the
 * simulator running, so this screen just observes the state flows and
 * lets the user switch the simulated gait type from a dropdown.
 *
 * The scrolling [ScoreHistoryView] is an Android Canvas view embedded via
 * [AndroidView]; it receives each new (MSE, threshold) pair via a
 * [LaunchedEffect] that collects [DetectionViewModel.scoreEvents].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    viewModel: DetectionViewModel,
    modifier:  Modifier = Modifier,
    onBack:    () -> Unit = {},
) {
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Re-read enrollment state each time this screen resumes (covers the case
    // where the user enrolled and then navigated back).
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshEnrollment()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Keep a reference to the ScoreHistoryView so the LaunchedEffect can feed it.
    val scoreViewRef = remember { mutableStateOf<ScoreHistoryView?>(null) }

    // Collect score events on the composition scope and forward them to the view.
    LaunchedEffect(Unit) {
        viewModel.scoreEvents.collect { (mse, threshold) ->
            scoreViewRef.value?.addScore(mse, threshold)
        }
    }

    // Gait-type dropdown state
    var dropdownExpanded by remember { mutableStateOf(false) }
    val gaitTypes        = GaitSimulator.GaitType.values()

    // Derived display values
    val statusColor = if (uiState.isAnomaly) Color.Red else Color(0xFF4CAF50)
    val statusText  = when {
        uiState.isAnomaly -> "⚠  ANOMALY"
        else              -> "✓  Normal"
    }
    val progressValue = (uiState.latestMse / (uiState.threshold * 3f)).coerceIn(0f, 1f)

    Column(
        modifier            = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Back link ─────────────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) {
                Text(
                    text       = "← Settings",
                    color      = selectedBottomBarColor,
                    fontSize   = 14.sp,
                    fontFamily = Inter,
                )
            }
        }

        // ── Title ─────────────────────────────────────────────────────────────
        Text(
            text       = "Live Detection",
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Inter,
            color      = Color.White,
            textAlign  = TextAlign.Center,
        )

        // ── Enrollment / threshold info ───────────────────────────────────────
        val enrollInfo = if (uiState.enrolled)
            "Enrolled ✓  ·  threshold: ${"%.6f".format(uiState.threshold)}"
        else
            "Not enrolled — threshold is default (1.0)"

        Text(
            text       = enrollInfo,
            fontSize   = 12.sp,
            fontFamily = Inter,
            color      = Color(0xFFAAAAAA),
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth(),
        )

        // ── Gait-type selector ────────────────────────────────────────────────
        Row(
            modifier     = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text       = "Simulate:",
                fontSize   = 13.sp,
                fontFamily = Inter,
                color      = Color(0xFF888888),
            )
            Spacer(modifier = Modifier.width(12.dp))
            ExposedDropdownMenuBox(
                expanded         = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                modifier         = Modifier.weight(1f),
            ) {
                TextField(
                    value             = uiState.gaitType.label,
                    onValueChange     = {},
                    readOnly          = true,
                    trailingIcon      = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    colors            = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFF1A1A1A),
                        focusedContainerColor   = Color(0xFF1A1A1A),
                        unfocusedTextColor      = Color.White,
                        focusedTextColor        = Color.White,
                        unfocusedTrailingIconColor = Color(0xFF888888),
                        focusedTrailingIconColor   = selectedBottomBarColor,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor   = selectedBottomBarColor,
                    ),
                    textStyle         = androidx.compose.ui.text.TextStyle(
                        fontFamily = Inter,
                        fontSize   = 14.sp,
                    ),
                    modifier          = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded         = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier         = Modifier.background(Color(0xFF1A1A1A)),
                ) {
                    gaitTypes.forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text       = type.label,
                                    fontFamily = Inter,
                                    fontSize   = 14.sp,
                                    color      = Color.White,
                                )
                            },
                            onClick = {
                                viewModel.setGaitType(type)
                                dropdownExpanded = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = Color.White,
                            ),
                        )
                    }
                }
            }
        }

        // ── Anomaly status ────────────────────────────────────────────────────
        Text(
            text       = statusText,
            fontSize   = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Inter,
            color      = statusColor,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth(),
        )

        // ── MSE value + threshold ─────────────────────────────────────────────
        Text(
            text       = "Reconstruction MSE: ${"%.6f".format(uiState.latestMse)}",
            fontSize   = 14.sp,
            fontFamily = Inter,
            color      = Color(0xFF555555),
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth(),
        )

        // ── Score progress bar ────────────────────────────────────────────────
        LinearProgressIndicator(
            progress      = { progressValue },
            modifier      = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color         = if (uiState.isAnomaly) Color.Red else selectedBottomBarColor,
            trackColor    = Color(0xFF2A2A2A),
            strokeCap     = StrokeCap.Round,
        )

        // ── History chart label ───────────────────────────────────────────────
        Text(
            text       = "Score history (last 60 windows)",
            fontSize   = 11.sp,
            fontFamily = Inter,
            color      = Color(0xFFAAAAAA),
            modifier   = Modifier.fillMaxWidth(),
        )

        // ── Scrolling score chart ─────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                ScoreHistoryView(ctx).also { view ->
                    scoreViewRef.value = view
                    view.setBackgroundColor(android.graphics.Color.parseColor("#111111"))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        // ── Inject Anomaly button ─────────────────────────────────────────────
        Button(
            onClick  = { viewModel.injectAnomaly() },
            enabled  = !uiState.isInjecting,
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = Color(0xFFB71C1C),   // dark red
                disabledContainerColor = Color(0xFF333333),
            ),
        ) {
            Text(
                text       = if (uiState.isInjecting) "Injecting…" else "Inject Anomaly",
                fontSize   = 15.sp,
                fontWeight = FontWeight.W600,
                fontFamily = Inter,
                color      = Color.White,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}
