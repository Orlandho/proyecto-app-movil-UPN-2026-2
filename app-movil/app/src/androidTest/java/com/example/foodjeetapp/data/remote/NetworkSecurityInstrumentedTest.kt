package com.example.foodjeetapp.data.remote

import android.security.NetworkSecurityPolicy
import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.net.UnknownServiceException
import java.util.concurrent.TimeUnit

/**
 * Pruebas instrumentadas en emulador/dispositivo real para verificar la política
 * de seguridad de red aplicada en tiempo de ejecución por Android (Corrección 03 - Semana 12).
 */
@RunWith(AndroidJUnit4::class)
class NetworkSecurityInstrumentedTest {

    @Test
    fun politicaDeRed_deniegaCleartextGlobalYPermiteSoloHostsLocalesDeDesarrollo() {
        val policy = NetworkSecurityPolicy.getInstance()

        // Negativos: política global y destinos externos HTTP deben estar bloqueados
        assertFalse("El tráfico HTTP global debe estar denegado", policy.isCleartextTrafficPermitted)
        assertFalse("HTTP externo debe bloquearse", policy.isCleartextTrafficPermitted("example.com"))
        assertFalse("Backend externo histórico debe bloquearse", policy.isCleartextTrafficPermitted("foodjet.asen.pe"))
        assertFalse("IP arbitraria de LAN debe bloquearse", policy.isCleartextTrafficPermitted("192.168.1.10"))
        assertFalse("Subdominio de localhost debe bloquearse", policy.isCleartextTrafficPermitted("evil.localhost"))

        // Positivos (variante debug): solo los dos hosts locales autorizados
        assertTrue("10.0.2.2 debe estar permitido en debug para el emulador", policy.isCleartextTrafficPermitted("10.0.2.2"))
        assertTrue("localhost debe estar permitido en debug para ADB reverse", policy.isCleartextTrafficPermitted("localhost"))
    }

    @Test
    fun okHttp_rechazaPeticionCleartextADestinoExternoNoAutorizado() {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("http://example.com/")
            .build()

        var blockedByCleartextPolicy = false
        try {
            client.newCall(request).execute().use { /* No debe llegar a ejecutarse */ }
        } catch (e: UnknownServiceException) {
            blockedByCleartextPolicy = e.message?.contains("CLEARTEXT", ignoreCase = true) == true
        } catch (_: IOException) {
            // Si la política de red bloquea con otra IOException relacionada a cleartext
            blockedByCleartextPolicy = false
        }

        assertTrue(
            "OkHttp debe rechazar HTTP externo mediante NetworkSecurityPolicy (UnknownServiceException CLEARTEXT)",
            blockedByCleartextPolicy
        )
    }
}
