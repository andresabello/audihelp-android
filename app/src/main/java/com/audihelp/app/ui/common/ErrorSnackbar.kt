package com.audihelp.app.ui.common

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun ErrorSnackbar(
    message: String?,
    snackbarHostState: SnackbarHostState,
) {
    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
        }
    }
}
