package app.cclauncher.ui.components

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import app.cclauncher.helper.iconpack.IconPackManager

/**
 * A settings section with a title and card container
 */
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column {
                content()
            }
        }
    }
}

/**
 * A clickable settings item with optional subtitle and description
 */
@Composable
fun SettingsItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    description: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    transparency: Float = 1.0f,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp)
            .alpha(if (enabled) transparency else 0.5f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * A settings item with a toggle switch
 */
@Composable
fun SettingsToggle(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    isChecked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    var toggleState by remember { mutableStateOf(isChecked) }

    LaunchedEffect(isChecked) {
        toggleState = isChecked
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                if (enabled) {
                    toggleState = !toggleState
                    onCheckedChange(toggleState)
                }
            }
            .padding(16.dp)
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Switch(
            checked = toggleState,
            onCheckedChange = { if (enabled) {
                toggleState = it
                onCheckedChange(it)
            }},
            enabled = enabled
        )
    }
}

@Composable
fun SettingsAction(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    buttonText: String = "Set",
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f)
            )

            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.7f else 0.5f)
                )
            }
        }

        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.padding(start = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(buttonText)
        }
    }
}

@Composable
fun GridSizeWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Grid Size Change",
        message = "Changing the grid size may move some apps and widgets to inaccessible positions. Do you want to continue?",
        confirmText = "Continue",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun PageReduceWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Remove Pages",
        message = "Some pages being removed contain apps or widgets. They will be moved to remaining pages if space is available, otherwise they will be removed. Do you want to continue?",
        confirmText = "Continue",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun IconPackSelectionDialog(
    iconPacks: List<IconPackManager.IconPackInfo>,
    selectedPack: String,
    onDismiss: () -> Unit,
    onPackSelected: (String) -> Unit
) {
    var selected by remember { mutableStateOf(selectedPack) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Icon Pack") },
        text = {
            LazyColumn {
                items(iconPacks.size) { index ->
                    val pack = iconPacks[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = pack.packageName }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == pack.packageName,
                            onClick = { selected = pack.packageName }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(pack.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPackSelected(selected) }) {
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
fun FontPickerDialog(
    title: String,
    currentPath: String,
    onDismiss: () -> Unit,
    onSelectClicked: () -> Unit,
    onResetClicked: () -> Unit,
) {
    val fontInfo = remember(currentPath) {
        if (currentPath.isBlank()) null else try {
            val file = java.io.File(currentPath)
            if (file.exists()) Pair(file.name, file.length()) else null
        } catch (_: Exception) {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    "Current font: ${fontInfo?.first ?: "System default"}",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (fontInfo != null) {
                    Text(
                        "Size: ${Formatter.formatFileSize(LocalContext.current, fontInfo.second)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "ABCDEFGabcdefg123",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = onSelectClicked) {
                        Text("Select Font")
                    }

                    if (fontInfo != null) {
                        Button(
                            onClick = onResetClicked,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Reset to Default")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ColorPickerDialog(
    title: String,
    currentColor: Int,
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit
) {
    // Color opts.
    val colorOptions = remember {
        listOf(
            Color.White to "White",
            Color.Black to "Black",
            Color(0xFFF5F5F5) to "Light Gray",
            Color(0xFF9E9E9E) to "Gray",
            Color(0xFF424242) to "Dark Gray",
            Color(0xFFFF5252) to "Red",
            Color(0xFFE91E63) to "Pink",
            Color(0xFF9C27B0) to "Purple",
            Color(0xFF673AB7) to "Deep Purple",
            Color(0xFF3F51B5) to "Indigo",
            Color(0xFF2196F3) to "Blue",
            Color(0xFF03A9F4) to "Light Blue",
            Color(0xFF00BCD4) to "Cyan",
            Color(0xFF009688) to "Teal",
            Color(0xFF4CAF50) to "Green",
            Color(0xFF8BC34A) to "Light Green",
            Color(0xFFCDDC39) to "Lime",
            Color(0xFFFFEB3B) to "Yellow",
            Color(0xFFFFC107) to "Amber",
            Color(0xFFFF9800) to "Orange",
            Color(0xFFFF5722) to "Deep Orange",
            Color(0xFF795548) to "Brown",
        )
    }

    var selectedColor by remember {
        mutableIntStateOf(
            if (currentColor == 0) Color.White.toArgb()
            else currentColor
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Preview of selected color
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(selectedColor)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sample Text",
                            color = Color(selectedColor),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .background(
                                    Color.Gray.copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Color grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(colorOptions) { (color, name) ->
                        ColorOption(
                            color = color,
                            isSelected = selectedColor == color.toArgb(),
                            onClick = { selectedColor = color.toArgb() }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onColorSelected(selectedColor)
                    onDismiss()
                }
            ) {
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
private fun ColorOption(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Gray.copy(alpha = 0.3f)
                },
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// Extension function to calculate luminance
fun Color.luminance(): Float {
    val red = red * 0.299f
    val green = green * 0.587f
    val blue = blue * 0.114f
    return red + green + blue
}

/**
 * App-wide Slider wrapper with a thicker track (8 dp) to match Material You conventions.
 * Replaces the default thin 4 dp track used by Material3's stock Slider.
 *
 * Includes a compact value display with a pencil icon that opens an inline text field for
 * precise numeric entry. The entered value is clamped to [valueRange] on commit.
 *
 * @param valueFormat controls how the current value is shown. Defaults to integer display
 *   when the value is whole, otherwise two decimal places.
 */
@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    valueFormat: (Float) -> String = { v ->
        if (v == kotlin.math.floor(v) && v >= Int.MIN_VALUE && v <= Int.MAX_VALUE)
            v.toInt().toString()
        else "%.2f".format(v)
    },
) {
    var isEditing by remember { mutableStateOf(false) }
    var textValue by remember(value, isEditing) {
        mutableStateOf(
            TextFieldValue(
                text = valueFormat(value),
                selection = TextRange(0, valueFormat(value).length),
            )
        )
    }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    // Tracks whether the text field has received focus at least once.
    // Prevents onFocusChanged(isFocused=false) during initial composition from
    // immediately dismissing the field before LaunchedEffect can request focus.
    var hasHadFocus by remember { mutableStateOf(false) }

    fun commit() {
        val parsed = textValue.text.trim().toFloatOrNull()
        if (parsed != null) {
            onValueChange(parsed.coerceIn(valueRange))
            onValueChangeFinished?.invoke()
        }
        isEditing = false
        hasHadFocus = false
    }

    Column(modifier = modifier) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            track = { state ->
                SliderDefaults.Track(
                    sliderState = state,
                    modifier = Modifier.height(8.dp),
                )
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isEditing) {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    modifier = Modifier
                        .width(120.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { state ->
                            if (state.isFocused) {
                                hasHadFocus = true
                            } else if (hasHadFocus) {
                                commit()
                            }
                        },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        commit()
                    }),
                )
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            } else {
                Text(
                    text = valueFormat(value),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                IconButton(
                    onClick = { isEditing = true },
                    enabled = enabled,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Enter value manually",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

// ─── PREVIEWS (debug-only, stripped from release) ────────────────────────────

@Preview(showBackground = true, name = "SettingsSection")
@Composable
private fun PreviewSettingsSection() {
    MaterialTheme {
        SettingsSection(title = "Appearance") {
            SettingsItem(title = "Theme", subtitle = "Dark", onClick = {})
            SettingsToggle(title = "Bold text", isChecked = true, onCheckedChange = {})
            SettingsAction(title = "Reset defaults", onClick = {})
        }
    }
}

@Preview(showBackground = true, name = "SettingsItem")
@Composable
private fun PreviewSettingsItem() {
    MaterialTheme {
        SettingsItem(
            title = "App font",
            subtitle = "System default",
            description = "Changes the font used across the launcher",
            onClick = {}
        )
    }
}

@Preview(showBackground = true, name = "SettingsToggle - on")
@Composable
private fun PreviewSettingsToggleOn() {
    MaterialTheme {
        SettingsToggle(
            title = "Show clock",
            description = "Displays a clock on the home screen",
            isChecked = true,
            onCheckedChange = {}
        )
    }
}

@Preview(showBackground = true, name = "SettingsToggle - off")
@Composable
private fun PreviewSettingsToggleOff() {
    MaterialTheme {
        SettingsToggle(
            title = "Show clock",
            description = "Displays a clock on the home screen",
            isChecked = false,
            onCheckedChange = {}
        )
    }
}

@Preview(showBackground = true, name = "SettingsAction")
@Composable
private fun PreviewSettingsAction() {
    MaterialTheme {
        SettingsAction(
            title = "Wallpaper",
            description = "Pick a wallpaper from your gallery",
            buttonText = "Set",
            onClick = {}
        )
    }
}
