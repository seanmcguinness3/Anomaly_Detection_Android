package com.gaitdetector.presenters.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.gaitdetector.R
import com.gaitdetector.navigation.BottomNavigationItem
import com.gaitdetector.navigation.Screen
import com.gaitdetector.ui.theme.AppBackgroundColor
import com.gaitdetector.ui.theme.Inter
import com.gaitdetector.ui.theme.selectedBottomBarColor

@Composable
fun BottomBar(
    navHostController: NavHostController,
    modifier: Modifier = Modifier,
) {
    // Derive the selected tab from the actual back-stack destination so that
    // navigating via back button / in-screen links keeps the bar in sync.
    val navBackStackEntry by navHostController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val selectedItemIndex = when (currentRoute) {
        Screen.Main.route       -> 0
        Screen.HealthStat.route -> 1
        Screen.Settings.route   -> 2
        else                    -> 0
    }

    val items = listOf(
        BottomNavigationItem(
            title          = stringResource(R.string.home_title_text),
            selectedIcon   = painterResource(R.drawable.ic_selected_house),
            unselectedIcon = painterResource(R.drawable.ic_unselected_house),
        ),
        BottomNavigationItem(
            title          = stringResource(R.string.health_stats_title_text),
            selectedIcon   = painterResource(R.drawable.ic_selected_health_stat),
            unselectedIcon = painterResource(R.drawable.ic_unselected_health_stat),
        ),
        BottomNavigationItem(
            title          = stringResource(R.string.settings_title_text),
            selectedIcon   = painterResource(R.drawable.ic_selected_hammer),
            unselectedIcon = painterResource(R.drawable.ic_unselected_hammer),
        ),
    )

    NavigationBar(
        containerColor = AppBackgroundColor,
        contentColor   = selectedBottomBarColor,
        modifier       = modifier,
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = selectedItemIndex == index
            NavigationBarItem(
                selected = isSelected,
                onClick  = {
                    val dest = when (index) {
                        0    -> Screen.Main.route
                        1    -> Screen.HealthStat.route
                        2    -> Screen.Settings.route
                        else -> Screen.Main.route
                    }
                    navHostController.navigate(dest) {
                        launchSingleTop = true
                        popUpTo(navHostController.graph.startDestinationId) { saveState = true }
                        restoreState = true
                    }
                },
                label = {
                    Text(
                        text       = item.title,
                        fontSize   = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.W600,
                        color      = if (isSelected) selectedBottomBarColor else Color.White,
                    )
                },
                icon = {
                    Image(
                        painter            = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title,
                        modifier           = Modifier.size(24.dp),
                    )
                },
                enabled = !isSelected,
                colors  = NavigationBarItemDefaults.colors(
                    selectedIconColor   = selectedBottomBarColor,
                    unselectedIconColor = Color.White,
                    selectedTextColor   = selectedBottomBarColor,
                    unselectedTextColor = Color.White,
                    indicatorColor      = Color.Transparent,
                ),
            )
        }
    }
}
