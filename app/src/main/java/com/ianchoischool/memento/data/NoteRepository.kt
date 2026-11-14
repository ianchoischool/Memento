package com.ianchoischool.memento.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val dao: NoteDao) {
    fun observeNotes(query: String): Flow<List<NoteEntity>> = dao.observeNotes(query.trim())

    suspend fun getNote(id: Long): NoteEntity? = dao.getNote(id)

    suspend fun save(note: NoteEntity): Long = dao.save(note.copy(updatedAt = System.currentTimeMillis()))

    suspend fun delete(id: Long) = dao.delete(id)

    fun observeFolders(): Flow<List<FolderEntity>> = dao.observeFolders()

    suspend fun createFolder(name: String): Long = dao.insertFolder(FolderEntity(name = name.trim()))

    suspend fun deleteFolder(id: Long) = dao.deleteFolder(id)
}
