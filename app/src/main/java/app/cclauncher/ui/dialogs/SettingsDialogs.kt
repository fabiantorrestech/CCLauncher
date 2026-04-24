package app.cclauncher.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.cclauncher.settings.WidgetImportMode
import app.cclauncher.ui.components.AppSlider
import app.cclauncher.ui.viewmodels.ImportExportState
import io.github.mlmgames.settings.core.backup.ValidationResult
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dialog for slider-based settings
 */
@Composable
fun SliderSettingDialog(
    title: String,
    currentValue: Float,
    min: Float,
    max: Float,
    step: Float,
    onDismiss: () -> Unit,
    onValueSelected: (Float) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            AppSlider(
                value = sliderValue,
                onValueChange = {
                    // Round to nearest step
                    val steps = ((it - min) / step).roundToInt()
                    sliderValue = min + (steps * step)
                },
                valueRange = min..max,
                steps = ((max - min) / step).toInt() - 1,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onValueSelected(sliderValue)
                onDismiss()
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for dropdown/selection settings
 */
@Composable
fun DropdownSettingDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onOptionSelected: (Int) -> Unit
) {
    var selected by remember { mutableIntStateOf(selectedIndex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(options.size) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = index }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == index,
                            onClick = { selected = index }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(options[index])
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onOptionSelected(selected)
                onDismiss()
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ImportExportResultDialog(
    state: ImportExportState,
    onDismiss: () -> Unit
) {
    when (state) {
        is ImportExportState.Loading -> {
            AlertDialog(
                onDismissRequest = { /* Don't dismiss while loading */ },
                title = { Text("Please wait...") },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                },
                confirmButton = { }
            )
        }

        is ImportExportState.ExportSuccess -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Export Successful") },
                text = { Text("Your settings have been exported successfully.") },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            )
        }

        is ImportExportState.ImportSuccess -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Import Successful") },
                text = {
                    Column {
                        Text("Settings imported successfully!")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Applied: ${state.appliedCount} settings",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (state.skippedCount > 0) {
                            Text(
                                "Skipped: ${state.skippedCount} settings (unknown or incompatible)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (state.errors.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Errors:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            state.errors.take(5).forEach { (key, error) ->
                                Text(
                                    "• $key: $error",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            if (state.errors.size > 5) {
                                Text(
                                    "... and ${state.errors.size - 5} more",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            )
        }

        is ImportExportState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Error") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            )
        }

        ImportExportState.Idle -> { /* No dialog */ }
    }
}

@Composable
fun ImportValidationDialog(
    validationResult: ValidationResult,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val exportDate = remember(validationResult.exportedAt) {
        if (validationResult.exportedAt > 0) {
            dateFormat.format(Date(validationResult.exportedAt))
        } else {
            "Unknown"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Settings") },
        text = {
            Column {
                Text(
                    "Backup Details:",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "• Settings: ${validationResult.settingsCount}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "• Schema version: ${validationResult.schemaVersion}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "• Exported: $exportDate",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (validationResult.issues.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Warnings:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    validationResult.issues.forEach { issue ->
                        Text(
                            "• $issue",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Do you want to proceed? This will overwrite your current settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun WidgetImportModeDialog(
    onDismiss: () -> Unit,
    onModeSelected: (WidgetImportMode) -> Unit,
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Widget Import Info") },
            text = {
                Text(
                    "Importing Android widgets from a backup can be unreliable, as sometimes the widgets spawn in a zombie or dead state where they are physically there, but not functioning as they should."
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Back")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text("Import Widgets", modifier = Modifier.align(Alignment.CenterStart))
                IconButton(
                    onClick = { showInfoDialog = true },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Widget import info"
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose how CCLauncher should import widgets from this backup.")
                Button(
                    onClick = { onModeSelected(WidgetImportMode.PLACEHOLDERS) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import with placeholder widgets (Recommended)")
                }
                OutlinedButton(
                    onClick = { onModeSelected(WidgetImportMode.WIDGETS) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import with Widgets")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


// ─── PREVIEWS (debug-only, stripped from release) ────────────────────────────

@Preview(showBackground = true, name = "SliderSettingDialog")
@Composable
private fun PreviewSliderSettingDialog() {
    MaterialTheme {
        SliderSettingDialog(
            title = "Text Size",
            currentValue = 1.0f,
            min = 0.5f,
            max = 2.0f,
            step = 0.1f,
            onDismiss = {},
            onValueSelected = {}
        )
    }
}

@Preview(showBackground = true, name = "DropdownSettingDialog")
@Composable
private fun PreviewDropdownSettingDialog() {
    MaterialTheme {
        DropdownSettingDialog(
            title = "Theme",
            options = listOf("System", "Light", "Dark"),
            selectedIndex = 2,
            onDismiss = {},
            onOptionSelected = {}
        )
    }
}

@Preview(showBackground = true, name = "ImportExportResult - Success")
@Composable
private fun PreviewImportExportResultSuccess() {
    MaterialTheme {
        ImportExportResultDialog(
            state = ImportExportState.ExportSuccess,
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "ImportExportResult - Error")
@Composable
private fun PreviewImportExportResultError() {
    MaterialTheme {
        ImportExportResultDialog(
            state = ImportExportState.Error("Failed to read backup file"),
            onDismiss = {}
        )
    }
}
