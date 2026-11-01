package com.example.foodjeetapp.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

/** El formulario de tarjeta no acepta datos vacíos ni imposibles. */
class ValidacionTarjetaTest {

    private val hoy = YearMonth.of(2026, 11)

    @Test
    fun `acepta numeros de prueba publicados que cumplen Luhn`() {
        assertTrue(ValidacionTarjeta.numeroValido("4242424242424242"))
        assertTrue(ValidacionTarjeta.numeroValido("5555 5555 5555 4444"))
        assertTrue(ValidacionTarjeta.numeroValido("378282246310005"))
    }

    @Test
    fun `rechaza numeros vacios cortos o con Luhn incorrecto`() {
        assertFalse(ValidacionTarjeta.numeroValido(""))
        assertFalse(ValidacionTarjeta.numeroValido("4242"))
        assertFalse(ValidacionTarjeta.numeroValido("4242424242424241"))
        assertFalse(ValidacionTarjeta.numeroValido("4242a24242424242"))
    }

    @Test
    fun `el vencimiento no puede estar en el pasado ni tener mes imposible`() {
        assertTrue(ValidacionTarjeta.vencimientoValido("11/26", hoy))
        assertTrue(ValidacionTarjeta.vencimientoValido("01/30", hoy))
        assertFalse(ValidacionTarjeta.vencimientoValido("10/26", hoy))
        assertFalse(ValidacionTarjeta.vencimientoValido("13/27", hoy))
        assertFalse(ValidacionTarjeta.vencimientoValido("00/27", hoy))
        assertFalse(ValidacionTarjeta.vencimientoValido("1227", hoy))
        assertFalse(ValidacionTarjeta.vencimientoValido("", hoy))
    }

    @Test
    fun `el cvc tiene 3 o 4 digitos`() {
        assertTrue(ValidacionTarjeta.cvcValido("123"))
        assertTrue(ValidacionTarjeta.cvcValido("1234"))
        assertFalse(ValidacionTarjeta.cvcValido("12"))
        assertFalse(ValidacionTarjeta.cvcValido(""))
    }

    @Test
    fun `la tarjeta completa solo vale con los tres datos correctos`() {
        assertTrue(ValidacionTarjeta.tarjetaValida("4242424242424242", "12/28", "123", hoy))
        assertFalse(ValidacionTarjeta.tarjetaValida("", "", "", hoy))
        assertFalse(ValidacionTarjeta.tarjetaValida("4242424242424242", "12/28", "", hoy))
    }

    @Test
    fun `da formato MM AA mientras se escribe`() {
        assertEquals("1", ValidacionTarjeta.formatearVencimiento("1"))
        assertEquals("12", ValidacionTarjeta.formatearVencimiento("12"))
        assertEquals("12/2", ValidacionTarjeta.formatearVencimiento("122"))
        assertEquals("12/28", ValidacionTarjeta.formatearVencimiento("12/28"))
        assertEquals("12/28", ValidacionTarjeta.formatearVencimiento("122899"))
    }
}
