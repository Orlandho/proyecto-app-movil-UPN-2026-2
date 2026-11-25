package com.example.foodjeetapp.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException

class ErroresApiTest {

    @Test
    fun `error de conexion en modo usb indica backend local y adb reverse sin mencionar Docker`() {
        val servidorPrevio = RetrofitClient.servidor
        try {
            RetrofitClient.servidor = ServidorFoodJet.NODE_LOCAL_USB

            val mensaje = ErroresApi.mensajeDeRed(
                ConnectException("Failed to connect to localhost/127.0.0.1:3000"),
                "cargar el catálogo"
            )

            assertTrue(mensaje.contains(ServidorFoodJet.NODE_LOCAL_USB.etiqueta))
            assertTrue(mensaje.contains("cargar el catálogo"))
            assertTrue(mensaje.contains("backend/"))
            assertTrue(mensaje.contains("adb reverse tcp:3000 tcp:3000"))
            assertFalse("No debe mencionar Docker: $mensaje", mensaje.contains("docker", ignoreCase = true))
        } finally {
            RetrofitClient.servidor = servidorPrevio
        }
    }

    @Test
    fun `error de conexion en modo emulador indica backend local en puerto 3000 sin mencionar Docker`() {
        val servidorPrevio = RetrofitClient.servidor
        try {
            RetrofitClient.servidor = ServidorFoodJet.NODE_LOCAL_EMULADOR

            val mensaje = ErroresApi.mensajeDeRed(
                ConnectException("Failed to connect to /10.0.2.2:3000"),
                "cargar el catálogo"
            )

            assertTrue(mensaje.contains(ServidorFoodJet.NODE_LOCAL_EMULADOR.etiqueta))
            assertTrue(mensaje.contains("cargar el catálogo"))
            assertTrue(mensaje.contains("backend/"))
            assertTrue(mensaje.contains("3000"))
            assertFalse("No debe mencionar Docker: $mensaje", mensaje.contains("docker", ignoreCase = true))
            assertFalse("No debe pedir adb reverse en emulador: $mensaje", mensaje.contains("adb reverse", ignoreCase = true))
        } finally {
            RetrofitClient.servidor = servidorPrevio
        }
    }
}
