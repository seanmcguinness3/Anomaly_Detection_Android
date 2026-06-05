package com.gaitdetector.presenters.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.gaitdetector.presenters.settings.components.SettingsScreenContent

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onEnrollClick: () -> Unit = {},
    onDebugClick: () -> Unit = {},
) {
    Surface(
        modifier     = modifier.fillMaxSize(),
        color        = Color.Black,
        contentColor = Color.Black,
    ) {
        SettingsScreenContent(
            onEnrollClick = onEnrollClick,
            onDebugClick  = onDebugClick,
        )
    }
}
