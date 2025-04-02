package com.example.aiagent

import kotlinx.coroutines.flow.firstOrNull
import java.util.NoSuchElementException
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesManager(private val context: Context) {
    companion object {
        val APP_NAME = stringPreferencesKey("app_name")
        val LEARNED_BEHAVIORS = stringSetPreferencesKey("learned_behaviors")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
    }

    suspend fun saveAppName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[APP_NAME] = name
        }
    }

    suspend fun getAppName(): String {
        return context.dataStore.data
            .map { prefs -> prefs[APP_NAME] ?: "AI Agent" }
            .firstOrNull() ?: "AI Agent"
    }

    suspend fun learnBehavior(behavior: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[LEARNED_BEHAVIORS] ?: setOf()
            prefs[LEARNED_BEHAVIORS] = current + behavior
        }
    }

    suspend fun getLearnedBehaviors(): Set<String> {
        return context.dataStore.data
            .map { prefs -> prefs[LEARNED_BEHAVIORS] ?: setOf() }
            .firstOrNull() ?: setOf()
    }

    suspend fun setSelectedModel(modelPath: String) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_MODEL] = modelPath
        }
    }

    suspend fun getSelectedModel(): String? {
        return context.dataStore.data
            .map { prefs -> prefs[SELECTED_MODEL] }
            .firstOrNull()
    }
}
