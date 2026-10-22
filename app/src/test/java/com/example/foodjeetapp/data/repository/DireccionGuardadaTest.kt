package com.example.foodjeetapp.data.repository

import com.example.foodjeetapp.data.model.DeliverySelection
import com.example.foodjeetapp.data.model.GeoPoint
import com.example.foodjeetapp.data.model.SavedAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Reutilizar una dirección guardada solo si es la misma puerta (regla de la web). */
class DireccionGuardadaTest {

    private val casa = SavedAddress(7, "Av. Larco 345, Miraflores", null, -12.1219, -77.0297, predeterminada = true)
    private val sinPunto = SavedAddress(8, "Jr. Ucayali 120, Lima", null, null, null, predeterminada = false)

    @Test
    fun `mismo texto y mismo punto reutiliza la guardada`() {
        val entrega = DeliverySelection("  av. larco 345,   Miraflores ", null, GeoPoint(-12.12191, -77.02971))
        assertEquals(7, buscarDireccionGuardada(listOf(casa, sinPunto), entrega)?.id)
    }

    @Test
    fun `si el pin se movio se crea otra direccion`() {
        val entrega = DeliverySelection("Av. Larco 345, Miraflores", null, GeoPoint(-12.1230, -77.0297))
        assertNull(buscarDireccionGuardada(listOf(casa), entrega))
    }

    @Test
    fun `sin punto marcado basta con el texto`() {
        val entrega = DeliverySelection("Jr. Ucayali 120, Lima", null, null)
        assertEquals(8, buscarDireccionGuardada(listOf(casa, sinPunto), entrega)?.id)
    }

    @Test
    fun `un texto vacio nunca coincide`() {
        assertNull(buscarDireccionGuardada(listOf(casa), DeliverySelection("  ", null, null)))
    }
}
