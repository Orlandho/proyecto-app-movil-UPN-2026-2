package com.example.foodjeetapp.data.remote

import com.example.foodjeetapp.data.remote.dto.ConfigResponseDto
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Solo backend/ del repositorio: Node.js en el puerto 3000 (emulador o USB). */
class ServidorFoodJetTest {
    private class CadenaFalsa(private val peticion: Request) : Interceptor.Chain {
        var enviada: Request? = null
        override fun request() = peticion
        override fun proceed(request: Request): Response {
            enviada = request
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

    private fun reescribir(servidor: ServidorFoodJet, url: String): String {
        val cadena = CadenaFalsa(Request.Builder().url(url).build())
        ServidorInterceptor { servidor }.intercept(cadena)
        return cadena.enviada!!.url.toString()
    }

    @Test fun `solo hay dos modos de acceso al mismo backend local`() {
        assertEquals(listOf(ServidorFoodJet.NODE_LOCAL_EMULADOR, ServidorFoodJet.NODE_LOCAL_USB), ServidorFoodJet.entries)
        assertEquals("http://10.0.2.2:3000/api/", RetrofitClient.TEMPLATE_API_URL)
    }

    @Test fun `emulador conserva ruta y parametros`() {
        assertEquals(
            "http://10.0.2.2:3000/api/orders/my-orders?x=1",
            reescribir(ServidorFoodJet.NODE_LOCAL_EMULADOR, "http://10.0.2.2:3000/api/orders/my-orders?x=1")
        )
    }

    @Test fun `conserva el path codificado sin alterar el query`() {
        assertEquals(
            "http://localhost:3000/api/products/item%2Fspecial?discount=100%25",
            reescribir(ServidorFoodJet.NODE_LOCAL_USB, "http://10.0.2.2:3000/api/products/item%2Fspecial?discount=100%25")
        )
    }

    @Test fun `modo usb solo usa localhost en puerto 3000`() {
        assertEquals(
            "http://localhost:3000/api/auth/login",
            reescribir(ServidorFoodJet.NODE_LOCAL_USB, "http://10.0.2.2:3000/api/auth/login")
        )
    }

    @Test fun `rechaza peticiones a produccion externa y proxy Docker`() {
        listOf(
            "https://foodjet.asen.pe/api/orders/7",
            "http://10.0.2.2:8081/api/orders/7",
            "http://localhost:8081/api/orders/7",
            "https://images.unsplash.com/photo.jpg",
            "http://10.0.2.2:3000/otra-ruta"
        ).forEach { url ->
            val cadena = CadenaFalsa(Request.Builder().url(url).build())
            var bloqueada = false
            try {
                ServidorInterceptor { ServidorFoodJet.NODE_LOCAL_EMULADOR }.intercept(cadena)
            } catch (_: IOException) {
                bloqueada = true
            }
            assertTrue("Debe bloquear: $url", bloqueada)
            assertNull(cadena.enviada)
        }
    }

    @Test fun `recursos QR solo provienen del backend local`() {
        assertEquals("http://10.0.2.2:3000/img/qr/burger-king.svg", ServidorFoodJet.NODE_LOCAL_EMULADOR.recurso("/img/qr/burger-king.svg"))
        assertEquals("http://localhost:3000/img/qr/burger-king.svg", ServidorFoodJet.NODE_LOCAL_USB.recurso("/img/qr/burger-king.svg"))
        assertNull(ServidorFoodJet.NODE_LOCAL_EMULADOR.recurso("https://foodjet.asen.pe/img/qr/externo.svg"))
        assertNull(ServidorFoodJet.NODE_LOCAL_EMULADOR.recurso("//example.com/image.svg"))
        assertNull(ServidorFoodJet.NODE_LOCAL_USB.recurso(" "))
        assertNull(ServidorFoodJet.NODE_LOCAL_USB.recurso("../secret"))
        assertNull(ServidorFoodJet.NODE_LOCAL_USB.recurso("https://example.com/qr.svg"))
    }

    @Test fun `configuraciones antiguas ya no habilitan servidores externos`() {
        assertEquals(ServidorFoodJet.NODE_LOCAL_USB, ServidorFoodJet.desdeNombre("LOCAL_USB"))
        assertEquals(ServidorFoodJet.NODE_LOCAL_EMULADOR, ServidorFoodJet.desdeNombre("LOCAL_EMULADOR"))
        assertEquals(ServidorFoodJet.PREDETERMINADO, ServidorFoodJet.desdeNombre("PRODUCCION"))
        assertEquals(ServidorFoodJet.PREDETERMINADO, ServidorFoodJet.desdeNombre(null))
    }

    @Test fun `la cascada de keys de mapa respeta el orden y quita repetidas`() {
        assertEquals(listOf("A", "B"), ConfigResponseDto("A", listOf("A", " B ", "")).keysDeMapa())
        assertEquals(listOf("A"), ConfigResponseDto("A", null).keysDeMapa())
        assertEquals(emptyList<String>(), ConfigResponseDto(null, null).keysDeMapa())
    }

    @Test fun `lee el mensaje de error que manda el backend`() {
        assertEquals("Hecho", ErroresApi.mensajeDelCuerpo("{\"message\":\"Hecho\"}"))
        assertNull(ErroresApi.mensajeDelCuerpo("<html>502</html>"))
    }
}
