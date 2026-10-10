package com.audihelp.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.audihelp.app.ui.auth.AuthViewModel
import com.audihelp.app.ui.navigation.AppNavGraph
import com.audihelp.app.ui.theme.AudihelpTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle deep link if app was launched cold via OAuth callback
        intent?.data?.let { uri -> handleDeepLink(uri) }

        setContent {
            AudihelpTheme {
                val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
                AppNavGraph(isLoggedIn = isLoggedIn)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle deep link when app is already running (singleTask)
        intent.data?.let { uri -> handleDeepLink(uri) }
    }

    private fun handleDeepLink(uri: Uri) {
        if (uri.scheme == "audihelp" && uri.host == "auth" && uri.path?.startsWith("/callback") == true) {
            val token = uri.getQueryParameter("token")
            if (!token.isNullOrBlank()) {
                authViewModel.handleOAuthCallback(token)
            }
        }
    }
}
