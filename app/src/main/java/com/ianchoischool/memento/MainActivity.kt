package com.ianchoischool.memento

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import com.ianchoischool.memento.ui.EditorScreen
import com.ianchoischool.memento.ui.FolderScreen
import com.ianchoischool.memento.ui.HomeScreen
import com.ianchoischool.memento.ui.NewItemSheet
import com.ianchoischool.memento.ui.SettingsScreen
import com.ianchoischool.memento.ui.theme.MementoTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        val app = application as MementoApplication
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MainViewModel(app.repository, app.settingsRepository) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val dark = when (settings.theme) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !dark
            }
            MementoTheme(darkTheme = dark, dynamicColor = settings.dynamicColor) {
                MementoApp(viewModel)
            }
        }
    }
}

private enum class Destination(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Folders("Folders", Icons.Default.Folder),
    Search("Search", Icons.Default.Search),
    Settings("Settings", Icons.Default.Settings),
}

@Composable
private fun MementoApp(viewModel: MainViewModel) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val saveStatus by viewModel.saveStatus.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(Destination.Home) }
    var folderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var showFolderDialog by rememberSaveable { mutableStateOf(false) }
    var fabExpanded by rememberSaveable { mutableStateOf(true) }

    BackHandler(enabled = draft != null || folderId != null || destination != Destination.Home) {
        when {
            draft != null -> viewModel.closeEditor()
            folderId != null -> folderId = null
            else -> { destination = Destination.Home; viewModel.setQuery("") }
        }
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val currentDraft = draft
        if (currentDraft != null) {
            EditorScreen(
                draft = currentDraft,
                folders = folders,
                saveStatus = saveStatus,
                onChange = viewModel::updateDraft,
                onClose = viewModel::closeEditor,
                onDelete = viewModel::deleteDraft,
            )
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        Destination.entries.forEach { item ->
                            NavigationBarItem(
                                selected = destination == item,
                                onClick = {
                                    destination = item
                                    folderId = null
                                    if (item != Destination.Search) viewModel.setQuery("")
                                },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(item.label) },
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (destination != Destination.Settings) {
                        ExtendedFloatingActionButton(
                            expanded = fabExpanded,
                            onClick = { showSheet = true },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("Create") },
                        )
                    }
                },
            ) { padding ->
                when (destination) {
                    Destination.Home, Destination.Search -> HomeScreen(
                        modifier = Modifier.padding(padding),
                        notes = notes,
                        query = query,
                        onQueryChange = viewModel::setQuery,
                        onOpen = viewModel::editNote,
                        onCreate = { showSheet = true },
                        onSettings = { destination = Destination.Settings },
                        listView = settings.listView,
                        onListView = viewModel::setListView,
                        searchMode = destination == Destination.Search,
                        onFabExpanded = { fabExpanded = it },
                    )
                    Destination.Folders -> FolderScreen(
                        modifier = Modifier.padding(padding),
                        folders = folders,
                        notes = notes.filterNot { it.isArchived },
                        selectedId = folderId,
                        onSelect = { folderId = it },
                        onCreate = { showFolderDialog = true },
                        onDelete = viewModel::deleteFolder,
                        onOpen = viewModel::editNote,
                    )
                    Destination.Settings -> SettingsScreen(
                        modifier = Modifier.padding(padding),
                        settings = settings,
                        onTheme = viewModel::setTheme,
                        onDynamic = viewModel::setDynamicColor,
                        onListView = viewModel::setListView,
                    )
                }
            }
        }
    }

    if (showSheet) {
        NewItemSheet(
            onDismiss = { showSheet = false },
            onNote = { showSheet = false; viewModel.createNote(folderId = folderId) },
            onChecklist = { showSheet = false; viewModel.createNote(isChecklist = true, folderId = folderId) },
            onFolder = { showSheet = false; showFolderDialog = true },
        )
    }
    if (showFolderDialog) {
        com.ianchoischool.memento.ui.FolderDialog(
            onDismiss = { showFolderDialog = false },
            onCreate = { viewModel.createFolder(it); showFolderDialog = false },
        )
    }
    if (error != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Something went wrong") },
            text = { Text(error.orEmpty()) },
            confirmButton = { TextButton(onClick = viewModel::dismissError) { Text("OK") } },
        )
    }
}
