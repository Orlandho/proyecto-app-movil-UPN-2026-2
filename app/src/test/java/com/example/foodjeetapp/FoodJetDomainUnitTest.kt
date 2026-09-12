package com.example.foodjeetapp

import com.example.foodjeetapp.data.model.CartItem
import com.example.foodjeetapp.data.model.OrderRecord
import com.example.foodjeetapp.data.model.OrderStatus
import com.example.foodjeetapp.data.model.ProductItem
import com.example.foodjeetapp.data.model.UserProfile
import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas de Caja Blanca y Unitarias para la lógica de dominio móvil de FoodJet.
 * Valida ramas lógicas, condiciones de borde y cálculo de precios con beneficios estudiantiles.
 */
class FoodJetDomainUnitTest {

    @Test
    fun testDescuentoEstudianteAplicadoCorrectamente() {
        val producto = ProductItem(
            id = 1,
            nombre = "Hamburguesa Doble",
            descripcion = "Carne a la parrilla con queso",
            precio = 20.0,
            tipoComida = "Hamburguesas",
            imagenUrl = "https://example.com/burger.jpg",
            descuentoEstudiante = 20.0
        )

        // Caso con estudiante: debe descontar 20% (S/ 4.00), precio final S/ 16.00
        val precioEstudiante = producto.getEffectivePrice(isStudent = true)
        assertEquals(16.0, precioEstudiante, 0.001)

        // Caso sin estudiante: debe mantener el precio regular de S/ 20.00
        val precioRegular = producto.getEffectivePrice(isStudent = false)
        assertEquals(20.0, precioRegular, 0.001)
    }

    @Test
    fun testProductoSinDescuentoEstudianteMantienePrecio() {
        val producto = ProductItem(
            id = 2,
            nombre = "Papas Fritas",
            descripcion = "Papas medianas crujientes",
            precio = 8.50,
            tipoComida = "Acompañamientos",
            imagenUrl = "https://example.com/fries.jpg",
            descuentoEstudiante = 0.0
        )

        // Ambos casos deben dar exactamente el precio original
        assertEquals(8.50, producto.getEffectivePrice(isStudent = true), 0.001)
        assertEquals(8.50, producto.getEffectivePrice(isStudent = false), 0.001)
    }

    @Test
    fun testOrderStatusLabelsAndTransitions() {
        assertEquals("Pendiente", OrderStatus.PENDIENTE.label)
        assertEquals("En preparación", OrderStatus.EN_PREPARACION.label)
        assertEquals("En camino", OrderStatus.EN_CAMINO.label)
        assertEquals("Entregado", OrderStatus.ENTREGADO.label)
        assertEquals("Cancelado", OrderStatus.CANCELADO.label)

        // Verificar exhaustividad de estados
        val allStates = OrderStatus.values()
        assertEquals(5, allStates.size)
        assertTrue(allStates.contains(OrderStatus.PENDIENTE))
        assertTrue(allStates.contains(OrderStatus.ENTREGADO))
    }

    @Test
    fun testOrderRecordTotalsIntegrity() {
        val producto = ProductItem(
            id = 10,
            nombre = "Combo Whopper",
            descripcion = "Combo completo con bebida",
            precio = 25.0,
            tipoComida = "Combos",
            imagenUrl = "https://example.com/whopper.jpg"
        )
        val item = CartItem(product = producto, quantity = 2)

        val subtotal = 50.0
        val impuestos = 9.0
        val envio = 5.0
        val total = subtotal + impuestos + envio

        val pedido = OrderRecord(
            id = "ORD-2026-001",
            fecha = "11/09/2026",
            estado = OrderStatus.PENDIENTE,
            items = listOf(item),
            subtotal = subtotal,
            impuestos = impuestos,
            envio = envio,
            total = total,
            paymentMethod = "Billetera Digital"
        )

        assertEquals("ORD-2026-001", pedido.id)
        assertEquals(OrderStatus.PENDIENTE, pedido.estado)
        assertEquals(64.0, pedido.total, 0.001)
        assertEquals(1, pedido.items.size)
        assertEquals(2, pedido.items[0].quantity)
    }

    @Test
    fun testUserProfileDefaultsAndRoleValidation() {
        val defaultUser = UserProfile()
        assertTrue("Por defecto el usuario de prueba debe tener beneficio de estudiante", defaultUser.isStudent)
        assertFalse("Por defecto el usuario cliente no debe ser admin", defaultUser.isAdmin)
        assertEquals("987654321", defaultUser.phone)

        val adminUser = defaultUser.copy(isAdmin = true, isStudent = false)
        assertTrue(adminUser.isAdmin)
        assertFalse(adminUser.isStudent)
    }
}
