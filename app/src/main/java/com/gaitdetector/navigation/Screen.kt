package com.gaitdetector.navigation

/** All Compose navigation destinations. */
sealed class Screen(val route: String) {
    object Main       : Screen("main")
    object HealthStat : Screen("health_stat")
    object Settings   : Screen("settings")
    /** Three-phase gait enrolment screen. */
    object Enroll     : Screen("enroll")
    /** Gait detector debug / test screen. */
    object Debug      : Screen("debug")
}
