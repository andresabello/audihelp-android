package com.audihelp.app.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object HearingTest : Screen("hearing_test")
    object Profile : Screen("profile")
    object DspLab : Screen("dsp_lab")
    object Live : Screen("live")
}
