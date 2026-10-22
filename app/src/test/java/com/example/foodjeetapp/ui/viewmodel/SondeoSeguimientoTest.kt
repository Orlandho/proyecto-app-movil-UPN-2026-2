package com.example.foodjeetapp.ui.viewmodel

import com.example.foodjeetapp.data.model.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** REQ-SEM04-INF-02: el seguimiento no consulta más de lo necesario. */
class SondeoSeguimientoTest {

    @Test
    fun `en camino se consulta cada tres segundos`() {
        assertEquals(3_000L, OrderViewModel.intervaloDeSondeo(OrderStatus.EN_CAMINO, 0))
    }

    @Test
    fun `en los demas estados se espera mas`() {
        assertEquals(8_000L, OrderViewModel.intervaloDeSondeo(OrderStatus.EN_PREPARACION, 0))
        assertEquals(8_000L, OrderViewModel.intervaloDeSondeo(null, 0))
    }

    @Test
    fun `un pedido terminado deja de consultarse`() {
        assertNull(OrderViewModel.intervaloDeSondeo(OrderStatus.ENTREGADO, 0))
        assertNull(OrderViewModel.intervaloDeSondeo(OrderStatus.CANCELADO, 3))
    }

    @Test
    fun `tras fallos la espera se duplica hasta un tope`() {
        assertEquals(6_000L, OrderViewModel.intervaloDeSondeo(OrderStatus.EN_CAMINO, 1))
        assertEquals(12_000L, OrderViewModel.intervaloDeSondeo(OrderStatus.EN_CAMINO, 2))
        assertEquals(30_000L, OrderViewModel.intervaloDeSondeo(OrderStatus.EN_PREPARACION, 10))
    }
}
