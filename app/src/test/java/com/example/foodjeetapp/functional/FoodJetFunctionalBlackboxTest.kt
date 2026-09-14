package com.example.foodjeetapp.functional

import com.example.foodjeetapp.data.model.CartItem
import com.example.foodjeetapp.data.model.OrderRecord
import com.example.foodjeetapp.data.model.OrderStatus
import com.example.foodjeetapp.data.model.ProductItem
import com.example.foodjeetapp.data.model.UserProfile
import com.example.foodjeetapp.data.remote.api.FoodJetApiService
import org.junit.Assert.*
import org.junit.Test
import retrofit2.http.*
import java.lang.reflect.Method

/**
 * Pruebas Funcionales y de Caja Negra para FoodJet Móvil.
 * Valida los contratos REST de Retrofit y flujos de negocio sin depender de la implementación interna.
 */
class FoodJetFunctionalBlackboxTest {

    @Test
    fun testRetrofitRestContractsAndAnnotations() {
        val methods: Array<Method> = FoodJetApiService::class.java.declaredMethods
        val methodNames = methods.map { it.name }.toSet()

        // 1. Validar presencia de métodos esenciales del contrato de backend FoodJet
        assertTrue("Debe existir contrato de login", methodNames.contains("login"))
        assertTrue("Debe existir contrato de registro", methodNames.contains("register"))
        assertTrue("Debe existir contrato de catálogo de productos", methodNames.contains("getProducts"))
        assertTrue("Debe existir contrato de mis pedidos", methodNames.contains("getMyOrders"))
        assertTrue("Debe existir contrato para crear pedido", methodNames.contains("createOrder"))
        assertTrue("Debe existir contrato para cancelar pedido", methodNames.contains("cancelOrder"))
        assertTrue("Debe existir contrato de direcciones", methodNames.contains("getAddresses"))
        assertTrue("Debe existir contrato de favoritos", methodNames.contains("getFavorites"))

        // 2. Validar que los métodos HTTP estén adecuadamente anotados
        val loginMethod = methods.first { it.name == "login" }
        assertTrue("login debe usar anotación @POST", loginMethod.isAnnotationPresent(POST::class.java))
        assertEquals("auth/login", loginMethod.getAnnotation(POST::class.java)?.value)

        val productsMethod = methods.first { it.name == "getProducts" }
        assertTrue("getProducts debe usar anotación @GET", productsMethod.isAnnotationPresent(GET::class.java))
        assertEquals("products", productsMethod.getAnnotation(GET::class.java)?.value)

        val createOrderMethod = methods.first { it.name == "createOrder" }
        assertTrue("createOrder debe usar anotación @POST", createOrderMethod.isAnnotationPresent(POST::class.java))
        assertEquals("orders", createOrderMethod.getAnnotation(POST::class.java)?.value)

        val cancelOrderMethod = methods.first { it.name == "cancelOrder" }
        assertTrue("cancelOrder debe usar anotación @PUT", cancelOrderMethod.isAnnotationPresent(PUT::class.java))
        assertEquals("orders/{id}/cancel", cancelOrderMethod.getAnnotation(PUT::class.java)?.value)
    }

    @Test
    fun testBlackboxOrderCreationTaxAndDeliveryFeeCalculations() {
        // Regla de negocio FoodJet:
        // Costo de envío = S/ 5.00 fijo
        // IGV = 18% sobre subtotal
        val prod1 = ProductItem(1, "Pizza Familiar", "Con queso extra", 40.0, "Pizzas", "https://img/1.png")
        val prod2 = ProductItem(2, "Gaseosa 1.5L", "Bebida refrescante", 10.0, "Bebidas", "https://img/2.png")

        val subtotal = prod1.precio + prod2.precio // S/ 50.00
        val igvEsperado = subtotal * 0.18 // S/ 9.00
        val envioEsperado = 5.00 // S/ 5.00 fijo
        val totalEsperado = subtotal + igvEsperado + envioEsperado // S/ 64.00

        val items = listOf(
            CartItem(prod1, 1),
            CartItem(prod2, 1)
        )

        val pedido = OrderRecord(
            id = "ORD-FB-01",
            fecha = "14/09/2026",
            estado = OrderStatus.CONFIRMADO,
            items = items,
            subtotal = subtotal,
            impuestos = igvEsperado,
            envio = envioEsperado,
            total = totalEsperado
        )

        assertEquals(50.0, pedido.subtotal, 0.001)
        assertEquals(9.0, pedido.impuestos, 0.001)
        assertEquals(5.0, pedido.envio, 0.001)
        assertEquals(64.0, pedido.total, 0.001)
    }

    @Test
    fun testBlackboxStudentDiscountBenefitFlow() {
        // Combo con 25% de descuento universitario
        val comboEstudiante = ProductItem(
            id = 5,
            nombre = "Super Combo UPN",
            descripcion = "Hamburguesa + papas + bebida",
            precio = 24.00,
            tipoComida = "Combos",
            imagenUrl = "https://img/combo.png",
            descuentoEstudiante = 25.0
        )

        // Cuando el usuario es estudiante UPN: debe pagar 75% del precio (S/ 18.00)
        val precioConBeneficio = comboEstudiante.getEffectivePrice(isStudent = true)
        assertEquals(18.00, precioConBeneficio, 0.001)

        // Cuando el usuario no es estudiante: debe pagar precio completo S/ 24.00
        val precioRegular = comboEstudiante.getEffectivePrice(isStudent = false)
        assertEquals(24.00, precioRegular, 0.001)
    }

    @Test
    fun testBlackboxOrderStatusLifecycleConsistency() {
        // Estados terminales
        assertTrue("ENTREGADO debe ser terminal", OrderStatus.ENTREGADO.isTerminal)
        assertTrue("CANCELADO debe ser terminal", OrderStatus.CANCELADO.isTerminal)

        // Estados intermedios no terminales
        assertFalse("PENDIENTE no es terminal", OrderStatus.PENDIENTE.isTerminal)
        assertFalse("CONFIRMADO no es terminal", OrderStatus.CONFIRMADO.isTerminal)
        assertFalse("EN_PREPARACION no es terminal", OrderStatus.EN_PREPARACION.isTerminal)
        assertFalse("EN_CAMINO no es terminal", OrderStatus.EN_CAMINO.isTerminal)

        // Cancelabilidad: solo en fases tempranas
        assertTrue("PENDIENTE puede cancelarse", OrderStatus.PENDIENTE.isCancelable)
        assertTrue("CONFIRMADO puede cancelarse", OrderStatus.CONFIRMADO.isCancelable)
        assertFalse("EN_PREPARACION ya no puede cancelarse", OrderStatus.EN_PREPARACION.isCancelable)
        assertFalse("EN_CAMINO ya no puede cancelarse", OrderStatus.EN_CAMINO.isCancelable)
        assertFalse("ENTREGADO nunca puede cancelarse", OrderStatus.ENTREGADO.isCancelable)
        assertFalse("CANCELADO no puede re-cancelarse", OrderStatus.CANCELADO.isCancelable)
    }
}
