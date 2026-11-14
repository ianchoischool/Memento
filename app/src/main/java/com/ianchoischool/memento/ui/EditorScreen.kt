package com.ianchoischool.memento.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ianchoischool.memento.EditorDraft
import com.ianchoischool.memento.data.ChecklistEntry
import com.ianchoischool.memento.data.FolderEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    draft: EditorDraft,
    folders: List<FolderEntity>,
    saveStatus: String,
    onChange: ((EditorDraft) -> EditorDraft) -> Unit,
    onClose: () -> Unit,
    onDelete: () -> Unit,
) {
    var title by remember(draft.createdAt) { mutableStateOf(draft.title) }
    var content by remember(draft.createdAt) { mutableStateOf(draft.content) }
    var checklist by remember(draft.createdAt) { mutableStateOf(draft.items) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var showFolders by rememberSaveable { mutableStateOf(false) }
    var showColors by rememberSaveable { mutableStateOf(false) }
    val (container, contentColor) = noteColors(draft.color)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (draft.isChecklist) "Checklist" else "Note", style = MaterialTheme.typography.titleMedium)
                        Text(saveStatus, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Save and go back") }
                },
                actions = {
                    IconButton(onClick = { onChange { it.copy(isPinned = !it.isPinned) } }) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = if (draft.isPinned) "Unpin note" else "Pin note",
                            tint = if (draft.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onChange { it.copy(isArchived = !it.isArchived) }; onClose() }) {
                        Icon(if (draft.isArchived) Icons.Default.Unarchive else Icons.Default.Archive, contentDescription = if (draft.isArchived) "Unarchive note" else "Archive note")
                    }
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete note")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = 20.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalIconButton(onClick = { showColors = true }) { Icon(Icons.Default.Palette, "Note color") }
                FilledTonalIconButton(onClick = { showFolders = true }) { Icon(Icons.Default.Folder, "Move to folder") }
                Text(
                    folders.find { it.id == draft.folderId }?.name ?: "All notes",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 16.dp),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 16.dp, bottomEnd = 32.dp, bottomStart = 16.dp),
                color = container,
                contentColor = contentColor,
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { value -> title = value; onChange { it.copy(title = value) } },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Give it a title") },
                        textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                        shape = RoundedCornerShape(24.dp),
                    )
                    Spacer(Modifier.size(12.dp))
                    if (draft.isChecklist) {
                        LazyColumn(
                            Modifier.weight(1f).animateContentSize(spring()),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            itemsIndexed(checklist) { index, item ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = item.checked,
                                        onCheckedChange = { checked ->
                                            checklist = checklist.mapIndexed { i, entry -> if (i == index) entry.copy(checked = checked) else entry }
                                            onChange { d -> d.copy(items = checklist) }
                                        },
                                    )
                                    OutlinedTextField(
                                        value = item.text,
                                        onValueChange = { value ->
                                            checklist = checklist.mapIndexed { i, entry -> if (i == index) entry.copy(text = value) else entry }
                                            onChange { d -> d.copy(items = checklist) }
                                        },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("List item") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(20.dp),
                                    )
                                    IconButton(onClick = {
                                        checklist = checklist.filterIndexed { i, _ -> i != index }
                                        onChange { d -> d.copy(items = checklist) }
                                    }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete item ${index + 1}")
                                    }
                                }
                            }
                            item {
                                TextButton(onClick = {
                                    checklist = checklist + ChecklistEntry()
                                    onChange { it.copy(items = checklist) }
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Text("Add item")
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = content,
                            onValueChange = { value -> content = value; onChange { it.copy(content = value) } },
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            placeholder = { Text("Start writing...") },
                            shape = RoundedCornerShape(24.dp),
                        )
                    }
                }
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete this note?") },
            text = { Text("This permanently deletes the note.") },
            confirmButton = { TextButton(onClick = { showDelete = false; onDelete() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }
    if (showFolders) {
        AlertDialog(
            onDismissRequest = { showFolders = false },
            title = { Text("Move to folder") },
            text = {
                LazyColumn {
                    item {
                        FolderChoice("All notes", draft.folderId == null) {
                            onChange { it.copy(folderId = null) }; showFolders = false
                        }
                    }
                    items(folders.size) { index ->
                        val folder = folders[index]
                        FolderChoice(folder.name, draft.folderId == folder.id) {
                            onChange { it.copy(folderId = folder.id) }; showFolders = false
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFolders = false }) { Text("Done") } },
        )
    }
    if (showColors) {
        AlertDialog(
            onDismissRequest = { showColors = false },
            title = { Text("Note color") },
            text = {
                Column {
                    listOf("Default", "Primary", "Secondary", "Tertiary", "Neutral").forEachIndexed { index, name ->
                        val value = if (index == 0) null else index
                        FolderChoice(name, draft.color == value) {
                            onChange { it.copy(color = value) }; showColors = false
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showColors = false }) { Text("Done") } },
        )
    }
}

@Composable
private fun FolderChoice(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        TextButton(onClick = onClick) { Text(name) }
    }
}
