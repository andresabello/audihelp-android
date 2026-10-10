package com.audihelp.app.ui.hearingtest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.audihelp.app.domain.enums.EarSide
import com.audihelp.app.domain.enums.PatternType
import com.audihelp.app.domain.enums.TrialResponse
import com.audihelp.app.domain.model.HearingAcuityScore
import com.audihelp.app.ui.common.AcuityScoreCard
import com.audihelp.app.ui.common.ErrorSnackbar
import com.audihelp.app.ui.common.LoadingScreen

@Composable
fun HearingTestScreen(
    onNavigateToProfile: () -> Unit = {},
    viewModel: HearingTestViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigate to Profile tab once profile is saved
    LaunchedEffect(state.navigateToProfile) {
        if (state.navigateToProfile) {
            viewModel.onNavigatedToProfile()
            onNavigateToProfile()
        }
    }

    ErrorSnackbar(state.error, snackbarHostState)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Hearing Test", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        when {
            state.isLoading -> LoadingScreen()
            state.activeSession != null && !state.isTestComplete -> ActiveTestPanel(state, viewModel)
            state.isTestComplete -> CompletionPanel(state.acuityScore)
            else -> SetupPanel(state, viewModel)
        }

        SnackbarHost(snackbarHostState)
    }

    // Profile name dialog shown after test completes
    if (state.showProfileNameDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Save Profile") },
            text = {
                Column {
                    Text(
                        "Give this hearing profile a name so you can find it later.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.profileName,
                        onValueChange = { viewModel.setProfileName(it) },
                        label = { Text("Profile Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("e.g. John, Left ear") },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveProfile() },
                    enabled = state.profileName.isNotBlank(),
                ) { Text("Save & View") }
            },
            dismissButton = null, // force a name — don't let them skip
        )
    }
}

@Composable
private fun SetupPanel(state: HearingTestUiState, viewModel: HearingTestViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Select Ear to Test", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EarSide.entries.forEach { ear ->
                    FilterChip(
                        selected = state.selectedEar == ear,
                        onClick = { viewModel.selectEar(ear) },
                        label = { Text(ear.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Pattern Type", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PatternType.entries.forEach { pattern ->
                    FilterChip(
                        selected = state.selectedPattern == pattern,
                        onClick = { viewModel.selectPattern(pattern) },
                        label = { Text(pattern.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { viewModel.startTest() }, modifier = Modifier.fillMaxWidth()) {
                Text("Start Test")
            }
        }
    }

    if (state.sessions.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        Text("Previous Sessions (${state.sessions.size})", style = MaterialTheme.typography.titleMedium)
        state.sessions.take(5).forEach { session ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                    Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(session.earTested.name, style = MaterialTheme.typography.bodyMedium)
                    Text(session.status.name, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ActiveTestPanel(state: HearingTestUiState, viewModel: HearingTestViewModel) {
    val freq = TEST_FREQUENCIES[state.currentFrequencyIndex]
    val progress = (state.currentFrequencyIndex + 1).toFloat() / TEST_FREQUENCIES.size

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${state.currentFrequencyIndex + 1} / ${TEST_FREQUENCIES.size}", style = MaterialTheme.typography.bodySmall)
                Text("Ear: ${state.selectedEar.name}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "$freq Hz",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(16.dp))
            Text("Level: ${state.currentLevelDbfs.toInt()} dBFS", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = state.currentLevelDbfs,
                onValueChange = { viewModel.setLevel(it) },
                valueRange = -80f..0f,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.isTonePlaying) {
                    OutlinedButton(onClick = { viewModel.stopTone() }, modifier = Modifier.weight(1f)) { Text("Stop") }
                } else {
                    Button(onClick = { viewModel.playTone() }, modifier = Modifier.weight(1f)) { Text("Play Tone") }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Did you hear it?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TrialResponse.entries.forEach { response ->
                    OutlinedButton(
                        onClick = { viewModel.recordResponse(response) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(response.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletionPanel(score: HearingAcuityScore?) {
    Column {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Test Complete!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Name your profile to save and view your audiogram.", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (score != null) {
            Spacer(Modifier.height(16.dp))
            AcuityScoreCard(score = score)
        }
    }
}
