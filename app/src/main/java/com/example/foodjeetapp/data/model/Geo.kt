package com.example.foodjeetapp.data.model

import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Cálculos geográficos del reparto (REQ-SEM10-LOG-03).
 *
 * Funciones puras, sin Android: se prueban en la JVM. Son el mismo cálculo
 * que hacen el backend (`domain/geo.js`) y la web
 * (`frontend/js/domain/seguimiento.js`), para que la app, la web y el
 * servidor den la misma distancia y la misma posición del repartidor.
 */
object Geo {

    private const val RADIO_TIERRA_M = 6_371_000.0

    /** Plaza Mayor de Lima: donde abre el mapa si no sabemos nada del cliente. */
    val LIMA_CENTRO = GeoPoint(-12.0464, -77.0428)

    /** Lima Metropolitana y Callao, los mismos límites que usa la web. */
    const val LIMA_SUR = -12.52
    const val LIMA_OESTE = -77.2
    const val LIMA_NORTE = -11.57
    const val LIMA_ESTE = -76.62

    /** Por debajo de esta distancia dos puntos son la misma puerta (error típico del GPS). */
    const val MISMA_UBICACION_M = 25.0

    private fun rad(grados: Double) = grados * Math.PI / 180.0

    fun coordenadasValidas(lat: Double?, lon: Double?): Boolean =
        lat != null && lon != null && lat.isFinite() && lon.isFinite() &&
            lat in -90.0..90.0 && lon in -180.0..180.0

    fun dentroDeLima(punto: GeoPoint): Boolean =
        punto.latitud in LIMA_SUR..LIMA_NORTE && punto.longitud in LIMA_OESTE..LIMA_ESTE

    /** Distancia en metros entre dos puntos (fórmula de haversine). */
    fun distanciaMetros(a: GeoPoint, b: GeoPoint): Double {
        val dLat = rad(b.latitud - a.latitud)
        val dLon = rad(b.longitud - a.longitud)
        val h = sin(dLat / 2).let { it * it } +
            cos(rad(a.latitud)) * cos(rad(b.latitud)) * sin(dLon / 2).let { it * it }
        return 2 * RADIO_TIERRA_M * asin(min(1.0, sqrt(h)))
    }

    /** Longitud de una ruta en metros, sumando sus tramos. */
    fun longitudRuta(ruta: List<GeoPoint>): Double =
        ruta.zipWithNext { a, b -> distanciaMetros(a, b) }.sum()

    /** Fracción del trayecto recorrida (0 a 1) según el reloj. */
    fun avanceDelReparto(inicioMs: Long?, duracionMs: Long?, ahoraMs: Long, entregado: Boolean = false): Double {
        if (entregado) return 1.0
        if (inicioMs == null || duracionMs == null || duracionMs <= 0) return 0.0
        return ((ahoraMs - inicioMs).toDouble() / duracionMs).coerceIn(0.0, 1.0)
    }

    data class RutaDividida(
        val recorrida: List<GeoPoint>,
        val pendiente: List<GeoPoint>,
        val posicion: GeoPoint?,
        val restanteMetros: Double
    )

    /** Parte la ruta por el punto que corresponde a [fraccion] de su longitud. */
    fun dividirRuta(ruta: List<GeoPoint>, fraccion: Double): RutaDividida {
        if (ruta.isEmpty()) return RutaDividida(emptyList(), emptyList(), null, 0.0)

        val total = longitudRuta(ruta)
        val objetivo = total * fraccion.coerceIn(0.0, 1.0)
        var acumulado = 0.0

        for (i in 1 until ruta.size) {
            val tramo = distanciaMetros(ruta[i - 1], ruta[i])
            if (acumulado + tramo >= objetivo) {
                val t = if (tramo == 0.0) 0.0 else (objetivo - acumulado) / tramo
                val a = ruta[i - 1]
                val b = ruta[i]
                val posicion = GeoPoint(
                    a.latitud + (b.latitud - a.latitud) * t,
                    a.longitud + (b.longitud - a.longitud) * t
                )
                return RutaDividida(
                    recorrida = ruta.subList(0, i) + posicion,
                    pendiente = listOf(posicion) + ruta.subList(i, ruta.size),
                    posicion = posicion,
                    restanteMetros = (total - objetivo).coerceAtLeast(0.0)
                )
            }
            acumulado += tramo
        }

        val fin = ruta.last()
        return RutaDividida(ruta, listOf(fin), fin, 0.0)
    }

    /** "850 m", "6,4 km" o "312 km", como la web. */
    fun formatearDistancia(metros: Double?): String {
        val m = (metros ?: 0.0).coerceAtLeast(0.0)
        return when {
            m < 1000 -> "${(m / 10).roundToInt() * 10} m"
            m < 100_000 -> String.format(java.util.Locale.US, "%.1f", m / 1000).replace('.', ',') + " km"
            else -> "${(m / 1000).roundToInt()} km"
        }
    }

    /** Cuánto falta para una hora, dicho para el cliente. */
    fun formatearRestante(restanteMs: Long): String {
        val segundos = (restanteMs / 1000.0).roundToInt()
        if (segundos <= 30) return "Llegando"
        val minutos = ceil(segundos / 60.0).toInt()
        if (minutos < 60) return "$minutos min"
        val horas = minutos / 60
        val resto = minutos % 60
        return if (resto != 0) "$horas h $resto min" else "$horas h"
    }
}
