package com.audihelp.app.ui.dsplab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import com.audihelp.app.domain.model.CompressorConfig
import com.audihelp.app.domain.model.DspConfig
import com.audihelp.app.domain.model.DspPreset
import com.audihelp.app.domain.model.FreqTransformConfig
import com.audihelp.app.domain.model.HearingProfile
import com.audihelp.app.domain.model.LimiterConfig
import com.audihelp.app.domain.model.ModulationConfig
import com.audihelp.app.ui.common.ErrorSnackbar

@Composable
fun DspLabScreen(viewModel: DspLabViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    ErrorSnackbar(state.error, snackbarHostState)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("DSP Lab", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // ── Profile selector ──────────────────────────────────────────────────
        if (state.profiles.isEmpty()) {
            Text(
                "No profiles yet. Complete a hearing test first.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ProfileDropdown(
                profiles = state.profiles,
                selectedProfileId = state.selectedProfileId,
                onSelect = { viewModel.selectProfile(it.id) },
            )
            Spacer(Modifier.height(8.dp))

            // ── Preset selector + add button ──────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PresetDropdown(
                    presets = state.presets,
                    selectedPreset = state.selectedPreset,
                    onSelect = { viewModel.selectPreset(it) },
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { viewModel.showNewPresetDialog() },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New preset", tint = MaterialTheme.colorScheme.primary)
                }
                if (state.selectedPreset != null) {
                    IconButton(
                        onClick = { viewModel.deleteSelectedPreset() },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete preset", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // ── Dirty save bar ────────────────────────────────────────────────
            if (state.isDirty) {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Unsaved changes",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (state.selectedPreset != null) {
                                Button(
                                    onClick = { viewModel.updateCurrentPreset() },
                                    modifier = Modifier.weight(1f),
                                    enabled = !state.isLoading,
                                ) {
                                    Text("Update \"${state.selectedPreset!!.name}\"", maxLines = 1)
                                }
                            }
                            OutlinedButton(
                                onClick = { viewModel.showNewPresetDialog() },
                                modifier = Modifier.weight(1f),
                                enabled = !state.isLoading,
                            ) {
                                Text("Save as New", maxLines = 1)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── DSP sliders ───────────────────────────────────────────────────
            if (state.selectedPreset != null || state.selectedProfileId != null) {
                DspConfigPanel(config = state.workingConfig, viewModel = viewModel)
            }
        }

        SnackbarHost(snackbarHostState)
    }

    // ── New preset dialog ─────────────────────────────────────────────────────
    if (state.showNewPresetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNewPresetDialog() },
            title = { Text("New Preset") },
            text = {
                OutlinedTextField(
                    value = state.newPresetName,
                    onValueChange = { viewModel.setNewPresetName(it) },
                    label = { Text("Preset Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g. Loud environment") },
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveAsNewPreset() },
                    enabled = state.newPresetName.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissNewPresetDialog() }) { Text("Cancel") }
            },
        )
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

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.let { it.name + if (it.isActive) "  •  Active" else "" } ?: "Select profile",
            onValueChange = {},
            readOnly = true,
            label = { Text("Profile") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            profiles.forEach { profile ->
                DropdownMenuItem(
                    text = {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(profile.name, style = MaterialTheme.typography.bodyMedium)
                            if (profile.isActive) Text("Active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    onClick = { onSelect(profile); expanded = false },
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
    selectedPreset: DspPreset?,
    onSelect: (DspPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selectedPreset?.name ?: if (presets.isEmpty()) "No presets yet — tap +" else "Select preset",
            onValueChange = {},
            readOnly = true,
            label = { Text("DSP Preset") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        if (presets.isNotEmpty()) {
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                presets.forEach { preset ->
                    DropdownMenuItem(
                        text = {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(preset.name, style = MaterialTheme.typography.bodyMedium)
                                if (preset.isActive) Text("Active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        onClick = { onSelect(preset); expanded = false },
                    )
                }
            }
        }
    }
}

// ── DSP Config Panel ──────────────────────────────────────────────────────────

@Composable
private fun DspConfigPanel(config: DspConfig, viewModel: DspLabViewModel) {
    SectionHeader("Master Gain")
    LabeledSlider("Gain: ${config.gainDb.toInt()} dB", config.gainDb, -40f..12f) { viewModel.updateMasterGain(it) }

    HorizontalDivider(Modifier.padding(vertical = 12.dp))

    SectionHeader("Equalizer (${config.eq.size} bands)")
    config.eq.forEachIndexed { index, band ->
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Band ${index + 1}: ${band.freq.toInt()} Hz (${band.type.name})", style = MaterialTheme.typography.labelLarge)
                LabeledSlider("Gain: ${band.gainDb.toInt()} dB", band.gainDb, -24f..24f) {
                    viewModel.updateEqBand(index, band.copy(gainDb = it))
                }
                LabeledSlider("Q: ${"%.1f".format(band.q)}", band.q, 0.1f..10f) {
                    viewModel.updateEqBand(index, band.copy(q = it))
                }
            }
        }
    }

    HorizontalDivider(Modifier.padding(vertical = 12.dp))

    val comp = config.compressor ?: CompressorConfig(-20f, 4f, 10f, 100f, 0f)
    SectionHeader("Compressor")
    LabeledSlider("Threshold: ${comp.thresholdDbfs.toInt()} dBFS", comp.thresholdDbfs, -60f..0f) { viewModel.updateCompressor(comp.copy(thresholdDbfs = it)) }
    LabeledSlider("Ratio: ${"%.1f".format(comp.ratio)}:1", comp.ratio, 1f..20f) { viewModel.updateCompressor(comp.copy(ratio = it)) }
    LabeledSlider("Attack: ${comp.attackMs.toInt()} ms", comp.attackMs, 1f..200f) { viewModel.updateCompressor(comp.copy(attackMs = it)) }
    LabeledSlider("Release: ${comp.releaseMs.toInt()} ms", comp.releaseMs, 10f..2000f) { viewModel.updateCompressor(comp.copy(releaseMs = it)) }
    LabeledSlider("Makeup: ${comp.makeupGainDb.toInt()} dB", comp.makeupGainDb, 0f..24f) { viewModel.updateCompressor(comp.copy(makeupGainDb = it)) }

    HorizontalDivider(Modifier.padding(vertical = 12.dp))

    val lim = config.limiter ?: LimiterConfig(-3f)
    SectionHeader("Limiter")
    LabeledSlider("Ceiling: ${lim.ceilingDbfs.toInt()} dBFS", lim.ceilingDbfs, -30f..0f) { viewModel.updateLimiter(lim.copy(ceilingDbfs = it)) }

    HorizontalDivider(Modifier.padding(vertical = 12.dp))

    val freq = config.freqTransform ?: FreqTransformConfig(false, 0f)
    SectionHeader("Frequency Shift")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Enabled", style = MaterialTheme.typography.bodyMedium)
        Switch(checked = freq.enabled, onCheckedChange = { viewModel.updateFreqTransform(freq.copy(enabled = it)) })
    }
    if (freq.enabled) {
        LabeledSlider("Shift: ${freq.shiftHz.toInt()} Hz", freq.shiftHz, -500f..500f) { viewModel.updateFreqTransform(freq.copy(shiftHz = it)) }
    }

    HorizontalDivider(Modifier.padding(vertical = 12.dp))

    val mod = config.modulation ?: ModulationConfig(false, 0f, 1f)
    SectionHeader("Amplitude Modulation")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Enabled", style = MaterialTheme.typography.bodyMedium)
        Switch(checked = mod.enabled, onCheckedChange = { viewModel.updateModulation(mod.copy(enabled = it)) })
    }
    if (mod.enabled) {
        LabeledSlider("Depth: ${"%.2f".format(mod.depth)}", mod.depth, 0f..1f) { viewModel.updateModulation(mod.copy(depth = it)) }
        LabeledSlider("Rate: ${"%.1f".format(mod.rateHz)} Hz", mod.rateHz, 0.1f..20f) { viewModel.updateModulation(mod.copy(rateHz = it)) }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Text(label, style = MaterialTheme.typography.bodySmall)
    Slider(value = value, onValueChange = onValueChange, valueRange = range, modifier = Modifier.fillMaxWidth())
}
