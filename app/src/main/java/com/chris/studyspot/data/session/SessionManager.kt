package com.chris.studyspot.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.preferences by preferencesDataStore(name = "studyspot_session")

class SessionManager(private val context: Context) {
    val userId: Flow<Long?> = context.preferences.data.map { it[longPreferencesKey("user_id")] }
    suspend fun setUser(id: Long) { context.preferences.edit { it[longPreferencesKey("user_id")] = id } }
    suspend fun clear() { context.preferences.edit { it.remove(longPreferencesKey("user_id")) } }
}

