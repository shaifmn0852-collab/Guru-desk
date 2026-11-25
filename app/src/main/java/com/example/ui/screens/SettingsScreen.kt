package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.InstituteProfile
import com.example.data.local.ThemePreference
import com.example.ui.components.InstituteEmblemBadge
import com.example.ui.components.SectionHeader
import com.example.ui.viewmodel.CoachingUiState
import com.example.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    uiState: CoachingUiState,
    onSelectTheme: (ThemePreference) -> Unit,
    onSaveInstituteProfile: (InstituteProfile) -> Unit,
    onExportJson: suspend () -> String,
    onExportCsv: suspend () -> String,
    onImportJson: (String) -> Unit,
    onResetDemoData: () -> Unit,
    onNotify: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var instituteName by remember { mutableStateOf(uiState.instituteProfile.name) }
    var instituteAddress by remember { mutableStateOf(uiState.instituteProfile.address) }
    var instituteLogo by remember { mutableStateOf(uiState.instituteProfile.logoText) }
    var instituteContact by remember { mutableStateOf(uiState.instituteProfile.contactNumber) }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.instituteProfile) {
        instituteName = uiState.instituteProfile.name
        instituteAddress = uiState.instituteProfile.address
        instituteLogo = uiState.instituteProfile.logoText
        instituteContact = uiState.instituteProfile.contactNumber
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = onExportJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        }
                    }
                    onNotify("JSON backup exported successfully")
                } catch (e: Exception) {
                    onNotify("Export failed: ${e.message}")
                }
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val csv = onExportCsv()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(csv.toByteArray(Charsets.UTF_8))
                        }
                    }
                    onNotify("CSV report exported successfully")
                } catch (e: Exception) {
                    onNotify("CSV export failed: ${e.message}")
                }
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }
                    if (!content.isNullOrBlank()) {
                        onImportJson(content)
                    } else {
                        onNotify("Selected file is empty")
                    }
                } catch (e: Exception) {
                    onNotify("Unable to read backup file: ${e.message}")
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Appearance Section
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(
                        title = "Appearance",
                        subtitle = "Switch between Light, Dark, or System theme",
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val options = listOf(
                            Triple(ThemePreference.LIGHT, "Light", Icons.Default.LightMode),
                            Triple(ThemePreference.DARK, "Dark", Icons.Default.DarkMode),
                            Triple(ThemePreference.SYSTEM, "System", Icons.Default.BrightnessAuto),
                        )
                        options.forEachIndexed { index, (pref, label, icon) ->
                            SegmentedButton(
                                selected = uiState.themePreference == pref,
                                onClick = { onSelectTheme(pref) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                icon = {
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.testTag("theme_${pref.name.lowercase()}"),
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }
        }

        // Coaching Institute Profile (shown on Fee Receipts)
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SectionHeader(
                        title = "Coaching Institute Details",
                        subtitle = "Appears on printed & shared fee receipts",
                    )

                    // Live Receipt Header Preview
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            InstituteEmblemBadge(logoText = instituteLogo.ifBlank { "GD" })
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = instituteName.ifBlank { "Coaching Institute" },
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = instituteAddress.ifBlank { "Institute Address" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = instituteContact.ifBlank { "+91 ..." },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = instituteName,
                        onValueChange = { instituteName = it },
                        label = { Text("Coaching Institute Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_institute_name"),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = instituteLogo,
                            onValueChange = { instituteLogo = it.take(3).uppercase() },
                            label = { Text("Logo Text") },
                            placeholder = { Text("VS") },
                            singleLine = true,
                            modifier = Modifier.weight(0.35f).testTag("settings_logo_text"),
                        )
                        OutlinedTextField(
                            value = instituteContact,
                            onValueChange = { instituteContact = it },
                            label = { Text("Contact Number") },
                            singleLine = true,
                            modifier = Modifier.weight(0.65f).testTag("settings_contact"),
                        )
                    }
                    OutlinedTextField(
                        value = instituteAddress,
                        onValueChange = { instituteAddress = it },
                        label = { Text("Institute Address") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("settings_address"),
                    )
                    Button(
                        onClick = {
                            onSaveInstituteProfile(
                                InstituteProfile(
                                    name = instituteName,
                                    address = instituteAddress,
                                    logoText = instituteLogo,
                                    contactNumber = instituteContact,
                                )
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("save_institute_settings_button"),
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Institute Profile")
                    }
                }
            }
        }

        // Data Backup & Restore
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SectionHeader(
                        title = "Backup & Data Export",
                        subtitle = "All data is stored persistently on this device. Export anytime for safety.",
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = {
                                exportJsonLauncher.launch("GuruDesk_Backup_${DateUtils.todayIso()}.json")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_json_button"),
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON")
                        }
                        FilledTonalButton(
                            onClick = {
                                exportCsvLauncher.launch("GuruDesk_Students_Fees_${DateUtils.todayIso()}.csv")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_csv_button"),
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                importJsonLauncher.launch(arrayOf("application/json", "text/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_json_button"),
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Backup")
                        }
                        OutlinedButton(
                            onClick = { showResetDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Demo")
                        }
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Restore Sample Data?") },
            text = {
                Text("This will replace current batches, students, fees, attendance, and tests with the default sample coaching institute dataset.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetDemoData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Restore Sample") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            },
        )
    }
}
