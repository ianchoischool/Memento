package com.ianchoischool.memento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ianchoischool.memento.data.AppSettings
import com.ianchoischool.memento.data.ChecklistEntry
import com.ianchoischool.memento.data.NoteEntity
import com.ianchoischool.memento.data.NoteRepository
import com.ianchoischool.memento.data.SettingsRepository
import com.ianchoischool.memento.data.decodeChecklist
import com.ianchoischool.memento.data.encodeChecklist
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class EditorDraft(
    val id: Long? = null,
    val title: String = "",
    val content: String = "",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isChecklist: Boolean = false,
    val folderId: Long? = null,
    val color: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val items: List<ChecklistEntry> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: NoteRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    val notes = _query.flatMapLatest(repository::observeNotes)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val folders = repository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _draft = MutableStateFlow<EditorDraft?>(null)
    val draft: StateFlow<EditorDraft?> = _draft.asStateFlow()
    private val _saveStatus = MutableStateFlow("")
    val saveStatus: StateFlow<String> = _saveStatus.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val saveLock = Mutex()
    private var debounceJob: Job? = null
    private var closing = false

    fun setQuery(value: String) { _query.value = value }

    fun createNote(isChecklist: Boolean = false, folderId: Long? = null) {
        _draft.value = EditorDraft(isChecklist = isChecklist, folderId = folderId)
        _saveStatus.value = ""
    }

    fun editNote(note: NoteEntity) {
        try {
            _draft.value = EditorDraft(
                id = note.id, title = note.title, content = if (note.isChecklist) "" else note.content,
                isPinned = note.isPinned, isArchived = note.isArchived,
                isChecklist = note.isChecklist, folderId = note.folderId, color = note.color,
                createdAt = note.createdAt,
                items = if (note.isChecklist) decodeChecklist(note.content) else emptyList(),
            )
            _saveStatus.value = "Saved"
        } catch (e: org.json.JSONException) {
            _error.value = "Could not open this checklist: ${e.localizedMessage}"
        }
    }

    fun updateDraft(transform: (EditorDraft) -> EditorDraft) {
        val current = _draft.value ?: return
        _draft.value = transform(current)
        scheduleSave()
    }

    private fun scheduleSave() {
        _saveStatus.value = "Saving…"
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(700)
            viewModelScope.launch { saveSafely() }
        }
    }

    private suspend fun saveSafely(): Boolean = saveLock.withLock {
        val current = _draft.value ?: return@withLock true
        if (current.id == null && current.title.isBlank() && current.content.isBlank() &&
            current.items.none { it.text.isNotBlank() }
        ) {
            _saveStatus.value = ""
            return@withLock true
        }
        try {
            val id = repository.save(
                NoteEntity(
                    id = current.id ?: 0,
                    title = current.title,
                    content = if (current.isChecklist) encodeChecklist(current.items) else current.content,
                    createdAt = current.createdAt,
                    updatedAt = System.currentTimeMillis(),
                    isPinned = current.isPinned,
                    isArchived = current.isArchived,
                    color = current.color,
                    isChecklist = current.isChecklist,
                    folderId = current.folderId,
                ),
            )
            if (current.id == null) _draft.value = _draft.value?.copy(id = id)
            _saveStatus.value = if (_draft.value?.copy(id = current.id) == current) "Saved" else "Saving…"
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _saveStatus.value = "Save failed"
            _error.value = "Could not save your note. Your changes remain in the editor: ${e.localizedMessage}"
            false
        }
    }

    fun closeEditor() {
        if (closing) return
        closing = true
        debounceJob?.cancel()
        viewModelScope.launch {
            try {
                do {
                    if (!saveSafely()) return@launch
                } while (_saveStatus.value == "Saving…")
                _draft.value = null
            } finally {
                closing = false
            }
        }
    }

    fun deleteDraft() {
        debounceJob?.cancel()
        viewModelScope.launch {
            saveLock.withLock {
                try {
                    _draft.value?.id?.let { repository.delete(it) }
                    _draft.value = null
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _error.value = "Could not delete the note: ${e.localizedMessage}"
                }
            }
        }
    }

    fun createFolder(name: String) = perform("create folder") {
        if (name.isNotBlank()) repository.createFolder(name)
    }

    fun deleteFolder(id: Long) = perform("delete folder") { repository.deleteFolder(id) }
    fun setTheme(value: String) = perform("change theme") { settingsRepository.setTheme(value) }
    fun setDynamicColor(value: Boolean) = perform("change dynamic color") { settingsRepository.setDynamicColor(value) }
    fun setListView(value: Boolean) = perform("change layout") { settingsRepository.setListView(value) }
    fun dismissError() { _error.value = null }

    private fun perform(action: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Could not $action: ${e.localizedMessage}"
            }
        }
    }
}
