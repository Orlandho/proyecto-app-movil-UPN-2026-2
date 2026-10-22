package com.example.foodjeetapp.data.model

import java.text.Normalizer
import java.time.Instant

/**
 * Línea de tiempo del seguimiento de un pedido.
 *
 * Port de `lineaDeTiempo` de la web (`frontend/js/domain/seguimiento.js`):
 * cada etapa lleva su hora real cuando ya ocurrió (sale del historial de
 * estados del backend) o una estimada, encadenando las duraciones del plan
 * del simulador, cuando aún no. Si falta una duración se deja de estimar:
 * a partir de ahí no hay estimación honesta y la etapa queda sin hora.
 */
object Seguimiento {

    enum class Situacion { HECHA, EN_CURSO, PENDIENTE, CANCELADA }

    data class Etapa(
        val etiqueta: String,
        val estado: String,
        val situacion: Situacion,
        val horaMs: Long?,
        val estimada: Boolean
    )

    data class Linea(
        val etapas: List<Etapa>,
        val llegadaMs: Long?,
        val llegadaEstimada: Boolean,
        val cancelado: Boolean
    )

    private val ETAPAS = listOf(
        "Confirmación" to "confirmado",
        "Preparación" to "en_preparacion",
        "En camino" to "en_camino",
        "Entregado" to "entregado"
    )

    private val FLUJO = listOf("pendiente", "confirmado", "en_preparacion", "en_camino", "entregado")

    /** Etapa en curso y cuántas quedaron atrás, según el estado del pedido. */
    private val PROGRESO = mapOf(
        "pendiente" to (0 to 0),
        "confirmado" to (1 to 1),
        "en_preparacion" to (1 to 1),
        "en_camino" to (2 to 2),
        "entregado" to (-1 to 4),
        "cancelado" to (-1 to 0)
    )

    /** Forma canónica de un estado: 'En preparación' → 'en_preparacion'. */
    fun normalizarEstado(valor: String?): String {
        if (valor == null) return ""
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), "_")
    }

    fun parseIso(fecha: String?): Long? =
        fecha?.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

    fun lineaDeTiempo(
        estado: String,
        historial: List<OrderHistoryEntry>,
        planSegundos: Map<String, Double>,
        fechaPedidoMs: Long?,
        ahoraMs: Long
    ): Linea {
        val actual = normalizarEstado(estado).ifEmpty { "pendiente" }
        val (activa, hechas) = PROGRESO[actual] ?: PROGRESO.getValue("pendiente")
        val cancelado = actual == "cancelado"

        val reales = mutableMapOf<String, Long>()
        historial.forEach { entrada ->
            val clave = normalizarEstado(entrada.estado)
            val hora = parseIso(entrada.fecha)
            // Si un estado se repitiera, cuenta la primera vez.
            if (hora != null && clave !in reales) reales[clave] = hora
        }
        val estimadas = if (cancelado) emptyMap() else estimar(actual, reales, planSegundos, ahoraMs, fechaPedidoMs)

        val etapas = ETAPAS.mapIndexed { i, (etiqueta, clave) ->
            val situacion = when {
                i < hechas -> Situacion.HECHA
                i == activa -> Situacion.EN_CURSO
                cancelado -> Situacion.CANCELADA
                else -> Situacion.PENDIENTE
            }
            val real = reales[clave]
            val estimada = estimadas[clave]
            Etapa(etiqueta, clave, situacion, real ?: estimada, real == null && estimada != null)
        }
        val entrega = etapas.last()
        return Linea(etapas, entrega.horaMs, entrega.estimada, cancelado)
    }

    private fun estimar(
        actual: String,
        reales: Map<String, Long>,
        plan: Map<String, Double>,
        ahoraMs: Long,
        fechaPedidoMs: Long?
    ): Map<String, Long> {
        val indice = FLUJO.indexOf(actual)
        if (indice == -1 || plan.isEmpty()) return emptyMap()
        val inicioActual = reales[actual] ?: fechaPedidoMs ?: return emptyMap()
        val duracionActual = plan[actual] ?: return emptyMap()

        val estimadas = mutableMapOf<String, Long>()
        // Si el estado ya debería haber terminado, lo próximo es "ya".
        var siguiente = maxOf(inicioActual + (duracionActual * 1000).toLong(), ahoraMs)
        for (i in indice + 1 until FLUJO.size) {
            val estado = FLUJO[i]
            estimadas[estado] = siguiente
            if (estado == "entregado") break
            val duracion = plan[estado] ?: break
            siguiente += (duracion * 1000).toLong()
        }
        return estimadas
    }
}
