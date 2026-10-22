package com.example.foodjeetapp.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Línea de tiempo del seguimiento: misma regla que `lineaDeTiempo` de la web. */
class SeguimientoTest {

    private fun ms(iso: String) = Instant.parse(iso).toEpochMilli()

    private val plan = mapOf(
        "pendiente" to 12.0,
        "confirmado" to 6.0,
        "en_preparacion" to 60.0,
        "en_camino" to 90.0
    )

    @Test
    fun `normaliza estados heredados con tildes y espacios`() {
        assertEquals("en_preparacion", Seguimiento.normalizarEstado("En preparación"))
        assertEquals("en_camino", Seguimiento.normalizarEstado(" En Camino "))
        assertEquals("", Seguimiento.normalizarEstado(null))
    }

    @Test
    fun `las etapas pasadas llevan su hora real y las futuras una estimada`() {
        val historial = listOf(
            OrderHistoryEntry("pendiente", "2026-10-18T10:00:00Z", "cliente"),
            OrderHistoryEntry("confirmado", "2026-10-18T10:00:12Z", "simulador")
        )
        val linea = Seguimiento.lineaDeTiempo(
            estado = "confirmado",
            historial = historial,
            planSegundos = plan,
            fechaPedidoMs = ms("2026-10-18T10:00:00Z"),
            ahoraMs = ms("2026-10-18T10:00:13Z")
        )

        val confirmacion = linea.etapas[0]
        assertEquals(ms("2026-10-18T10:00:12Z"), confirmacion.horaMs)
        assertFalse(confirmacion.estimada)
        assertEquals(Seguimiento.Situacion.HECHA, confirmacion.situacion)

        // Confirmado dura 6 s: preparación a las 10:00:18, en camino 60 s después, entrega 90 s más.
        assertEquals(ms("2026-10-18T10:00:18Z"), linea.etapas[1].horaMs)
        assertTrue(linea.etapas[1].estimada)
        assertEquals(ms("2026-10-18T10:01:18Z"), linea.etapas[2].horaMs)
        assertEquals(ms("2026-10-18T10:02:48Z"), linea.llegadaMs)
        assertTrue(linea.llegadaEstimada)
    }

    @Test
    fun `si el estado actual ya vencio lo proximo es ahora`() {
        val linea = Seguimiento.lineaDeTiempo(
            estado = "en_preparacion",
            historial = listOf(OrderHistoryEntry("en_preparacion", "2026-10-18T10:00:00Z", "simulador")),
            planSegundos = plan,
            fechaPedidoMs = null,
            ahoraMs = ms("2026-10-18T10:05:00Z")
        )
        assertEquals(ms("2026-10-18T10:05:00Z"), linea.etapas[2].horaMs)
    }

    @Test
    fun `sin plan no se inventan horas`() {
        val linea = Seguimiento.lineaDeTiempo(
            estado = "en_preparacion",
            historial = emptyList(),
            planSegundos = emptyMap(),
            fechaPedidoMs = ms("2026-10-18T10:00:00Z"),
            ahoraMs = ms("2026-10-18T10:05:00Z")
        )
        assertTrue(linea.etapas.all { it.horaMs == null })
        assertNull(linea.llegadaMs)
    }

    @Test
    fun `un pedido cancelado no estima nada`() {
        val linea = Seguimiento.lineaDeTiempo(
            estado = "cancelado",
            historial = listOf(OrderHistoryEntry("pendiente", "2026-10-18T10:00:00Z", "cliente")),
            planSegundos = plan,
            fechaPedidoMs = ms("2026-10-18T10:00:00Z"),
            ahoraMs = ms("2026-10-18T10:01:00Z")
        )
        assertTrue(linea.cancelado)
        assertNull(linea.llegadaMs)
        assertTrue(linea.etapas.all { it.situacion == Seguimiento.Situacion.CANCELADA })
    }

    @Test
    fun `entregado usa la hora real de entrega`() {
        val linea = Seguimiento.lineaDeTiempo(
            estado = "entregado",
            historial = listOf(OrderHistoryEntry("entregado", "2026-10-18T10:03:00Z", "simulador")),
            planSegundos = plan,
            fechaPedidoMs = null,
            ahoraMs = ms("2026-10-18T11:00:00Z")
        )
        assertEquals(ms("2026-10-18T10:03:00Z"), linea.llegadaMs)
        assertFalse(linea.llegadaEstimada)
        assertTrue(linea.etapas.all { it.situacion == Seguimiento.Situacion.HECHA })
    }
}
