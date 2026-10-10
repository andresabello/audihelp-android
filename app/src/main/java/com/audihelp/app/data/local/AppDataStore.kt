package com.audihelp.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "audihelp_prefs")

@Singleton
class AppDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val AUTH_TOKEN        = stringPreferencesKey("auth_token")
        val USER_ID           = stringPreferencesKey("user_id")
        val USER_NAME         = stringPreferencesKey("user_name")
        val USER_EMAIL        = stringPreferencesKey("user_email")
        val USER_AVATAR       = stringPreferencesKey("user_avatar")
        val ACTIVE_PRESET_ID  = stringPreferencesKey("active_preset_id")
    }

    fun getToken(): Flow<String?> = context.dataStore.data.map { it[Keys.AUTH_TOKEN] }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[Keys.AUTH_TOKEN] = token }
    }

    suspend fun saveUser(id: String, name: String, email: String, avatar: String?) {
        context.dataStore.edit {
            it[Keys.USER_ID]    = id
            it[Keys.USER_NAME]  = name
            it[Keys.USER_EMAIL] = email
            avatar?.let { av -> it[Keys.USER_AVATAR] = av }
        }
    }

    fun getUserId(): Flow<String?> = context.dataStore.data.map { it[Keys.USER_ID] }

    fun getActivePresetId(): Flow<String?> = context.dataStore.data.map { it[Keys.ACTIVE_PRESET_ID] }

    suspend fun saveActivePresetId(id: String) {
        context.dataStore.edit { it[Keys.ACTIVE_PRESET_ID] = id }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
