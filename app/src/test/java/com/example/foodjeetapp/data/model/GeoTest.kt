package com.example.foodjeetapp.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** REQ-SEM10-LOG-03: cálculo de distancias y trayectorias. */
class GeoTest {

    private val plazaMayor = GeoPoint(-12.0464, -77.0428)
    private val larco = GeoPoint(-12.1219, -77.0297)

    @Test
    fun `un grado de latitud mide unos 111 km`() {
        assertEquals(111_195.0, Geo.distanciaMetros(GeoPoint(0.0, 0.0), GeoPoint(1.0, 0.0)), 5.0)
    }

    @Test
    fun `la distancia es simetrica y cero en el mismo punto`() {
        assertEquals(Geo.distanciaMetros(plazaMayor, larco), Geo.distanciaMetros(larco, plazaMayor), 1e-6)
        assertEquals(0.0, Geo.distanciaMetros(larco, larco), 1e-9)
        // Plaza Mayor a Av. Larco: unos 8,5 km en línea recta.
        assertEquals(8_500.0, Geo.distanciaMetros(plazaMayor, larco), 300.0)
    }

    @Test
    fun `la longitud de la ruta suma sus tramos`() {
        val a = GeoPoint(0.0, 0.0)
        val b = GeoPoint(0.0, 0.01)
        val c = GeoPoint(0.01, 0.01)
        val esperado = Geo.distanciaMetros(a, b) + Geo.distanciaMetros(b, c)
        assertEquals(esperado, Geo.longitudRuta(listOf(a, b, c)), 1e-6)
        assertEquals(0.0, Geo.longitudRuta(listOf(a)), 0.0)
    }

    @Test
    fun `dividir la ruta a la mitad deja al repartidor en el punto medio`() {
        val ruta = listOf(GeoPoint(0.0, 0.0), GeoPoint(0.0, 0.02))
        val mitad = Geo.dividirRuta(ruta, 0.5)
        assertEquals(0.0, mitad.posicion!!.latitud, 1e-9)
        assertEquals(0.01, mitad.posicion!!.longitud, 1e-6)
        assertEquals(Geo.longitudRuta(ruta) / 2, mitad.restanteMetros, 1.0)
        assertEquals(ruta.first(), mitad.recorrida.first())
        assertEquals(ruta.last(), mitad.pendiente.last())
    }

    @Test
    fun `los extremos y la ruta vacia no se salen de rango`() {
        val ruta = listOf(GeoPoint(0.0, 0.0), GeoPoint(0.0, 0.02))
        assertEquals(ruta.first(), Geo.dividirRuta(ruta, -1.0).posicion)
        val fin = Geo.dividirRuta(ruta, 2.0)
        assertEquals(ruta.last().longitud, fin.posicion!!.longitud, 1e-9)
        assertEquals(0.0, fin.restanteMetros, 1e-6)
        assertNull(Geo.dividirRuta(emptyList(), 0.5).posicion)
    }

    @Test
    fun `el avance del reparto sale del reloj`() {
        assertEquals(0.25, Geo.avanceDelReparto(1_000, 4_000, 2_000), 1e-9)
        assertEquals(1.0, Geo.avanceDelReparto(1_000, 4_000, 99_000), 1e-9)
        assertEquals(0.0, Geo.avanceDelReparto(null, 4_000, 2_000), 1e-9)
        assertEquals(1.0, Geo.avanceDelReparto(null, null, 0, entregado = true), 1e-9)
    }

    @Test
    fun `los textos coinciden con los de la web`() {
        assertEquals("850 m", Geo.formatearDistancia(847.0))
        assertEquals("6,4 km", Geo.formatearDistancia(6_420.0))
        assertEquals("312 km", Geo.formatearDistancia(312_400.0))
        assertEquals("Llegando", Geo.formatearRestante(20_000))
        assertEquals("2 min", Geo.formatearRestante(90_000))
        assertEquals("1 h 2 min", Geo.formatearRestante(3_720_000))
        assertEquals("2 h", Geo.formatearRestante(7_200_000))
    }

    @Test
    fun `valida coordenadas y limites de Lima`() {
        assertTrue(Geo.coordenadasValidas(-12.0, -77.0))
        assertFalse(Geo.coordenadasValidas(null, -77.0))
        assertFalse(Geo.coordenadasValidas(-95.0, -77.0))
        assertFalse(Geo.coordenadasValidas(Double.NaN, -77.0))
        assertTrue(Geo.dentroDeLima(larco))
        assertFalse(Geo.dentroDeLima(GeoPoint(-16.4, -71.5))) // Arequipa
    }
}
