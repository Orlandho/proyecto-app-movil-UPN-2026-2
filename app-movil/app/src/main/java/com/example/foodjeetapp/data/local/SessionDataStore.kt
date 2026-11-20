package com.example.foodjeetapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.foodjeetapp.data.model.UserProfile
import com.example.foodjeetapp.data.remote.ServidorFoodJet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

internal const val SESSION_DATASTORE_NAME = "foodjet_session"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = SESSION_DATASTORE_NAME)

/**
 * Gestor de persistencia asíncrona de preferencias y sesión del usuario con Jetpack DataStore.
 * Cumple con REQ-SEM05-INF-01 y protección de secretos de sesión (Semana 12).
 *
 * Seguridad aplicada:
 * - El JWT se almacena cifrado con AES-256-GCM respaldado por Android Keystore (`jwt_token_enc`).
 * - El correo y el teléfono del usuario (PII de sesión) también se cifran en reposo
 *   (`user_email_enc` y `user_phone_enc`) usando un IV único en cada operación.
 * - Cualquier dato legado en texto plano (`jwt_token`, `user_email`, `user_phone`) se migra
 *   automáticamente a su versión cifrada y se elimina de DataStore.
 * - Ante errores de descifrado, clave invalidada o archivo corrupto se realiza un cierre
 *   controlado de sesión sin provocar cierres inesperados ni filtrar secretos en logs.
 */
class SessionDataStore internal constructor(
    private val store: DataStore<Preferences>,
    private val cipher: SessionCipher,
    private val backgroundScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    autoCollect: Boolean = true
) {
    constructor(context: Context) : this(
        store = context.applicationContext.dataStore,
        cipher = AndroidKeystoreAesGcmCipher(),
        autoCollect = true
    )

    companion object {
        // Claves cifradas actuales (AES-256-GCM / Android Keystore)
        internal val KEY_TOKEN_ENC = stringPreferencesKey("jwt_token_enc")
        internal val KEY_EMAIL_ENC = stringPreferencesKey("user_email_enc")
        internal val KEY_PHONE_ENC = stringPreferencesKey("user_phone_enc")

        // Claves legadas en texto plano: solo se leen para migración única y siempre se eliminan.
        internal val KEY_TOKEN_LEGACY = stringPreferencesKey("jwt_token")
        internal val KEY_EMAIL_LEGACY = stringPreferencesKey("user_email")
        internal val KEY_PHONE_LEGACY = stringPreferencesKey("user_phone")

        private val KEY_USER_ID = intPreferencesKey("user_id")
        private val KEY_NAME = stringPreferencesKey("user_name")
        private val KEY_IS_STUDENT = booleanPreferencesKey("is_student")
        private val KEY_IS_ADMIN = booleanPreferencesKey("is_admin")
        private val KEY_SERVIDOR = stringPreferencesKey("servidor")

        private val CLAVES_SESION = listOf(
            KEY_TOKEN_ENC,
            KEY_EMAIL_ENC,
            KEY_PHONE_ENC,
            KEY_TOKEN_LEGACY,
            KEY_EMAIL_LEGACY,
            KEY_PHONE_LEGACY,
            KEY_USER_ID,
            KEY_NAME,
            KEY_IS_STUDENT,
            KEY_IS_ADMIN
        )
    }

    @Volatile
    private var cachedToken: String? = null

    private val migrationMutex = Mutex()

    private val safePreferencesFlow: Flow<Preferences> = store.data.catch { error ->
        if (error is IOException || error is Exception) {
            cachedToken = null
            emit(emptyPreferences())
        } else {
            throw error
        }
    }

    init {
        if (autoCollect) {
            backgroundScope.launch {
                try {
                    migrarDatosSensibles()
                    tokenFlow.collect { token ->
                        cachedToken = token
                    }
                } catch (_: Exception) {
                    cachedToken = null
                }
            }
        }
    }

    val tokenFlow: Flow<String?> = safePreferencesFlow.map { preferences ->
        extraerTokenSeguro(preferences)
    }.distinctUntilChanged()

    val userProfileFlow: Flow<UserProfile?> = safePreferencesFlow.map { preferences ->
        // Si no existe un token válido/descifrable, no se expone una sesión autenticada inconsistente.
        val token = extraerTokenSeguro(preferences)
        if (token.isNullOrBlank()) {
            return@map null
        }

        val email = extraerCampoSensible(
            encryptedValue = preferences[KEY_EMAIL_ENC],
            legacyPlainValue = preferences[KEY_EMAIL_LEGACY]
        )
        if (email.isNullOrBlank()) {
            return@map null
        }

        val phone = extraerCampoSensible(
            encryptedValue = preferences[KEY_PHONE_ENC],
            legacyPlainValue = preferences[KEY_PHONE_LEGACY]
        ).orEmpty()

        UserProfile(
            name = preferences[KEY_NAME] ?: "Usuario FoodJet",
            email = email,
            phone = phone,
            isStudent = preferences[KEY_IS_STUDENT] ?: false,
            isAdmin = preferences[KEY_IS_ADMIN] ?: false
        )
    }.distinctUntilChanged()

    /** Modo de acceso al backend local (Mi cuenta → Conexión local). */
    val servidorFlow: Flow<ServidorFoodJet> = safePreferencesFlow.map { preferences ->
        ServidorFoodJet.desdeNombre(preferences[KEY_SERVIDOR])
    }.distinctUntilChanged()

    suspend fun saveSession(
        token: String,
        userId: Int,
        name: String,
        email: String,
        phone: String?,
        isStudent: Boolean,
        isAdmin: Boolean = false
    ) {
        val encryptedToken = cipher.encrypt(token)
        val encryptedEmail = cipher.encrypt(email)
        val encryptedPhone = phone.orEmpty().takeIf { it.isNotEmpty() }?.let { cipher.encrypt(it) }

        cachedToken = token
        store.edit { preferences ->
            preferences[KEY_TOKEN_ENC] = encryptedToken
            preferences[KEY_EMAIL_ENC] = encryptedEmail
            if (encryptedPhone != null) {
                preferences[KEY_PHONE_ENC] = encryptedPhone
            } else {
                preferences.remove(KEY_PHONE_ENC)
            }
            // Eliminar siempre cualquier copia legada en texto plano.
            preferences.remove(KEY_TOKEN_LEGACY)
            preferences.remove(KEY_EMAIL_LEGACY)
            preferences.remove(KEY_PHONE_LEGACY)

            preferences[KEY_USER_ID] = userId
            preferences[KEY_NAME] = name
            preferences[KEY_IS_STUDENT] = isStudent
            preferences[KEY_IS_ADMIN] = isAdmin
        }
    }

    suspend fun updateStudentStatus(isStudent: Boolean) {
        store.edit { preferences ->
            preferences[KEY_IS_STUDENT] = isStudent
        }
    }

    /** Cierra la sesión eliminando credenciales cifradas y legadas. El servidor elegido se conserva. */
    suspend fun clearSession() {
        cachedToken = null
        try {
            store.edit { preferences ->
                CLAVES_SESION.forEach { preferences.remove(it) }
            }
        } catch (_: Exception) {
            // Si el archivo estuviera dañado, evitar propagar un fallo no controlado.
        }
    }

    suspend fun guardarServidor(servidor: ServidorFoodJet) {
        store.edit { preferences ->
            preferences[KEY_SERVIDOR] = servidor.name
        }
    }

    /**
     * Migra cualquier sesión legada en texto plano (`jwt_token`, `user_email`, `user_phone`)
     * a su formato cifrado AES-GCM y borra los campos sin cifrar.
     * Si la migración falla (p. ej. Keystore no disponible), ejecuta un cierre controlado.
     */
    suspend fun migrarDatosSensibles() = migrationMutex.withLock {
        val current = safePreferencesFlow.firstOrNull() ?: return@withLock
        val legacyToken = current[KEY_TOKEN_LEGACY]
        val legacyEmail = current[KEY_EMAIL_LEGACY]
        val legacyPhone = current[KEY_PHONE_LEGACY]

        if (legacyToken == null && legacyEmail == null && legacyPhone == null) {
            return@withLock
        }

        try {
            val newEncryptedToken = if (current[KEY_TOKEN_ENC] == null && !legacyToken.isNullOrBlank()) {
                cipher.encrypt(legacyToken)
            } else null

            val newEncryptedEmail = if (current[KEY_EMAIL_ENC] == null && !legacyEmail.isNullOrBlank()) {
                cipher.encrypt(legacyEmail)
            } else null

            val newEncryptedPhone = if (current[KEY_PHONE_ENC] == null && !legacyPhone.isNullOrBlank()) {
                cipher.encrypt(legacyPhone)
            } else null

            store.edit { preferences ->
                if (newEncryptedToken != null && preferences[KEY_TOKEN_ENC] == null) {
                    preferences[KEY_TOKEN_ENC] = newEncryptedToken
                }
                if (newEncryptedEmail != null && preferences[KEY_EMAIL_ENC] == null) {
                    preferences[KEY_EMAIL_ENC] = newEncryptedEmail
                }
                if (newEncryptedPhone != null && preferences[KEY_PHONE_ENC] == null) {
                    preferences[KEY_PHONE_ENC] = newEncryptedPhone
                }
                preferences.remove(KEY_TOKEN_LEGACY)
                preferences.remove(KEY_EMAIL_LEGACY)
                preferences.remove(KEY_PHONE_LEGACY)
            }
            if (!legacyToken.isNullOrBlank() && cachedToken == null) {
                cachedToken = legacyToken
            }
        } catch (_: Exception) {
            // Si no se puede cifrar de forma segura, se cierra la sesión controlada y se purga el texto plano.
            clearSession()
        }
    }

    /**
     * Borra JWT y caché de identidad ligados a perfiles de otro backend y garantiza
     * la migración de sesiones previas al formato cifrado.
     * Los dos perfiles NODE_LOCAL_* representan el MISMO backend interno.
     */
    suspend fun migrarConfiguracionServidor() {
        store.edit { preferences ->
            when (preferences[KEY_SERVIDOR]) {
                null, "NODE_LOCAL_EMULADOR", "NODE_LOCAL_USB" -> Unit
                "BACKEND_LOCAL_EMULADOR" -> preferences[KEY_SERVIDOR] = "NODE_LOCAL_EMULADOR"
                "BACKEND_LOCAL_USB" -> preferences[KEY_SERVIDOR] = "NODE_LOCAL_USB"
                else -> {
                    CLAVES_SESION.forEach { preferences.remove(it) }
                    cachedToken = null
                    preferences[KEY_SERVIDOR] = ServidorFoodJet.PREDETERMINADO.name
                }
            }
        }
        migrarDatosSensibles()
    }

    fun getCachedToken(): String? = cachedToken

    suspend fun getToken(): String? {
        cachedToken?.let { return it }
        migrarDatosSensibles()
        val preferences = safePreferencesFlow.firstOrNull() ?: return null
        val encrypted = preferences[KEY_TOKEN_ENC] ?: return null
        return try {
            val decrypted = cipher.decrypt(encrypted)
            if (decrypted.isBlank()) {
                clearSession()
                null
            } else {
                cachedToken = decrypted
                decrypted
            }
        } catch (_: Exception) {
            clearSession()
            null
        }
    }

    private fun extraerTokenSeguro(preferences: Preferences): String? {
        val encrypted = preferences[KEY_TOKEN_ENC]
        if (encrypted != null) {
            return try {
                cipher.decrypt(encrypted).takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                programarLimpiezaControlada()
                null
            }
        }
        val legacy = preferences[KEY_TOKEN_LEGACY]
        if (!legacy.isNullOrBlank()) {
            programarMigracion()
            return legacy
        }
        return null
    }

    private fun extraerCampoSensible(encryptedValue: String?, legacyPlainValue: String?): String? {
        if (encryptedValue != null) {
            return try {
                cipher.decrypt(encryptedValue)
            } catch (_: Exception) {
                programarLimpiezaControlada()
                null
            }
        }
        if (legacyPlainValue != null) {
            programarMigracion()
            return legacyPlainValue
        }
        return null
    }

    private fun programarMigracion() {
        backgroundScope.launch {
            migrarDatosSensibles()
        }
    }

    private fun programarLimpiezaControlada() {
        cachedToken = null
        backgroundScope.launch {
            clearSession()
        }
    }
}
