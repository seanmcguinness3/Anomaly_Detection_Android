package com.gaitdetector

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gaitdetector.navigation.AppNavGraph
import com.gaitdetector.navigation.Screen
import com.gaitdetector.presenters.components.BottomBar
import com.gaitdetector.presenters.components.TopBar
import com.gaitdetector.ui.theme.AppBackgroundColor
import com.gaitdetector.ui.theme.GaitAppTheme
import com.gaitdetector.ui.theme.Inter

/**
 * Root composable for the app.
 *
 * [DetectionViewModel] is obtained here (activity-scoped via [viewModel]) so the
 * simulator starts running as soon as the app's UI is first composed — i.e. at
 * launch — regardless of which screen the user is viewing.
 *
 * The "Anomaly Detected" [AlertDialog] is hosted here so it can appear over
 * any screen. It fires on every rising-edge event emitted by
 * [DetectionViewModel.anomalyAlerts] (normal → anomaly transition only).
 */
@Composable
fun GaitApp(
    detectionViewModel: DetectionViewModel = viewModel(),
) {
    GaitAppTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val showBottomBar = currentRoute != Screen.Debug.route

        // ── Global anomaly alert ──────────────────────────────────────────────
        var showAnomalyDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            detectionViewModel.anomalyAlerts.collect {
                showAnomalyDialog = true
            }
        }

        if (showAnomalyDialog) {
            AlertDialog(
                onDismissRequest = { showAnomalyDialog = false },
                icon = {
                    Icon(
                        imageVector        = Icons.Filled.Warning,
                        contentDescription = "Anomaly",
                        tint               = Color.Red,
                        modifier           = Modifier.size(40.dp),
                    )
                },
                title = {
                    Text(
                        text       = "Anomaly Detected",
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp,
                        color      = Color.White,
                    )
                },
                text = {
                    Text(
                        text       = "An unusual gait pattern was detected. Reconstruction MSE exceeded the personal threshold.",
                        fontFamily = Inter,
                        fontSize   = 14.sp,
                        color      = Color(0xFFCCCCCC),
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showAnomalyDialog = false }) {
                        Text(
                            text       = "OK",
                            fontFamily = Inter,
                            fontWeight = FontWeight.W600,
                            color      = Color.Red,
                        )
                    }
                },
                containerColor = Color(0xFF1A1A1A),
                titleContentColor  = Color.White,
                textContentColor   = Color(0xFFCCCCCC),
            )
        }

        // ── Main scaffold ─────────────────────────────────────────────────────
        Surface(modifier = Modifier.fillMaxSize(), color = AppBackgroundColor) {
            Scaffold(
                topBar         = { TopBar() },
                bottomBar      = { if (showBottomBar) BottomBar(navHostController = navController) },
                containerColor = AppBackgroundColor,
                modifier       = Modifier.fillMaxSize(),
            ) { paddingValues ->
                AppNavGraph(
                    navHostController  = navController,
                    detectionViewModel = detectionViewModel,
                    modifier           = Modifier.padding(paddingValues),
                )
            }
        }
    }
}
