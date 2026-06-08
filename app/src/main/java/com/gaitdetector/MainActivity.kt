package com.gaitdetector

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity

/**
 * Single entry-point Activity for the app.
 *
 * The WEAR DARPA demo UI (Home / Health Stats / Settings) is rendered entirely
 * in Compose via [GaitApp]. From the Settings screen the user can tap
 * "Debug Screen" to navigate — within the same Activity — to [DebugScreen],
 * which hosts the gait-anomaly enrolment and detection controls.
 *
 * [DetectionViewModel] is created here (activity-scoped) so the anomaly-detection
 * pipeline starts running immediately at launch, before the user opens DebugScreen.
 *
 * The enrolment flow uses [EnrollmentScreen] (Screen.Enroll route). The legacy
 * [DetectionActivity] is retained in the codebase but is no longer launched.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GaitApp()
        }
    }

    companion object {
        const val PREFS_NAME    = "gait_prefs"
        const val KEY_ENROLLED  = "enrolled"
        const val KEY_THRESHOLD = "threshold"   // personal MSE threshold (float)
    }
}
