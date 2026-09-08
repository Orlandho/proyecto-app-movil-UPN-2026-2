package com.example.foodjeetapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.foodjeetapp.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "foodjet_session")

/**
 * Gestor de persistencia asíncrona de preferencias y sesión del usuario con Jetpack DataStore.
 * Cumple con REQ-SEM05-INF-01.
 */
class SessionDataStore(private val context: Context) {

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("jwt_token")
        private val KEY_USER_ID = intPreferencesKey("user_id")
        private val KEY_NAME = stringPreferencesKey("user_name")
        private val KEY_EMAIL = stringPreferencesKey("user_email")
        private val KEY_PHONE = stringPreferencesKey("user_phone")
        private val KEY_IS_STUDENT = booleanPreferencesKey("is_student")
        private val KEY_IS_ADMIN = booleanPreferencesKey("is_admin")
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_TOKEN]
    }

    val userProfileFlow: Flow<UserProfile?> = context.dataStore.data.map { preferences ->
        val email = preferences[KEY_EMAIL]
        if (email.isNullOrBlank()) {
            null
        } else {
            UserProfile(
                name = preferences[KEY_NAME] ?: "Usuario FoodJet",
                email = email,
                phone = preferences[KEY_PHONE] ?: "987654321",
                isStudent = preferences[KEY_IS_STUDENT] ?: false,
                isAdmin = preferences[KEY_IS_ADMIN] ?: false
            )
        }
    }

    suspend fun saveSession(
        token: String,
        userId: Int,
        name: String,
        email: String,
        phone: String?,
        isStudent: Boolean,
        isAdmin: Boolean = false
    ) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TOKEN] = token
            preferences[KEY_USER_ID] = userId
            preferences[KEY_NAME] = name
            preferences[KEY_EMAIL] = email
            preferences[KEY_PHONE] = phone ?: "987654321"
            preferences[KEY_IS_STUDENT] = isStudent
            preferences[KEY_IS_ADMIN] = isAdmin
        }
    }

    suspend fun updateStudentStatus(isStudent: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_STUDENT] = isStudent
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun getToken(): String? {
        return tokenFlow.firstOrNull()
    }
}
