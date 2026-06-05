package com.gaitdetector

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gaitdetector.navigation.AppNavGraph
import com.gaitdetector.navigation.Screen
import com.gaitdetector.presenters.components.BottomBar
import com.gaitdetector.presenters.components.TopBar
import com.gaitdetector.ui.theme.AppBackgroundColor
import com.gaitdetector.ui.theme.GaitAppTheme

/**
 * Root composable for the app.
 * Hosts the Scaffold (TopBar + BottomBar) and the Compose navigation graph.
 * The bottom bar is hidden on the Debug screen so it doesn't feel like a
 * peer tab — the user gets there via the "Debug Screen" button in Settings.
 */
@Composable
fun GaitApp() {
    GaitAppTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Hide the bottom nav bar on the Debug screen
        val showBottomBar = currentRoute != Screen.Debug.route

        Surface(modifier = Modifier.fillMaxSize(), color = AppBackgroundColor) {
            Scaffold(
                topBar         = { TopBar() },
                bottomBar      = { if (showBottomBar) BottomBar(navHostController = navController) },
                containerColor = AppBackgroundColor,
                modifier       = Modifier.fillMaxSize(),
            ) { paddingValues ->
                AppNavGraph(
                    navHostController = navController,
                    modifier          = Modifier.padding(paddingValues),
                )
            }
        }
    }
}
