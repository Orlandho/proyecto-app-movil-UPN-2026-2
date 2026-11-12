package com.example.foodjeetapp.ui.components

import com.example.foodjeetapp.data.model.GeoPoint
import com.example.foodjeetapp.data.remote.ServidorFoodJet
import com.example.foodjeetapp.data.remote.dto.ConfigResponseDto
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException

class MapaFoodJetTest {

    @Before
    fun limpiarAntes() {
        ConfigMapa.reiniciarParaPruebas()
    }

    @After
    fun limpiarDespues() {
        ConfigMapa.reiniciarParaPruebas()
    }

    @Test
    fun `ConfigMapa no cachea lista vacia cuando falla el backend y reintenta luego`() = runBlocking {
        var llamadas = 0
        val fallo = ConfigMapa.keys(ServidorFoodJet.NODE_LOCAL_EMULADOR) {
            llamadas++
            Result.failure(IOException("Backend apagado"))
        }
        assertEquals(emptyList<String>(), fallo)
        assertEquals(1, llamadas)

        val recuperado = ConfigMapa.keys(ServidorFoodJet.NODE_LOCAL_EMULADOR) {
            llamadas++
            Result.success(ConfigResponseDto("KEY_1", listOf("KEY_1", "KEY_2")))
        }
        assertEquals(listOf("KEY_1", "KEY_2"), recuperado)
        assertEquals(2, llamadas)

        // Una vez obtenidas keys válidas, sí se cachean para el mismo servidor.
        val cacheado = ConfigMapa.keys(ServidorFoodJet.NODE_LOCAL_EMULADOR) {
            llamadas++
            Result.success(ConfigResponseDto("OTRA", emptyList()))
        }
        assertEquals(listOf("KEY_1", "KEY_2"), cacheado)
        assertEquals(2, llamadas)
    }

    @Test
    fun `ConfigMapa invalida cache al cambiar de servidor y deduplica con manifestKey`() = runBlocking {
        ConfigMapa.reiniciarParaPruebas(manifestKeyPrueba = "KEY_MANIFEST")
        var llamadas = 0

        val emulador = ConfigMapa.keys(ServidorFoodJet.NODE_LOCAL_EMULADOR) {
            llamadas++
            Result.success(ConfigResponseDto("KEY_MANIFEST", listOf("KEY_MANIFEST", "KEY_BACKUP")))
        }
        assertEquals(listOf("KEY_MANIFEST", "KEY_BACKUP"), emulador)
        assertEquals(1, llamadas)

        val usb = ConfigMapa.keys(ServidorFoodJet.NODE_LOCAL_USB) {
            llamadas++
            Result.success(ConfigResponseDto("KEY_USB", null))
        }
        assertEquals(listOf("KEY_MANIFEST", "KEY_USB"), usb)
        assertEquals(2, llamadas)
    }

    @Test
    fun `ControladorMapa guarda ultimo irA pendiente cuando el mapa aun no esta listo`() {
        val controlador = ControladorMapa()
        val p1 = GeoPoint(-12.10, -77.01)
        val p2 = GeoPoint(-12.13, -77.02)

        controlador.irA(p1, zoom = 15)
        assertEquals(p1 to 15, controlador.pendiente)

        controlador.irA(p2, zoom = 18)
        assertEquals(p2 to 18, controlador.pendiente)
        assertNull(controlador.vista)
    }

    @Test
    fun `ConfigMapa no cachea resultados fallidos cuando manifestKey esta ausente`() = runBlocking {
        var intentos = 0
        repeat(2) {
            val res = ConfigMapa.keys(ServidorFoodJet.NODE_LOCAL_EMULADOR) {
                intentos++
                Result.success(ConfigResponseDto(null, emptyList()))
            }
            assertEquals(emptyList<String>(), res)
        }
        assertEquals(2, intentos)
    }
}
