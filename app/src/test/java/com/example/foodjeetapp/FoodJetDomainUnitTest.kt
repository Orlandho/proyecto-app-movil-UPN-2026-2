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
        assertEquals("Confirmado", OrderStatus.CONFIRMADO.label)
        assertEquals("En preparación", OrderStatus.EN_PREPARACION.label)
        assertEquals("En camino", OrderStatus.EN_CAMINO.label)
        assertEquals("Entregado", OrderStatus.ENTREGADO.label)
        assertEquals("Cancelado", OrderStatus.CANCELADO.label)

        // Verificar exhaustividad de estados
        val allStates = OrderStatus.values()
        assertEquals(6, allStates.size)
        assertTrue(allStates.contains(OrderStatus.PENDIENTE))
        assertTrue(allStates.contains(OrderStatus.CONFIRMADO))
        assertTrue(allStates.contains(OrderStatus.ENTREGADO))

        // Verificar propiedades terminales y cancelables
        assertTrue(OrderStatus.ENTREGADO.isTerminal)
        assertTrue(OrderStatus.CANCELADO.isTerminal)
        assertFalse(OrderStatus.PENDIENTE.isTerminal)
        assertTrue(OrderStatus.PENDIENTE.isCancelable)
        assertTrue(OrderStatus.CONFIRMADO.isCancelable)
        assertFalse(OrderStatus.EN_PREPARACION.isCancelable)
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

    @Test
    fun testOrderRecordNumericIdParsing() {
        val order1 = OrderRecord(
            id = "FJ-105",
            fecha = "12/09/2026",
            estado = OrderStatus.CONFIRMADO,
            items = emptyList(),
            subtotal = 30.0,
            impuestos = 5.4,
            total = 40.4
        )
        assertEquals(105, order1.numericId)

        val order2 = OrderRecord(
            id = "CUSTOM-99",
            fecha = "12/09/2026",
            estado = OrderStatus.PENDIENTE,
            items = emptyList(),
            subtotal = 20.0,
            impuestos = 3.6,
            total = 28.6
        )
        assertEquals(0, order2.numericId)
    }

    @Test
    fun testOrderDtoToDomainStatusAndTransactionMapping() {
        val productDto = com.example.foodjeetapp.data.remote.dto.ProductDto(
            id = 1,
            restauranteId = 1,
            nombre = "Hamburguesa Doble",
            descripcion = "Carne y queso",
            precio = 20.00,
            tipoComida = "Hamburguesas",
            imagenUrl = "/images/burger.jpg",
            descuentoEstudiante = 10.0,
            disponibilidad = true,
            restaurant = null
        )
        val itemDetailDto = com.example.foodjeetapp.data.remote.dto.OrderItemDetailDto(
            id = 1,
            productId = 1,
            cantidad = 2,
            precioUnitario = 20.00,
            product = productDto
        )
        val transactionDto = com.example.foodjeetapp.data.remote.dto.TransactionDto(
            id = 10,
            metodoPago = "wallet",
            monto = 45.00,
            estadoPago = "completado"
        )
        val dto = com.example.foodjeetapp.data.remote.dto.OrderResponseDto(
            id = 42,
            userId = 1,
            restauranteId = 1,
            direccionEntregaId = 1,
            total = 45.00,
            impuestos = 5.40,
            costoEnvio = 5.00,
            estado = "confirmado",
            fecha = "2026-09-12T01:00:00.000Z",
            restaurant = com.example.foodjeetapp.data.remote.dto.RestaurantDto(
                nombre = "Burger King",
                estadoAfiliacion = "activo",
                qrPago = null,
                tiempoEntrega = "30 min",
                calificacionPromedio = 4.5
            ),
            review = null,
            transaction = transactionDto,
            orderItems = listOf(itemDetailDto)
        )

        val domain = dto.toDomain()
        assertEquals("FJ-42", domain.id)
        assertEquals(42, domain.numericId)
        assertEquals(OrderStatus.CONFIRMADO, domain.estado)
        assertEquals("Billetera Digital", domain.paymentMethod)
        assertEquals(1, domain.items.size)
        assertEquals("Hamburguesa Doble", domain.items[0].product.nombre)
    }
}
