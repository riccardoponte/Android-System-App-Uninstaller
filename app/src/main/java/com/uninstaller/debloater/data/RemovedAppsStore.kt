package com.uninstaller.debloater.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.removedAppsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "uninstaller_removed_apps"
)

data class RemovedAppRecord(
    val packageName: String,
    val appName: String
)

class RemovedAppsStore(private val context: Context) {

    private val KEY_REMOVED = stringSetPreferencesKey("removed_system_apps")

    val removedAppsFlow: Flow<Map<String, String>> = context.removedAppsDataStore.data.map { prefs ->
        val rawSet = prefs[KEY_REMOVED] ?: emptySet()
        rawSet.mapNotNull { entry ->
            val parts = entry.split("|", limit = 2)
            if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                val pkg = parts[0]
                val name = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: pkg
                pkg to name
            } else null
        }.toMap()
    }

    suspend fun recordRemovedApp(packageName: String, appName: String) {
        context.removedAppsDataStore.edit { prefs ->
            val current = prefs[KEY_REMOVED] ?: emptySet()
            val cleaned = current.filterNot { it.startsWith("$packageName|") }.toSet()
            prefs[KEY_REMOVED] = cleaned + "$packageName|$appName"
        }
    }

    suspend fun removeRestoredApp(packageName: String) {
        context.removedAppsDataStore.edit { prefs ->
            val current = prefs[KEY_REMOVED] ?: emptySet()
            prefs[KEY_REMOVED] = current.filterNot { it.startsWith("$packageName|") }.toSet()
        }
    }
}
