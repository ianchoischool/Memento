package com.ianchoischool.memento.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val theme: String = "System",
    val dynamicColor: Boolean = true,
    val listView: Boolean = false,
)

class SettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val dynamicKey = booleanPreferencesKey("dynamic_color")
    private val listKey = booleanPreferencesKey("list_view")

    val settings = context.settingsStore.data.map { preferences ->
        AppSettings(
            theme = preferences[themeKey] ?: "System",
            dynamicColor = preferences[dynamicKey] ?: true,
            listView = preferences[listKey] ?: false,
        )
    }

    suspend fun setTheme(value: String) {
        context.settingsStore.edit { it[themeKey] = value }
    }

    suspend fun setDynamicColor(value: Boolean) {
        context.settingsStore.edit { it[dynamicKey] = value }
    }

    suspend fun setListView(value: Boolean) {
        context.settingsStore.edit { it[listKey] = value }
    }
}
