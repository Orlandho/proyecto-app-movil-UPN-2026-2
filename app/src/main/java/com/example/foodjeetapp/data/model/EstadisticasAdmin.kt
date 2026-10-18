package com.example.foodjeetapp.data.model

import com.example.foodjeetapp.data.remote.dto.AdminOrderDto
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Cifras del panel de operaciones, calculadas de los pedidos que devuelve
 * `GET /api/orders`. Los cancelados no cuentan como venta.
 */
data class EstadisticasAdmin(
    /** Últimos siete días, del más antiguo a hoy: etiqueta del día y soles vendidos. */
    val ventasPorDia: List<Pair<String, Double>>,
    /** Cantidad de pedidos por estado canónico (pendiente, en_camino...). */
    val pedidosPorEstado: List<Pair<String, Int>>,
    /** Soles vendidos por local, de mayor a menor. */
    val ventasPorLocal: List<Pair<String, Double>>,
    val totalVendido: Double,
    val pedidosActivos: Int
) {
    companion object {
        private val ORDEN_ESTADOS = listOf("pendiente", "confirmado", "en_preparacion", "en_camino", "entregado", "cancelado")

        fun calcular(
            pedidos: List<AdminOrderDto>,
            hoy: LocalDate = LocalDate.now(),
            zona: ZoneId = ZoneId.systemDefault()
        ): EstadisticasAdmin {
            val conEstado = pedidos.map { it to Seguimiento.normalizarEstado(it.estado) }
            val vendidos = conEstado.filter { (_, estado) -> estado != "cancelado" }.map { it.first }

            val diaDe = { pedido: AdminOrderDto ->
                Seguimiento.parseIso(pedido.fecha)?.let { java.time.Instant.ofEpochMilli(it).atZone(zona).toLocalDate() }
            }
            val porDia = vendidos.groupBy(diaDe).mapValues { (_, lista) -> lista.sumOf { it.total } }
            val ventasPorDia = (6 downTo 0).map { atras ->
                val dia = hoy.minusDays(atras.toLong())
                etiqueta(dia.dayOfWeek) to (porDia[dia] ?: 0.0)
            }

            val porEstado = conEstado.groupingBy { it.second }.eachCount()
            val pedidosPorEstado = porEstado.entries
                .sortedBy { ORDEN_ESTADOS.indexOf(it.key).let { i -> if (i == -1) Int.MAX_VALUE else i } }
                .map { it.key to it.value }

            val ventasPorLocal = vendidos
                .groupBy { it.restaurante?.takeIf(String::isNotBlank) ?: "Sin local" }
                .mapValues { (_, lista) -> lista.sumOf { it.total } }
                .entries.sortedByDescending { it.value }
                .map { it.key to it.value }

            return EstadisticasAdmin(
                ventasPorDia = ventasPorDia,
                pedidosPorEstado = pedidosPorEstado,
                ventasPorLocal = ventasPorLocal,
                totalVendido = vendidos.sumOf { it.total },
                pedidosActivos = conEstado.count { (_, estado) -> estado !in setOf("entregado", "cancelado") }
            )
        }

        private fun etiqueta(dia: DayOfWeek): String =
            dia.getDisplayName(TextStyle.SHORT, Locale("es", "PE")).trimEnd('.').replaceFirstChar { it.uppercase() }
    }
}
