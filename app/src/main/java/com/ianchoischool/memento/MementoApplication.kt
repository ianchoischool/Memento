package com.ianchoischool.memento

import android.app.Application
import androidx.room.Room
import com.ianchoischool.memento.data.MementoDatabase
import com.ianchoischool.memento.data.NoteRepository
import com.ianchoischool.memento.data.SettingsRepository

class MementoApplication : Application() {
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(applicationContext) }
    val repository: NoteRepository by lazy {
        val database = Room.databaseBuilder(
            applicationContext,
            MementoDatabase::class.java,
            "memento.db",
        ).addMigrations(MementoDatabase.MIGRATION_1_2).build()
        NoteRepository(database.noteDao())
    }
}
