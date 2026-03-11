package com.safetry.privacy.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "privacy_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val BLUR_FACES = booleanPreferencesKey("blur_faces")
        val AUTO_REMOVE_METADATA = booleanPreferencesKey("auto_remove_metadata")
        val BACKGROUND_SERVICE_ENABLED = booleanPreferencesKey("background_service_enabled")
    }

    fun getBlurFaces(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[BLUR_FACES] ?: false
        }
    }

    suspend fun setBlurFaces(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BLUR_FACES] = enabled
        }
    }

    fun getAutoRemoveMetadata(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[AUTO_REMOVE_METADATA] ?: true
        }
    }

    suspend fun setAutoRemoveMetadata(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_REMOVE_METADATA] = enabled
        }
    }

    fun getBackgroundServiceEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[BACKGROUND_SERVICE_ENABLED] ?: false
        }
    }

    suspend fun setBackgroundServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BACKGROUND_SERVICE_ENABLED] = enabled
        }
    }
}
