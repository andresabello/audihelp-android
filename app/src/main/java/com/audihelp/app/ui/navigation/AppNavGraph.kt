package com.audihelp.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.audihelp.app.BuildConfig
import com.audihelp.app.ui.auth.LoginScreen
import com.audihelp.app.ui.dsplab.DspLabScreen
import com.audihelp.app.ui.hearingtest.HearingTestScreen
import com.audihelp.app.ui.live.LiveScreen
import com.audihelp.app.ui.profile.ProfileScreen

private data class TopLevelRoute(
    val screen: Screen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val topLevelRoutes = listOf(
    TopLevelRoute(Screen.HearingTest, "Test", Icons.Default.Hearing),
    TopLevelRoute(Screen.Profile, "Profile", Icons.Default.Person),
    TopLevelRoute(Screen.DspLab, "DSP Lab", Icons.Default.Equalizer),
    TopLevelRoute(Screen.Live, "Live", Icons.Default.GraphicEq),
)

@Composable
fun AppNavGraph(isLoggedIn: Boolean) {
    val navController = rememberNavController()

    // LOGIN_ENABLED=false in build.gradle → skip login screen entirely
    val startDestination = if (BuildConfig.LOGIN_ENABLED && !isLoggedIn) {
        Screen.Login.route
    } else {
        Screen.HearingTest.route
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = topLevelRoutes.any { it.screen.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelRoutes.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(navController, startDestination = startDestination, modifier = Modifier.padding(innerPadding)) {
            composable(Screen.Login.route) {
                LoginScreen(onLoginSuccess = {
                    navController.navigate(Screen.HearingTest.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.HearingTest.route) {
                HearingTestScreen(onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
            composable(Screen.Profile.route) { ProfileScreen() }
            composable(Screen.DspLab.route) { DspLabScreen() }
            composable(Screen.Live.route) { LiveScreen() }
        }
    }
}
