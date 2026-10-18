package com.example.foodjeetapp.ui.screens

import com.example.foodjeetapp.data.model.GeoPoint
import com.example.foodjeetapp.data.model.OrderRecord
import com.example.foodjeetapp.data.model.OrderStatus
import com.example.foodjeetapp.data.remote.dto.OrderResponseDto
import com.example.foodjeetapp.data.remote.dto.TrackingProgressDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** Posición del repartidor derivada de la salida y la duración, como en la web. */
class RepartoEnVivoTest {

    private val ruta = listOf(GeoPoint(-12.0, -77.0), GeoPoint(-12.0, -77.02))
    private val salida = "2026-10-18T10:00:00Z"
    private fun ms(iso: String) = Instant.parse(iso).toEpochMilli()

    private fun pedido(estado: OrderStatus, plan: Map<String, Double> = mapOf("en_camino" to 100.0)) = OrderRecord(
        id = "FJ-1", fecha = "2026-10-18", estado = estado, items = emptyList(),
        subtotal = 0.0, impuestos = 0.0, total = 0.0,
        rutaPuntos = ruta, planTiempos = plan, envioIniciadoEn = salida
    )

    @Test
    fun `a mitad del tiempo el repartidor va a mitad de ruta`() {
        val vivo = repartoEnVivo(pedido(OrderStatus.EN_CAMINO), ms("2026-10-18T10:00:50Z"))!!
        assertEquals(0.5, vivo.avance, 1e-9)
        assertEquals(-77.01, vivo.posicion!!.longitud, 1e-6)
        assertEquals(ms("2026-10-18T10:01:40Z"), vivo.llegadaMs)
    }

    @Test
    fun `entregado queda al final aunque el reloj diga otra cosa`() {
        val vivo = repartoEnVivo(pedido(OrderStatus.ENTREGADO), ms("2026-10-18T10:00:10Z"))!!
        assertEquals(1.0, vivo.avance, 1e-9)
        assertEquals(0.0, vivo.restanteMetros!!, 1e-6)
    }

    @Test
    fun `sin ruta ni avance del servidor no se inventa posicion`() {
        val sinDatos = pedido(OrderStatus.EN_CAMINO).copy(rutaPuntos = emptyList(), envioIniciadoEn = null)
        assertNull(repartoEnVivo(sinDatos, ms("2026-10-18T10:00:50Z")))
    }

    @Test
    fun `el DTO lleva el plan, la salida y el desfase de reloj`() {
        val dto = OrderResponseDto(
            id = 12, userId = 1, restauranteId = 2, direccionEntregaId = 3, total = 40.0,
            impuestos = 5.0, costoEnvio = 5.0, estado = "En camino", fecha = "2026-10-18T09:58:00Z",
            restaurant = null, review = null, transaction = null, orderItems = emptyList(),
            ruta = listOf(listOf(-12.0, -77.0), listOf(-12.0, -77.02)),
            planTiempos = mapOf("en_camino" to 100.0, "En preparación" to 60.0),
            seguimiento = TrackingProgressDto(avance = 0.4),
            servidorAhora = "2026-10-18T10:00:05Z",
            envioIniciadoEn = salida
        )
        val dominio = dto.toDomain(recibidoEnMs = ms("2026-10-18T10:00:00Z"))
        assertEquals(OrderStatus.EN_CAMINO, dominio.estado)
        assertEquals(mapOf("en_camino" to 100.0, "en_preparacion" to 60.0), dominio.planTiempos)
        assertEquals(5_000L, dominio.desfaseRelojMs)
        assertEquals(salida, dominio.envioIniciadoEn)
        assertEquals(2, dominio.restauranteId)
        assertEquals(ruta, dominio.rutaPuntos)
    }
}
