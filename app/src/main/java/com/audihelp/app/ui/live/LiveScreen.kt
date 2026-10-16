package com.audihelp.app.ui.live

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.audihelp.app.domain.model.DspPreset
import com.audihelp.app.domain.model.HearingProfile
import com.audihelp.app.ui.common.ErrorSnackbar
import kotlin.math.pow

@Composable
fun LiveScreen(viewModel: LiveViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    ErrorSnackbar(state.error, snackbarHostState)

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startLiveSession()
        else viewModel.dismissError()
    }

    // On Android 13+ we need POST_NOTIFICATIONS so the foreground service notification shows
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, proceed — notification is non-critical */ }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val hasNotifPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasNotifPermission) {
            androidx.compose.runtime.LaunchedEffect(Unit) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    DisposableEffect(Unit) {
        viewModel.onResume()
        onDispose { viewModel.onPause() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Live Mode", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // ── Mic input status ──────────────────────────────────────────────────
        UsbStatusBanner(connected = state.usbMic.connected, deviceName = state.usbMic.deviceName)
        Spacer(Modifier.height(8.dp))

        // ── Output route ──────────────────────────────────────────────────────
        OutputRouteBanner(route = state.outputRoute, onRefresh = { viewModel.refreshOutputRoute() })
        Spacer(Modifier.height(16.dp))

        // ── Profile selector ──────────────────────────────────────────────────
        if (state.profiles.isNotEmpty()) {
            ProfileDropdown(
                profiles = state.profiles,
                selectedProfileId = state.selectedProfileId,
                onSelect = { viewModel.selectProfile(it.id) },
            )
            Spacer(Modifier.height(8.dp))
        }

        // ── Preset selector ───────────────────────────────────────────────────
        if (state.presets.isNotEmpty()) {
            PresetDropdown(
                presets = state.presets,
                selectedPresetId = state.selectedPresetId,
                onSelect = { viewModel.selectPreset(it.id) },
            )
            Spacer(Modifier.height(16.dp))
        }

        // ── Level meters ──────────────────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Audio Levels", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LevelMeter("In", state.inputLevelDbfs)
                Spacer(Modifier.height(8.dp))
                LevelMeter("Out", state.outputLevelDbfs)
            }
        }
        Spacer(Modifier.height(16.dp))

        // ── Start / Stop ──────────────────────────────────────────────────────
        val speakerOnly = state.outputRoute == OutputRoute.INTERNAL_SPEAKER ||
                          state.outputRoute == OutputRoute.UNKNOWN
        val canStart = !state.isLoading && (!speakerOnly || state.isLiveRunning)

        if (speakerOnly && !state.isLiveRunning) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Text(
                    "Plug in headphones before starting — using the speaker causes feedback.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (state.isLiveRunning) {
                    viewModel.stopLiveSession()
                } else {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) viewModel.startLiveSession()
                    else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            enabled = canStart,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isLiveRunning) MaterialTheme.colorScheme.error
                                 else MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(
                if (state.isLiveRunning) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (state.isLiveRunning) "Stop" else "Start",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Recent sessions ───────────────────────────────────────────────────
        if (state.sessions.isNotEmpty()) {
            Text("Recent Sessions (${state.sessions.size})", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            state.sessions.take(5).forEach { session ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(session.startedAt ?: "—", style = MaterialTheme.typography.bodySmall)
                        Text(if (session.endedAt != null) "Ended" else "Active", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        SnackbarHost(snackbarHostState)
    }
}

// ── Profile dropdown ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileDropdown(
    profiles: List<HearingProfile>,
    selectedProfileId: String?,
    onSelect: (HearingProfile) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = profiles.firstOrNull { it.id == selectedProfileId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected?.let { it.name + if (it.isActive) "  •  Active" else "" }
                ?: "Select profile",
            onValueChange = {},
            readOnly = true,
            label = { Text("Who's listening") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            profiles.forEach { profile ->
                DropdownMenuItem(
                    text = {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(profile.name, style = MaterialTheme.typography.bodyMedium)
                            if (profile.isActive) {
                                Text(
                                    "Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelect(profile)
                        expanded = false
                    },
                )
            }
        }
    }
}

// ── Preset dropdown ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetDropdown(
    presets: List<DspPreset>,
    selectedPresetId: String?,
    onSelect: (DspPreset) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = presets.firstOrNull { it.id == selectedPresetId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected?.name ?: "No preset (flat)",
            onValueChange = {},
            readOnly = true,
            label = { Text("DSP Preset") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            presets.forEach { preset ->
                DropdownMenuItem(
                    text = {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(preset.name, style = MaterialTheme.typography.bodyMedium)
                            if (preset.isActive) {
                                Text(
                                    "Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelect(preset)
                        expanded = false
                    },
                )
            }
        }
    }
}

// ── Mic status banner ─────────────────────────────────────────────────────────

@Composable
private fun UsbStatusBanner(connected: Boolean, deviceName: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (connected) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (connected) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = null,
                tint = if (connected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    if (connected) deviceName ?: "USB mic connected" else "Built-in mic",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!connected) {
                    Text(
                        "Plug in DJI Mic Mini via USB-C hub for external mic",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── Output route banner ───────────────────────────────────────────────────────

@Composable
private fun OutputRouteBanner(route: OutputRoute, onRefresh: () -> Unit) {
    val (label, warning) = when (route) {
        OutputRoute.WIRED_HEADSET    -> "Output: Wired (3.5mm / RCA)" to false
        OutputRoute.USB_AUDIO        -> "Output: USB-C audio adapter" to false
        OutputRoute.BLUETOOTH        -> "Output: Bluetooth" to false
        OutputRoute.INTERNAL_SPEAKER -> "Output: Internal speaker — plug in headphones" to true
        OutputRoute.UNKNOWN          -> "Output: Unknown — plug in headphones" to true
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (warning) MaterialTheme.colorScheme.tertiaryContainer
                             else MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            androidx.compose.material3.TextButton(onClick = onRefresh) { Text("Refresh") }
        }
    }
}

// ── Level meter ───────────────────────────────────────────────────────────────

@Composable
private fun LevelMeter(label: String, levelDbfs: Float) {
    val clamped = levelDbfs.coerceIn(-80f, 0f)
    val progress = 10f.pow(clamped / 20f)

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(32.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.weight(1f),
            color = when {
                clamped > -6f  -> MaterialTheme.colorScheme.error
                clamped > -18f -> Color(0xFFFFA000)
                else           -> MaterialTheme.colorScheme.primary
            },
        )
        Spacer(Modifier.width(8.dp))
        Text("${clamped.toInt()} dB", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(52.dp))
    }
}
