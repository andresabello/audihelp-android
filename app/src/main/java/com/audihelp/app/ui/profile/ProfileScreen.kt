package com.audihelp.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.audihelp.app.domain.enums.TrialResponse
import com.audihelp.app.domain.model.HearingAcuityScore
import com.audihelp.app.domain.model.HearingProfile
import com.audihelp.app.domain.model.HearingThreshold
import com.audihelp.app.ui.common.AcuityScoreCard
import com.audihelp.app.ui.common.ErrorSnackbar
import com.audihelp.app.ui.common.LoadingScreen

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    ErrorSnackbar(state.error, snackbarHostState)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Add Profile")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "Hearing Profiles",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))

            if (state.isLoading && state.profiles.isEmpty()) {
                LoadingScreen()
                return@Column
            }

            if (state.profiles.isEmpty()) {
                Text(
                    "No profiles yet. Complete a hearing test to create one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                // ── Profile dropdown ──────────────────────────────────────────
                ProfileDropdown(
                    profiles = state.profiles,
                    selectedProfile = state.selectedProfile,
                    onSelect = { viewModel.selectProfile(it) },
                )

                // Actions for selected profile
                state.selectedProfile?.let { profile ->
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!profile.isActive) {
                            Button(
                                onClick = { viewModel.activateProfile(profile.id) },
                                modifier = Modifier.weight(1f),
                            ) { Text("Set as Active") }
                        } else {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            ) {
                                Text(
                                    "Active Profile",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.deleteProfile(profile.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete profile",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            // ── Audiogram + DSP for selected profile ──────────────────────────
            state.selectedProfile?.let { profile ->
                Spacer(Modifier.height(16.dp))
                state.acuityScore?.let { score ->
                    AcuityScoreCard(score = score)
                    Spacer(Modifier.height(16.dp))
                }
                AudigramPanel(
                    profile = profile,
                    thresholds = state.thresholds,
                    generatedPresetName = state.generatedPresetName,
                    dspDescription = state.dspDescription,
                    onGenerateDsp = { viewModel.generateDspPreset() },
                    onDismissBanner = { viewModel.dismissGeneratedBanner() },
                )
            }
        }

        if (state.showCreateDialog) {
            CreateProfileDialog(
                name = state.newProfileName,
                onNameChange = viewModel::setNewProfileName,
                onConfirm = { viewModel.createProfile() },
                onDismiss = { viewModel.dismissCreateDialog() },
            )
        }
    }
}

// ── Profile dropdown ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileDropdown(
    profiles: List<HearingProfile>,
    selectedProfile: HearingProfile?,
    onSelect: (HearingProfile) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedProfile?.let {
                it.name + if (it.isActive) "  •  Active" else ""
            } ?: "Select a profile",
            onValueChange = {},
            readOnly = true,
            label = { Text("Profile") },
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
                            Text(
                                profile.name,
                                style = MaterialTheme.typography.bodyMedium,
                            )
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

// ── Audiogram + DSP generation ────────────────────────────────────────────────

@Composable
private fun AudigramPanel(
    profile: HearingProfile,
    thresholds: List<HearingThreshold>,
    generatedPresetName: String?,
    dspDescription: String?,
    onGenerateDsp: () -> Unit,
    onDismissBanner: () -> Unit,
) {
    Text(
        "Audiogram — ${profile.name}",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(4.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("○ Right ear", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        Text("× Left ear",  style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text("— = not heard", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(6.dp))

    if (thresholds.isEmpty()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                "No threshold data for this profile.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    } else {
        Card(modifier = Modifier.fillMaxWidth()) {
            AudigramChart(
                thresholds = thresholds,
                modifier = Modifier.padding(8.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
        ZoneGuide()
        Spacer(Modifier.height(16.dp))

        if (generatedPresetName != null) {
            GeneratedBanner(
                presetName = generatedPresetName,
                description = dspDescription,
                onDismiss = onDismissBanner,
            )
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = onGenerateDsp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Text(
                "  Generate DSP Preset from Audiogram",
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Creates EQ boosts at frequencies where you have hearing loss, " +
                "and enables vibrotactile feedback for frequencies you can only feel.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        ThresholdTable(thresholds)
    }
}

@Composable
private fun ZoneGuide() {
    val zones = listOf(
        "Normal" to MaterialTheme.colorScheme.tertiary,
        "Mild" to MaterialTheme.colorScheme.secondary,
        "Moderate" to MaterialTheme.colorScheme.primary,
        "Severe" to MaterialTheme.colorScheme.error,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        zones.forEach { (label, color) ->
            Text("▪ $label", style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
private fun GeneratedBanner(
    presetName: String,
    description: String?,
    onDismiss: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    "Preset saved: \"$presetName\"",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss) { Text("OK") }
            }
            if (!description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Go to Live tab to activate it.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun ThresholdTable(thresholds: List<HearingThreshold>) {
    Text("Raw thresholds", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(4.dp))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            thresholds.groupBy { it.ear }.forEach { (ear, earThresholds) ->
                Text(ear.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text("Freq", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Level", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Response", Modifier.weight(2f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                earThresholds.sortedBy { it.frequencyHz }.forEach { t ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                        Text("${t.frequencyHz} Hz", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text("${t.levelDbfs.toInt()} dBFS", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text(
                            t.response.label,
                            Modifier.weight(2f),
                            style = MaterialTheme.typography.bodySmall,
                            color = when (t.response) {
                                TrialResponse.HEAR_CLEARLY -> MaterialTheme.colorScheme.tertiary
                                TrialResponse.BARELY_HEAR -> MaterialTheme.colorScheme.primary
                                TrialResponse.FEEL -> MaterialTheme.colorScheme.secondary
                                TrialResponse.NOTHING -> MaterialTheme.colorScheme.error
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── Create profile dialog ─────────────────────────────────────────────────────

@Composable
private fun CreateProfileDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Profile") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Profile Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("e.g. John, Left ear test") },
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
