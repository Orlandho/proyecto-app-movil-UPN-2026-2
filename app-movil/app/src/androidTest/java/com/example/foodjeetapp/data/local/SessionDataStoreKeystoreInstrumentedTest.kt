package com.example.foodjeetapp.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Pruebas instrumentadas en dispositivo/emulador Android real para verificar
 * Android Keystore + AES-256-GCM + persistencia en Jetpack DataStore (Corrección 01 - Semana 12).
 */
@RunWith(AndroidJUnit4::class)
class SessionDataStoreKeystoreInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testAlias = "foodjet_test_keystore_alias_v1"

    @After
    fun cleanup() = runBlocking {
        SessionDataStore(context).clearSession()
        AndroidKeystoreAesGcmCipher(testAlias).deleteKeySilently()
    }

    @Test
    fun androidKeystore_cifraYDescifraConIvUnicoYRecuperaTrasReinicio() = runBlocking {
        val cipher = AndroidKeystoreAesGcmCipher(testAlias)
        val jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.keystore_real_sig"

        val c1 = cipher.encrypt(jwt)
        val c2 = cipher.encrypt(jwt)

        assertTrue(c1.startsWith("v1:"))
        assertNotEquals("Android Keystore debe generar IV distinto en cada cifrado", c1, c2)
        assertNotEquals(c1.split(':')[1], c2.split(':')[1])
        assertFalse(c1.contains(jwt))
        assertEquals(jwt, cipher.decrypt(c1))
        assertEquals(jwt, cipher.decrypt(c2))

        // Persistir en SessionDataStore real y verificar archivo en disco
        val store1 = SessionDataStore(context)
        store1.saveSession(
            token = jwt,
            userId = 99,
            name = "Estudiante UPN",
            email = "seguridad.upn@mail.com",
            phone = "987111222",
            isStudent = true,
            isAdmin = false
        )

        val pbFile = File(context.filesDir, "datastore/$SESSION_DATASTORE_NAME.preferences_pb")
        assertTrue("El archivo DataStore debe existir en files/datastore/", pbFile.exists())
        val rawContent = pbFile.readBytes().toString(Charsets.ISO_8859_1)
        assertFalse("El JWT no debe aparecer en texto plano en disco", rawContent.contains(jwt))
        assertFalse("El correo no debe aparecer en texto plano en disco", rawContent.contains("seguridad.upn@mail.com"))
        assertFalse("El telefono no debe aparecer en texto plano en disco", rawContent.contains("987111222"))

        // Nueva instancia simulando reinicio de app (sin token en caché de instancia)
        val store2 = SessionDataStore(context)
        assertEquals(jwt, store2.getToken())
        val profile = store2.userProfileFlow.first()
        assertEquals("Estudiante UPN", profile?.name)
        assertEquals("seguridad.upn@mail.com", profile?.email)
        assertEquals("987111222", profile?.phone)
        assertEquals(true, profile?.isStudent)

        // Logout limpia el token y el perfil
        store2.clearSession()
        assertNull(store2.getToken())
        assertNull(store2.userProfileFlow.first())
    }

    @Test
    fun androidKeystore_claveEliminadaNoCrasheaYCierraSesionDeFormaControlada() = runBlocking {
        val jwt = "eyJhbGciOiJIUzI1NiJ9.clave_invalidada.test"
        val store1 = SessionDataStore(context)
        store1.saveSession(
            token = jwt,
            userId = 10,
            name = "Demo",
            email = "demo@mail.com",
            phone = "900000001",
            isStudent = false
        )

        // Simular invalidación/eliminación de clave en Android Keystore
        AndroidKeystoreAesGcmCipher().deleteKeySilently()

        val store2 = SessionDataStore(context)
        assertNull("Con clave eliminada debe cerrar sesion y devolver null", store2.getToken())
        assertNull(store2.userProfileFlow.first())

        // Tras la limpieza controlada, un nuevo login regenera la clave y funciona
        val newJwt = "eyJhbGciOiJIUzI1NiJ9.nueva_sesion.ok"
        store2.saveSession(
            token = newJwt,
            userId = 10,
            name = "Demo",
            email = "demo@mail.com",
            phone = "900000001",
            isStudent = false
        )
        assertEquals(newJwt, SessionDataStore(context).getToken())
    }
}
