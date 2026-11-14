package com.ianchoischool.memento.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ianchoischool.memento.data.NoteEntity
import com.ianchoischool.memento.data.decodeChecklist
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    notes: List<NoteEntity>,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpen: (NoteEntity) -> Unit,
    onCreate: () -> Unit,
    onSettings: () -> Unit,
    listView: Boolean,
    onListView: (Boolean) -> Unit,
    searchMode: Boolean,
    onFabExpanded: (Boolean) -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf("All") }
    val visible = remember(notes, filter, searchMode) {
        when {
            searchMode -> notes.filterNot { it.isArchived }
            filter == "Archived" -> notes.filter { it.isArchived }
            filter == "Pinned" -> notes.filter { it.isPinned && !it.isArchived }
            filter == "Recent" -> notes.filterNot { it.isArchived }.sortedByDescending { it.updatedAt }
            else -> notes.filterNot { it.isArchived }
        }
    }
    val gridState = rememberLazyStaggeredGridState()
    LaunchedEffect(gridState) {
        var previous = 0
        snapshotFlow { gridState.firstVisibleItemIndex * 10000 + gridState.firstVisibleItemScrollOffset }
            .collect { position ->
                if (position == 0 || position < previous) onFabExpanded(true)
                else if (position > previous) onFabExpanded(false)
                previous = position
            }
    }
    LazyVerticalStaggeredGrid(
        columns = if (listView) StaggeredGridCells.Fixed(1) else StaggeredGridCells.Adaptive(165.dp),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 112.dp),
        verticalItemSpacing = 12.dp,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Column(Modifier.padding(top = 20.dp, bottom = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(42.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Text(
                        "  MEMENTO", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalIconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    if (searchMode) "Find a thought." else greeting(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (searchMode) "Search your notes and checklists." else "What’s on your mind today?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                )
                var active by rememberSaveable { mutableStateOf(false) }
                val focusManager = LocalFocusManager.current
                DockedSearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = query,
                            onQueryChange = onQueryChange,
                            onSearch = { active = false; focusManager.clearFocus() },
                            expanded = false,
                            onExpandedChange = { active = it },
                            placeholder = { Text("Search your thoughts") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                AnimatedVisibility(visible = active || query.isNotEmpty()) {
                                    TextButton(onClick = {
                                        onQueryChange("")
                                        active = false
                                        focusManager.clearFocus()
                                    }) { Text("Cancel") }
                                }
                            },
                        )
                    },
                    expanded = false,
                    onExpandedChange = { active = it },
                    modifier = Modifier.fillMaxWidth(),
                    content = {},
                )
                if (!searchMode) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf("All", "Recent", "Pinned", "Archived").forEach { label ->
                                FilterChip(selected = filter == label, onClick = { filter = label }, label = { Text(label) })
                            }
                        }
                        IconButton(onClick = { onListView(!listView) }) {
                            Icon(
                                if (listView) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = if (listView) "Grid view" else "List view",
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (searchMode) "Search results" else if (filter == "All") "Your notes" else filter,
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text("${visible.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (visible.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                EmptyNotes(
                    title = if (query.isNotBlank()) "No thoughts found." else if (filter == "Archived") "Nothing archived." else if (filter == "Pinned") "No pinned notes yet." else "Your ideas belong here.",
                    description = if (query.isNotBlank()) "Try a different search." else "Start with a thought. Make it yours.",
                    onCreate = onCreate,
                    showAction = query.isBlank() && filter != "Archived" && filter != "Pinned",
                )
            }
        } else {
            items(visible, key = { it.id }) { note ->
                NoteCard(note, onClick = { onOpen(note) }, modifier = Modifier.animateItem())
            }
        }
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning,"
    in 12..17 -> "Good afternoon,"
    else -> "Good evening,"
}

@Composable
fun noteColors(color: Int?): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (color) {
        1 -> scheme.primaryContainer to scheme.onPrimaryContainer
        2 -> scheme.secondaryContainer to scheme.onSecondaryContainer
        3 -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        4 -> scheme.surfaceContainerHighest to scheme.onSurface
        else -> scheme.surfaceContainerHigh to scheme.onSurface
    }
}

@Composable
fun NoteCard(note: NoteEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (container, content) = noteColors(note.color)
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().animateContentSize(spring()),
        shape = if (note.isPinned) RoundedCornerShape(topStart = 32.dp, topEnd = 12.dp, bottomEnd = 32.dp, bottomStart = 12.dp)
        else RoundedCornerShape(topStart = 24.dp, topEnd = 32.dp, bottomEnd = 24.dp, bottomStart = 32.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    note.title.ifBlank { "Untitled" }, Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (note.isPinned) Icon(Icons.Default.PushPin, "Pinned", Modifier.size(18.dp))
            }
            if (note.isChecklist) {
                val items = remember(note.content) {
                    try { decodeChecklist(note.content) } catch (_: org.json.JSONException) { null }
                }
                if (items == null) {
                    Text("Checklist could not be read", style = MaterialTheme.typography.bodyMedium)
                } else {
                    val checked = items.count { it.checked }
                    Text("$checked / ${items.size} completed", style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(
                        progress = { if (items.isEmpty()) 0f else checked.toFloat() / items.size },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    items.take(2).forEach {
                        Text("${if (it.checked) "✓" else "○"}  ${it.text}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            } else if (note.content.isNotBlank()) {
                Text(note.content, style = MaterialTheme.typography.bodyMedium, maxLines = 6, overflow = TextOverflow.Ellipsis)
            }
            Text(
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(note.updatedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = content.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
fun EmptyNotes(title: String, description: String, onCreate: () -> Unit, showAction: Boolean = true, actionLabel: String = "Create a note") {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(visible = visible, enter = fadeIn() + scaleIn(initialScale = 0.88f, animationSpec = spring())) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(92.dp).background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(32.dp, 44.dp, 20.dp, 44.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(42.dp))
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (showAction) Button(onClick = onCreate) { Text(actionLabel) }
        }
    }
}
