package com.example.foodjeetapp.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.example.foodjeetapp.data.remote.ServidorFoodJet
import com.example.foodjeetapp.data.remote.interceptor.AuthInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Pruebas unitarias JVM para el cifrado autenticado AES-GCM de sesión (Corrección 01 - Semana 12).
 */
class SessionDataStoreSecurityTest {

    private class FakePreferencesDataStore(
        initial: Preferences = emptyPreferences()
    ) : DataStore<Preferences> {
        val state = MutableStateFlow(initial)
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    private class TestAesGcmCipher(
        private var key: SecretKey = generateAes256Key(),
        private val failEncrypt: Boolean = false
    ) : SessionCipher {
        private val random = SecureRandom()

        fun rotateKey() {
            key = generateAes256Key()
        }

        override fun encrypt(plaintext: String): String {
            if (failEncrypt) throw SessionDecryptionException("Fallo simulado de Keystore")
            val iv = ByteArray(AesGcmPayloadCodec.IV_LENGTH_BYTES).also { random.nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(AesGcmPayloadCodec.TAG_LENGTH_BITS, iv))
            val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            return AesGcmPayloadCodec.encode(iv, encrypted)
        }

        override fun decrypt(payload: String): String {
            val (iv, ciphertext) = AesGcmPayloadCodec.decode(payload)
            return try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(AesGcmPayloadCodec.TAG_LENGTH_BITS, iv))
                String(cipher.doFinal(ciphertext), Charsets.UTF_8)
            } catch (e: GeneralSecurityException) {
                throw SessionDecryptionException("Error de descifrado en prueba", e)
            }
        }

        companion object {
            fun generateAes256Key(): SecretKey = KeyGenerator.getInstance("AES").apply {
                init(256)
            }.generateKey()
        }
    }

    private class CapturingChain(private val request: Request) : Interceptor.Chain {
        var sentRequest: Request? = null
        override fun request() = request
        override fun proceed(request: Request): Response {
            sentRequest = request
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody())
                .build()
        }
        override fun connection(): Connection? = null
        override fun call(): Call = throw UnsupportedOperationException()
        override fun connectTimeoutMillis() = 0
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit) = this
        override fun readTimeoutMillis() = 0
        override fun withReadTimeout(timeout: Int, unit: TimeUnit) = this
        override fun writeTimeoutMillis() = 0
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit) = this
    }

    private fun newStore(
        backing: FakePreferencesDataStore,
        cipher: SessionCipher
    ) = SessionDataStore(
        store = backing,
        cipher = cipher,
        backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        autoCollect = false
    )

    @Test
    fun `cifrado AES-GCM genera IV unico en cada operacion y nunca deja JWT en texto plano`() = runBlocking {
        val backing = FakePreferencesDataStore()
        val cipher = TestAesGcmCipher()
        val jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6Mn0.firma_prueba"

        val enc1 = cipher.encrypt(jwt)
        val enc2 = cipher.encrypt(jwt)
        assertNotEquals("Cada cifrado debe usar un IV unico", enc1, enc2)
        assertNotEquals(enc1.split(':')[1], enc2.split(':')[1])
        assertFalse(enc1.contains(jwt))
        assertEquals(jwt, cipher.decrypt(enc1))
        assertEquals(jwt, cipher.decrypt(enc2))

        val session = newStore(backing, cipher)
        session.saveSession(
            token = jwt,
            userId = 2,
            name = "Cliente UPN",
            email = "cliente1@mail.com",
            phone = "987654321",
            isStudent = true,
            isAdmin = false
        )

        val persisted = backing.state.value
        assertNull("La clave legada en texto plano no debe existir", persisted[SessionDataStore.KEY_TOKEN_LEGACY])
        assertNull("El email legado en texto plano no debe existir", persisted[SessionDataStore.KEY_EMAIL_LEGACY])
        assertNull("El telefono legado en texto plano no debe existir", persisted[SessionDataStore.KEY_PHONE_LEGACY])

        val storedEncToken = persisted[SessionDataStore.KEY_TOKEN_ENC]!!
        assertTrue(storedEncToken.startsWith("v1:"))
        assertFalse(storedEncToken.contains(jwt))
        assertFalse(persisted[SessionDataStore.KEY_EMAIL_ENC]!!.contains("cliente1@mail.com"))
        assertFalse(persisted[SessionDataStore.KEY_PHONE_ENC]!!.contains("987654321"))

        // Simular reinicio de app creando nueva instancia sin cache en memoria
        val restarted = newStore(backing, cipher)
        assertEquals(jwt, restarted.getToken())
        val profile = restarted.userProfileFlow.first()
        assertEquals("Cliente UPN", profile?.name)
        assertEquals("cliente1@mail.com", profile?.email)
        assertEquals("987654321", profile?.phone)
        assertEquals(true, profile?.isStudent)
    }

    @Test
    fun `AuthInterceptor inyecta el JWT descifrado en la cabecera Authorization`() = runBlocking {
        val backing = FakePreferencesDataStore()
        val cipher = TestAesGcmCipher()
        val jwt = "header.payload.signature_auth_test"

        newStore(backing, cipher).saveSession(
            token = jwt,
            userId = 5,
            name = "Admin FoodJet",
            email = "admin@foodjet.com",
            phone = null,
            isStudent = false,
            isAdmin = true
        )

        // Nueva instancia (simulando arranque frio)
        val coldSession = newStore(backing, cipher)
        val interceptor = AuthInterceptor(coldSession)
        val chain = CapturingChain(Request.Builder().url("http://10.0.2.2:3000/api/orders/my-orders").build())
        interceptor.intercept(chain)

        assertEquals("Bearer $jwt", chain.sentRequest?.header("Authorization"))
    }

    @Test
    fun `migra sesion legada en texto plano a cifrado y elimina copias sin cifrar`() = runBlocking {
        val backing = FakePreferencesDataStore()
        val cipher = TestAesGcmCipher()
        val legacyJwt = "legacy.plain.jwt_token_123"

        backing.edit { prefs ->
            prefs[SessionDataStore.KEY_TOKEN_LEGACY] = legacyJwt
            prefs[SessionDataStore.KEY_EMAIL_LEGACY] = "legacy@mail.com"
            prefs[SessionDataStore.KEY_PHONE_LEGACY] = "999888777"
        }

        val session = newStore(backing, cipher)
        val recovered = session.getToken()

        assertEquals(legacyJwt, recovered)
        val persisted = backing.state.value
        assertNull("jwt_token legado debe quedar eliminado", persisted[SessionDataStore.KEY_TOKEN_LEGACY])
        assertNull("user_email legado debe quedar eliminado", persisted[SessionDataStore.KEY_EMAIL_LEGACY])
        assertNull("user_phone legado debe quedar eliminado", persisted[SessionDataStore.KEY_PHONE_LEGACY])
        assertTrue(persisted[SessionDataStore.KEY_TOKEN_ENC]!!.startsWith("v1:"))
        assertFalse(persisted[SessionDataStore.KEY_TOKEN_ENC]!!.contains(legacyJwt))

        val profile = session.userProfileFlow.first()
        assertEquals("legacy@mail.com", profile?.email)
        assertEquals("999888777", profile?.phone)
    }

    @Test
    fun `clave invalidada o almacenamiento corrupto cierra sesion de forma controlada sin crashear`() = runBlocking {
        val backing = FakePreferencesDataStore()
        val cipher = TestAesGcmCipher()
        val jwt = "secret.jwt.do_not_leak"

        val session = newStore(backing, cipher)
        session.guardarServidor(ServidorFoodJet.NODE_LOCAL_USB)
        session.saveSession(
            token = jwt,
            userId = 7,
            name = "Usuario",
            email = "u@mail.com",
            phone = "912345678",
            isStudent = false
        )

        // Caso 1: rotacion / invalidacion de clave en Keystore
        cipher.rotateKey()
        val afterKeyInvalidation = newStore(backing, cipher)
        assertNull(afterKeyInvalidation.getToken())
        assertNull(afterKeyInvalidation.userProfileFlow.first())
        assertNull(backing.state.value[SessionDataStore.KEY_TOKEN_ENC])
        assertEquals(
            "El servidor local no debe perderse al limpiar sesion",
            ServidorFoodJet.NODE_LOCAL_USB,
            afterKeyInvalidation.servidorFlow.first()
        )

        // Caso 2: payload corrupto manualmente en DataStore
        backing.edit { prefs ->
            prefs[SessionDataStore.KEY_TOKEN_ENC] = "v1:not-valid-base64:corrupted!!"
        }
        val afterCorruption = newStore(backing, cipher)
        assertNull(afterCorruption.getToken())
        assertNull(backing.state.value[SessionDataStore.KEY_TOKEN_ENC])
    }

    @Test
    fun `fallo al migrar sesion legada realiza cierre controlado y borra el texto plano`() = runBlocking {
        val backing = FakePreferencesDataStore()
        val failingCipher = TestAesGcmCipher(failEncrypt = true)
        backing.edit { prefs ->
            prefs[SessionDataStore.KEY_TOKEN_LEGACY] = "legacy.secret.token"
            prefs[SessionDataStore.KEY_EMAIL_LEGACY] = "a@b.com"
        }

        val session = newStore(backing, failingCipher)
        assertNull(session.getToken())
        assertNull(backing.state.value[SessionDataStore.KEY_TOKEN_LEGACY])
        assertNull(backing.state.value[SessionDataStore.KEY_EMAIL_LEGACY])
    }

    @Test
    fun `errores de descifrado nunca filtran el token ni el payload en su mensaje`() {
        val cipher = TestAesGcmCipher()
        val jwt = "sensitive.jwt.token.value"
        val encrypted = cipher.encrypt(jwt)
        cipher.rotateKey()

        var thrown: Throwable? = null
        try {
            cipher.decrypt(encrypted)
        } catch (e: Throwable) {
            thrown = e
        }

        assertTrue(thrown is SessionDecryptionException)
        val msg = thrown?.message.orEmpty()
        assertFalse(msg.contains(jwt))
        assertFalse(msg.contains(encrypted))
    }

    @Test
    fun `clearSession elimina credenciales y conserva configuracion de servidor`() = runBlocking {
        val backing = FakePreferencesDataStore()
        val cipher = TestAesGcmCipher()
        val session = newStore(backing, cipher)

        session.guardarServidor(ServidorFoodJet.NODE_LOCAL_USB)
        session.saveSession(
            token = "jwt.logout.test",
            userId = 1,
            name = "Test",
            email = "test@mail.com",
            phone = "900111222",
            isStudent = true,
            isAdmin = true
        )

        session.clearSession()

        assertNull(session.getCachedToken())
        assertNull(session.getToken())
        assertNull(session.userProfileFlow.first())
        assertEquals(ServidorFoodJet.NODE_LOCAL_USB, session.servidorFlow.first())
    }
}
