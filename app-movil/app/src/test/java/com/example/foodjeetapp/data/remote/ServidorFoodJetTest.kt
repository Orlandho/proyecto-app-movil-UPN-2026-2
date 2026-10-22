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
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Selección de servidor: producción por el túnel o el Docker local. */
class ServidorFoodJetTest {

    /** Cadena mínima que registra la petición que le llega. */
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

    @Test
    fun `produccion deja la URL igual`() {
        assertEquals(
            "https://foodjet.asen.pe/api/orders/7",
            reescribir(ServidorFoodJet.PRODUCCION, "https://foodjet.asen.pe/api/orders/7")
        )
    }

    @Test
    fun `el emulador va al proxy local por 10_0_2_2 conservando ruta y consulta`() {
        assertEquals(
            "http://10.0.2.2:8081/api/orders/my-orders?x=1",
            reescribir(ServidorFoodJet.LOCAL_EMULADOR, "https://foodjet.asen.pe/api/orders/my-orders?x=1")
        )
    }

    @Test
    fun `el celular por USB usa localhost con adb reverse`() {
        assertEquals(
            "http://localhost:8081/api/favorites/3/toggle",
            reescribir(ServidorFoodJet.LOCAL_USB, "https://foodjet.asen.pe/api/favorites/3/toggle")
        )
    }

    @Test
    fun `otros hosts no se tocan`() {
        assertEquals(
            "https://images.unsplash.com/photo.jpg",
            reescribir(ServidorFoodJet.LOCAL_EMULADOR, "https://images.unsplash.com/photo.jpg")
        )
    }

    @Test
    fun `resuelve recursos relativos de la web`() {
        assertEquals("http://10.0.2.2:8081/img/qr/burger-king.svg", ServidorFoodJet.LOCAL_EMULADOR.recurso("/img/qr/burger-king.svg"))
        assertEquals("https://cdn.test/qr.svg", ServidorFoodJet.PRODUCCION.recurso("https://cdn.test/qr.svg"))
        assertNull(ServidorFoodJet.PRODUCCION.recurso("  "))
    }

    @Test
    fun `un nombre desconocido vuelve al servidor predeterminado`() {
        assertEquals(ServidorFoodJet.LOCAL_USB, ServidorFoodJet.desdeNombre("LOCAL_USB"))
        assertEquals(ServidorFoodJet.PREDETERMINADO, ServidorFoodJet.desdeNombre(null))
        assertEquals(ServidorFoodJet.PREDETERMINADO, ServidorFoodJet.desdeNombre("otro"))
    }

    @Test
    fun `la cascada de keys de mapa respeta el orden y quita repetidas`() {
        assertEquals(listOf("A", "B"), ConfigResponseDto("A", listOf("A", " B ", "")).keysDeMapa())
        assertEquals(listOf("A"), ConfigResponseDto("A", null).keysDeMapa())
        assertEquals(emptyList<String>(), ConfigResponseDto(null, null).keysDeMapa())
    }

    @Test
    fun `lee el mensaje de error que manda el backend`() {
        assertEquals("La dirección de entrega no te pertenece.", ErroresApi.mensajeDelCuerpo("{\"error\":\"La dirección de entrega no te pertenece.\"}"))
        assertEquals("Hecho", ErroresApi.mensajeDelCuerpo("{\"message\":\"Hecho\"}"))
        assertNull(ErroresApi.mensajeDelCuerpo("<html>502</html>"))
        assertNull(ErroresApi.mensajeDelCuerpo(null))
    }
}
