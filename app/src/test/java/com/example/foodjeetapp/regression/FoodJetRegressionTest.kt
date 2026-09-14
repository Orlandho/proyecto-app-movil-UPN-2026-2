package com.example.foodjeetapp.regression

import com.example.foodjeetapp.data.model.CartItem
import com.example.foodjeetapp.data.model.OrderRecord
import com.example.foodjeetapp.data.model.OrderStatus
import com.example.foodjeetapp.data.model.ProductItem
import com.example.foodjeetapp.data.model.UserProfile
import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas de Regresión para la App Móvil de FoodJet.
 * Garantiza la estabilidad histórica y no degradación de modelos, compatibilidad hacia atrás
 * y manejo seguro de condiciones de borde (Blast Radius).
 */
class FoodJetRegressionTest {

    @Test
    fun testDataModelBackwardCompatibility() {
        // Validación de constructores y valores por defecto para no romper compatibilidad
        val user = UserProfile()
        assertEquals("Orlando Dorival", user.name)
        assertEquals("987654321", user.phone)
        assertTrue("isStudent debe ser true por defecto en entorno universitario", user.isStudent)
        assertFalse("isAdmin debe ser false por defecto", user.isAdmin)

        val product = ProductItem(
            id = 99,
            nombre = "Bebida Simple",
            descripcion = "Agua mineral",
            precio = 3.50,
            tipoComida = "Bebidas",
            imagenUrl = "https://img/agua.png"
        )
        assertEquals(0.0, product.descuentoEstudiante, 0.001)
        assertTrue(product.disponibilidad)
        assertEquals(3.50, product.getEffectivePrice(isStudent = true), 0.001)
        assertEquals(3.50, product.getEffectivePrice(isStudent = false), 0.001)
    }

    @Test
    fun testOrderNumericIdRegressionHandling() {
        // Órdenes con prefijo "FJ-" deben extraer su ID numérico
        val orderFJ = OrderRecord(
            id = "FJ-789",
            fecha = "14/09/2026",
            estado = OrderStatus.PENDIENTE,
            items = emptyList(),
            subtotal = 0.0,
            impuestos = 0.0,
            total = 0.0
        )
        assertEquals(789, orderFJ.numericId)

        // Órdenes con IDs alfanuméricos no canónicos deben degradar a 0 de forma segura sin excepciones
        val orderLegacy = OrderRecord(
            id = "LEGACY-ALPHA",
            fecha = "14/09/2026",
            estado = OrderStatus.ENTREGADO,
            items = emptyList(),
            subtotal = 0.0,
            impuestos = 0.0,
            total = 0.0
        )
        assertEquals(0, orderLegacy.numericId)
    }

    @Test
    fun testBoundaryConditionsZeroDiscountAndEmptyCartBlastRadius() {
        val emptyOrder = OrderRecord(
            id = "FJ-000",
            fecha = "14/09/2026",
            estado = OrderStatus.PENDIENTE,
            items = emptyList(),
            subtotal = 0.0,
            impuestos = 0.0,
            envio = 0.0,
            total = 0.0
        )
        assertTrue(emptyOrder.items.isEmpty())
        assertEquals(0.0, emptyOrder.total, 0.001)

        // Producto con descuento 100% (borde superior)
        val freeSample = ProductItem(
            id = 100,
            nombre = "Muestra Gratis",
            descripcion = "Degustación",
            precio = 10.0,
            tipoComida = "Snacks",
            imagenUrl = "https://img/free.png",
            descuentoEstudiante = 100.0
        )
        assertEquals(0.0, freeSample.getEffectivePrice(isStudent = true), 0.001)
        assertEquals(10.0, freeSample.getEffectivePrice(isStudent = false), 0.001)
    }

    @Test
    fun testOrderStatusCountAndValuesStability() {
        // Garantizar que la máquina de estados contenga exactamente los 6 estados canónicos
        val states = OrderStatus.values()
        assertEquals("OrderStatus debe contar con exactamente 6 estados", 6, states.size)
        val stateNames = states.map { it.name }.toSet()
        assertTrue(stateNames.contains("PENDIENTE"))
        assertTrue(stateNames.contains("CONFIRMADO"))
        assertTrue(stateNames.contains("EN_PREPARACION"))
        assertTrue(stateNames.contains("EN_CAMINO"))
        assertTrue(stateNames.contains("ENTREGADO"))
        assertTrue(stateNames.contains("CANCELADO"))
    }
}
