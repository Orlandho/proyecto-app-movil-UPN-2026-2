package com.example.foodjeetapp.data.model

import com.example.foodjeetapp.data.remote.dto.AdminOrderDto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/** El panel admin calcula sus cifras de los pedidos reales, no de valores fijos. */
class EstadisticasAdminTest {

    private val hoy = LocalDate.of(2026, 10, 18)

    private fun pedido(id: Int, estado: String, total: Double, fecha: String, local: String) =
        AdminOrderDto(id = id, estado = estado, total = total, fecha = fecha, cliente = "Cliente $id", restaurante = local)

    private val pedidos = listOf(
        pedido(1, "entregado", 30.0, "2026-10-18T15:00:00Z", "Burger King"),
        pedido(2, "En camino", 20.0, "2026-10-18T16:00:00Z", "Pizza Hut"),
        pedido(3, "cancelado", 99.0, "2026-10-18T17:00:00Z", "Pizza Hut"),
        pedido(4, "pendiente", 10.0, "2026-10-16T12:00:00Z", "Burger King"),
        pedido(5, "entregado", 50.0, "2026-10-01T12:00:00Z", "Sushi Club")
    )

    private val estadisticas = EstadisticasAdmin.calcular(pedidos, hoy, ZoneOffset.UTC)

    @Test
    fun `las ventas por dia cubren siete dias y excluyen cancelados`() {
        assertEquals(7, estadisticas.ventasPorDia.size)
        assertEquals(50.0, estadisticas.ventasPorDia.last().second, 1e-9) // hoy: 30 + 20
        assertEquals(10.0, estadisticas.ventasPorDia[4].second, 1e-9)    // hace dos días
        assertEquals(60.0, estadisticas.ventasPorDia.sumOf { it.second }, 1e-9)
    }

    @Test
    fun `cuenta pedidos por estado canonico en el orden del flujo`() {
        assertEquals(
            listOf("pendiente" to 1, "en_camino" to 1, "entregado" to 2, "cancelado" to 1),
            estadisticas.pedidosPorEstado
        )
        assertEquals(2, estadisticas.pedidosActivos)
    }

    @Test
    fun `ordena las ventas por local de mayor a menor`() {
        assertEquals(
            listOf("Sushi Club" to 50.0, "Burger King" to 40.0, "Pizza Hut" to 20.0),
            estadisticas.ventasPorLocal
        )
        assertEquals(110.0, estadisticas.totalVendido, 1e-9)
    }

    @Test
    fun `sin pedidos todo queda en cero`() {
        val vacio = EstadisticasAdmin.calcular(emptyList(), hoy, ZoneOffset.UTC)
        assertEquals(7, vacio.ventasPorDia.size)
        assertEquals(0.0, vacio.totalVendido, 0.0)
        assertEquals(emptyList<Pair<String, Double>>(), vacio.ventasPorLocal)
    }
}
