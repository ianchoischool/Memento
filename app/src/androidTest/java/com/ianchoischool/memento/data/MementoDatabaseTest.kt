package com.ianchoischool.memento.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MementoDatabaseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "memento-migration-test.db"
    private var db: MementoDatabase? = null

    @After
    fun cleanup() {
        db?.close()
        context.deleteDatabase(name)
    }

    @Test
    fun migrationPreservesNotesAndSupportsFoldersAndArchive() = runBlocking {
        context.deleteDatabase(name)
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            old.execSQL(
                "CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isPinned INTEGER NOT NULL, isArchived INTEGER NOT NULL, color INTEGER)",
            )
            old.execSQL("INSERT INTO notes VALUES (42, 'Existing', 'Preserved body', 100, 200, 1, 0, NULL)")
            old.version = 1
        }
        val migrated = Room.databaseBuilder(context, MementoDatabase::class.java, name)
            .addMigrations(MementoDatabase.MIGRATION_1_2).build()
        db = migrated
        val dao = migrated.noteDao()
        val note = requireNotNull(dao.getNote(42))
        assertEquals("Preserved body", note.content)
        assertEquals(100L, note.createdAt)
        assertTrue(note.isPinned)
        assertFalse(note.isChecklist)
        assertNull(note.folderId)

        val folderId = dao.insertFolder(FolderEntity(name = "Work"))
        dao.save(note.copy(folderId = folderId, isArchived = true))
        assertEquals(folderId, dao.getNote(42)?.folderId)
        assertTrue(dao.observeNotes("Existing").first().single().isArchived)
        dao.deleteFolder(folderId)
        assertNull(dao.getNote(42)?.folderId)
        assertTrue(dao.observeFolders().first().isEmpty())
    }

    @Test
    fun checklistRoundTripsAndRemainsSearchable() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, MementoDatabase::class.java).build()
        db = database
        val items = listOf(ChecklistEntry("Buy milk", true), ChecklistEntry("Call home", false))
        val id = database.noteDao().save(
            NoteEntity(title = "Errands", content = encodeChecklist(items), createdAt = 1, updatedAt = 2, isChecklist = true),
        )
        val saved = requireNotNull(database.noteDao().getNote(id))
        assertEquals(items, decodeChecklist(saved.content))
        assertEquals(id, database.noteDao().observeNotes("milk").first().single().id)
    }
}
