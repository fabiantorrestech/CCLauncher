package app.cclauncher.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import app.cclauncher.data.KeyModifier

@Composable
fun KeyboardShortcutDialog(
    appName: String,
    currentShortcutLabel: String?,
    onAssign: (modifier: Int, keyCode: Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var capturedLabel by remember { mutableStateOf<String?>(null) }
    var capturedModifier by remember { mutableIntStateOf(0) }
    var capturedKeyCode by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Keyboard Shortcut") },
        text = {
            Column {
                Text("App: $appName")
                if (currentShortcutLabel != null) {
                    Text(
                        "Current: $currentShortcutLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(8.dp)
                        )
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp)
                        .focusRequester(focusRequester)
                        .focusable()
                        .onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                            val mod = when {
                                event.isCtrlPressed -> KeyModifier.CTRL
                                event.isAltPressed -> KeyModifier.ALT
                                else -> 0
                            }
                            val k = event.key
                            val isModifierKey = k == Key.CtrlLeft || k == Key.CtrlRight ||
                                k == Key.AltLeft || k == Key.AltRight ||
                                k == Key.ShiftLeft || k == Key.ShiftRight ||
                                k == Key.MetaLeft || k == Key.MetaRight
                            if (mod != 0 && !isModifierKey) {
                                capturedModifier = mod
                                capturedKeyCode = k.nativeKeyCode
                                val modName = if (mod == KeyModifier.CTRL) "Ctrl" else "Alt"
                                capturedLabel = "$modName+${nativeKeyCodeToLabel(k.nativeKeyCode)}"
                                true
                            } else {
                                false
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = capturedLabel ?: "Press Ctrl+key or Alt+key…",
                        color = if (capturedLabel != null)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = capturedLabel != null,
                onClick = {
                    onAssign(capturedModifier, capturedKeyCode)
                    onDismiss()
                },
            ) { Text("Assign") }
        },
        dismissButton = {
            Row {
                if (currentShortcutLabel != null) {
                    TextButton(onClick = { onClear(); onDismiss() }) { Text("Clear") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

private fun nativeKeyCodeToLabel(nativeKeyCode: Int): String =
    android.view.KeyEvent.keyCodeToString(nativeKeyCode)
        .removePrefix("KEYCODE_")
        .replace('_', ' ')
        .lowercase()
        .replaceFirstChar { it.uppercase() }
