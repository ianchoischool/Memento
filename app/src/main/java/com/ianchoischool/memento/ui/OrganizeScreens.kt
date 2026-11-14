package com.ianchoischool.memento.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ianchoischool.memento.data.AppSettings
import com.ianchoischool.memento.data.FolderEntity
import com.ianchoischool.memento.data.NoteEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewItemSheet(onDismiss: () -> Unit, onNote: () -> Unit, onChecklist: () -> Unit, onFolder: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Make space for an idea", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("Choose where to begin.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(12.dp))
            SheetAction("New note", "Capture a thought", Icons.Default.EditNote, onNote)
            SheetAction("New checklist", "Keep track of the little things", Icons.Default.Checklist, onChecklist)
            SheetAction("New folder", "Bring related notes together", Icons.Default.CreateNewFolder, onFolder)
        }
    }
}

@Composable
private fun SheetAction(title: String, description: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        ListItem(
            headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text(description) },
            leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}

@Composable
fun FolderDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New folder") },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Folder name") },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onCreate(name.trim()) }, enabled = name.isNotBlank()) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun FolderScreen(
    modifier: Modifier = Modifier,
    folders: List<FolderEntity>,
    notes: List<NoteEntity>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onCreate: () -> Unit,
    onDelete: (Long) -> Unit,
    onOpen: (NoteEntity) -> Unit,
) {
    var deleting by rememberSaveable { mutableStateOf<Long?>(null) }
    val selected = folders.find { it.id == selectedId }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedId != null) {
                    IconButton(onClick = { onSelect(null) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "All folders")
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(selected?.name ?: "Folders", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (selectedId == null) "A place for every thought." else "Notes in this folder",
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (selectedId == null) {
            if (folders.isEmpty()) {
                item { EmptyNotes("Make room for ideas.", "Create a folder to organize your notes.", onCreate, actionLabel = "Create a folder") }
            }
            items(folders, key = { it.id }) { folder ->
                Card(
                    onClick = { onSelect(folder.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(36.dp))
                        Column(Modifier.weight(1f).padding(start = 16.dp)) {
                            Text(folder.name, style = MaterialTheme.typography.titleLarge)
                            Text("${notes.count { it.folderId == folder.id }} notes", style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { deleting = folder.id }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${folder.name}")
                        }
                    }
                }
            }
        } else {
            val folderNotes = notes.filter { it.folderId == selectedId }
            if (folderNotes.isEmpty()) {
                item { EmptyNotes("A fresh start.", "Create a note here or move one into this folder.", {}, showAction = false) }
            }
            items(folderNotes, key = { it.id }) { note -> NoteCard(note, onClick = { onOpen(note) }) }
        }
    }
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete folder?") },
            text = { Text("Notes in this folder will remain in your library.") },
            confirmButton = { TextButton(onClick = { deleting?.let(onDelete); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settings: AppSettings,
    onTheme: (String) -> Unit,
    onDynamic: (Boolean) -> Unit,
    onListView: (Boolean) -> Unit,
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Make it yours.", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
            Text("Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SettingsGroup("Appearance") {
                listOf("System", "Light", "Dark").forEach { theme ->
                    ListItem(
                        headlineContent = { Text(theme) },
                        trailingContent = { RadioButton(selected = settings.theme == theme, onClick = { onTheme(theme) }) },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    )
                }
                ListItem(
                    headlineContent = { Text("Dynamic color") },
                    supportingContent = { Text("Use colors from your wallpaper on Android 12 and newer") },
                    trailingContent = { Switch(checked = settings.dynamicColor, onCheckedChange = onDynamic) },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                )
            }
        }
        item {
            SettingsGroup("Your library") {
                ListItem(
                    headlineContent = { Text("List view") },
                    supportingContent = { Text("Show notes in a single column") },
                    trailingContent = { Switch(checked = settings.listView, onCheckedChange = onListView) },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                )
            }
        }
        item {
            SettingsGroup("About") {
                ListItem(
                    headlineContent = { Text("Memento") },
                    supportingContent = { Text("Think it. Write it. Keep it. · Version 1.0") },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column { content() }
        }
    }
}
