package app.cclauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cclauncher.helper.AppTagUtils
import kotlinx.coroutines.delay

@Composable
fun AppTagsEditorDialog(
    title: String,
    initialTags: List<String>,
    onSave: (List<String>) -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    var workingTags by remember(initialTags) { mutableStateOf(AppTagUtils.normalizeTags(initialTags)) }
    var inputValue by remember { mutableStateOf("") }
    var editingIndex by remember { mutableIntStateOf(-1) }
    var saveNoticeVisible by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    val normalizedInitial = remember(initialTags) { AppTagUtils.normalizeTags(initialTags) }
    val normalizedWorking = remember(workingTags) { AppTagUtils.normalizeTags(workingTags) }
    val isDirty = normalizedWorking != normalizedInitial
    val canAddOrUpdate = AppTagUtils.normalizeTagInput(inputValue).isNotBlank()

    LaunchedEffect(saveNoticeVisible) {
        if (!saveNoticeVisible) return@LaunchedEffect
        delay(2000)
        saveNoticeVisible = false
    }

    fun commitInput() {
        val cleaned = AppTagUtils.normalizeTagInput(inputValue)
        if (cleaned.isBlank()) return

        workingTags = if (editingIndex in workingTags.indices) {
            workingTags.toMutableList().also { it[editingIndex] = cleaned }
        } else {
            workingTags + cleaned
        }
        inputValue = ""
        editingIndex = -1
    }

    fun saveCurrentTags() {
        val normalized = AppTagUtils.normalizeTags(workingTags)
        workingTags = normalized
        onSave(normalized)
        saveNoticeVisible = true
    }

    fun handleBack() {
        if (isDirty) {
            showDiscardConfirm = true
        } else {
            onBack()
        }
    }

    AlertDialog(
        onDismissRequest = ::handleBack,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(if (editingIndex >= 0) "Edit tag" else "Add tag") },
                    )
                    TextButton(
                        onClick = ::commitInput,
                        enabled = canAddOrUpdate
                    ) {
                        Text(if (editingIndex >= 0) "Update" else "Add")
                    }
                }

                if (normalizedWorking.isEmpty()) {
                    Text(
                        text = "No tags yet",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(normalizedWorking) { index, tag ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = tag,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                                IconButton(
                                    onClick = {
                                        editingIndex = index
                                        inputValue = tag
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit tag")
                                }
                                IconButton(
                                    onClick = {
                                        workingTags = normalizedWorking.toMutableList().also { it.removeAt(index) }
                                        if (editingIndex == index) {
                                            editingIndex = -1
                                            inputValue = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete tag")
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (saveNoticeVisible) "tags saved" else "",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "${normalizedWorking.size}/${AppTagUtils.MAX_TAGS_PER_APP}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = ::handleBack) { Text("Back") }
                TextButton(onClick = {
                    if (canAddOrUpdate) {
                        commitInput()
                    }
                    saveCurrentTags()
                }) {
                    Text("Save")
                }
                TextButton(onClick = {
                    if (canAddOrUpdate) {
                        commitInput()
                    }
                    saveCurrentTags()
                    onDone()
                }) {
                    Text("Done")
                }
            }
        }
    )

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard tag changes?") },
            text = { Text("Unsaved tag edits will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirm = false
                    onBack()
                }) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }
}
