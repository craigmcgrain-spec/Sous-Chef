package com.souschef.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SettingsStore {
    val keepScreenAwake: Flow<Boolean>
    val customCategories: Flow<List<String>>
    suspend fun setKeepScreenAwake(enabled: Boolean)
    suspend fun addCategory(name: String)
}

private val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
private val CUSTOM_CATEGORIES = stringSetPreferencesKey("custom_categories")

class DataStoreSettingsStore(
    private val dataStore: DataStore<Preferences>,
) : SettingsStore {

    override val keepScreenAwake: Flow<Boolean> = dataStore.data.map {
        it[KEEP_SCREEN_AWAKE] ?: true
    }

    override val customCategories: Flow<List<String>> = dataStore.data.map {
        it[CUSTOM_CATEGORIES].orEmpty().sorted()
    }

    override suspend fun setKeepScreenAwake(enabled: Boolean) {
        dataStore.edit { it[KEEP_SCREEN_AWAKE] = enabled }
    }

    override suspend fun addCategory(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        dataStore.edit { prefs ->
            prefs[CUSTOM_CATEGORIES] = prefs[CUSTOM_CATEGORIES].orEmpty() + clean
        }
    }
}
