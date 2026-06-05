package com.gaitdetector.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gaitdetector.presenters.debug.DebugScreen
import com.gaitdetector.presenters.enrollment.EnrollmentScreen
import com.gaitdetector.presenters.healthstats.HealthStatScreen
import com.gaitdetector.presenters.main.MainScreen
import com.gaitdetector.presenters.settings.SettingsScreen

@Composable
fun AppNavGraph(
    navHostController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(navController = navHostController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainScreen(modifier = modifier)
        }
        composable(Screen.HealthStat.route) {
            HealthStatScreen(modifier = modifier)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                modifier      = modifier,
                onEnrollClick = { navHostController.navigate(Screen.Enroll.route) },
                onDebugClick  = { navHostController.navigate(Screen.Debug.route) },
            )
        }
        composable(Screen.Enroll.route) {
            EnrollmentScreen(
                modifier = modifier,
                onDone   = {
                    navHostController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Settings.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Screen.Debug.route) {
            DebugScreen(
                modifier  = modifier,
                onBack    = { navHostController.popBackStack() }
            )
        }
    }
}
