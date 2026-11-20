package com.example.foodjeetapp.data

import com.example.foodjeetapp.data.common.UiState
import com.example.foodjeetapp.data.model.DeliverySelection
import com.example.foodjeetapp.data.model.OrderRecord
import com.example.foodjeetapp.data.model.PaginacionCatalogo
import com.example.foodjeetapp.data.model.ProductItem
import com.example.foodjeetapp.data.model.SavedAddress
import com.example.foodjeetapp.data.model.formatearMonto
import com.example.foodjeetapp.data.model.formatearSoles
import com.example.foodjeetapp.data.remote.dto.AdminOrderDto
import com.example.foodjeetapp.data.remote.dto.ConfigResponseDto
import com.example.foodjeetapp.data.remote.dto.OrderItemDetailDto
import com.example.foodjeetapp.data.remote.dto.OrderResponseDto
import com.example.foodjeetapp.data.remote.dto.ProductDto
import com.example.foodjeetapp.data.remote.dto.RestaurantDto
import com.example.foodjeetapp.data.remote.dto.TransactionDto
import com.example.foodjeetapp.data.repository.OrderRepository
import com.example.foodjeetapp.ui.viewmodel.CartViewModel
import com.example.foodjeetapp.ui.viewmodel.OrderViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Pruebas unitarias para las correcciones de pre-examen:
 * 1. Consistencia financiera en OrderResponseDto.toDomain() (precioUnitario + descuento por cupón).
 * 2. Guardia de restaurante único en CartViewModel.
 * 3. Sin peticiones no autenticadas en OrderViewModel.init y limpiar().
 * 4. Formato de moneda independiente del Locale del dispositivo.
 */
class PreExamenFixesUnitTest {

    @Test
    fun `OrderResponseDto usa precio_unitario efectivo con descuento de estudiante`() {
        val dto = OrderResponseDto(
            id = 9,
            userId = 2,
            restauranteId = 1,
            direccionEntregaId = 1,
            total = 14.03,
            impuestos = 1.38,
            costoEnvio = 5.00,
            estado = "pendiente",
            fecha = "2026-11-20T18:00:00.000Z",
            restaurant = RestaurantDto(id = 1, nombre = "Burger King", direccion = "Av. Arequipa 123"),
            review = null,
            transaction = TransactionDto(id = 1, metodoPago = "cash", monto = 14.03, estadoPago = "pendiente"),
            orderItems = listOf(
                OrderItemDetailDto(
                    id = 1,
                    productId = 1,
                    cantidad = 1,
                    precioUnitario = 7.65,
                    product = ProductDto(
                        id = 1,
                        nombre = "Whopper Clásica",
                        descripcion = "Hamburguesa",
                        precio = 8.50,
                        descuentoEstudiante = 10.0,
                        tipoComida = "Comida rápida",
                        imagenUrl = "",
                        disponibilidad = true,
                        restauranteId = 1,
                        restaurant = null
                    )
                )
            )
        )

        val domain = dto.toDomain()
        assertEquals(7.65, domain.items.first().product.precio, 0.001)
        assertEquals(7.65, domain.subtotal, 0.001)
        assertEquals(0.0, domain.descuento, 0.001)
        assertEquals(1.38, domain.impuestos, 0.001)
        assertEquals(5.00, domain.envio, 0.001)
        assertEquals(14.03, domain.subtotal - domain.descuento + domain.impuestos + domain.envio, 0.01)
        assertEquals(14.03, domain.total, 0.001)
    }

    @Test
    fun `OrderResponseDto calcula descuento de cupon coherentemente`() {
        // Subtotal 20.00, cupón 20% (-4.00) => neto 16.00, IGV 2.88, envío 5.00 => total 23.88
        val dto = OrderResponseDto(
            id = 10,
            userId = 2,
            restauranteId = 2,
            direccionEntregaId = 1,
            total = 23.88,
            impuestos = 2.88,
            costoEnvio = 5.00,
            estado = "confirmado",
            fecha = "2026-11-20T18:10:00.000Z",
            restaurant = RestaurantDto(id = 2, nombre = "Pizza Hut", direccion = "Av. Larco 456"),
            review = null,
            transaction = TransactionDto(id = 2, metodoPago = "card", monto = 23.88, estadoPago = "completado"),
            orderItems = listOf(
                OrderItemDetailDto(
                    id = 2,
                    productId = 3,
                    cantidad = 2,
                    precioUnitario = 10.00,
                    product = ProductDto(
                        id = 3,
                        nombre = "Pizza Pepperoni",
                        descripcion = "Pizza",
                        precio = 10.00,
                        descuentoEstudiante = 0.0,
                        tipoComida = "Pizzas",
                        imagenUrl = "",
                        disponibilidad = true,
                        restauranteId = 2,
                        restaurant = null
                    )
                )
            )
        )

        val domain = dto.toDomain()
        assertEquals(20.00, domain.subtotal, 0.001)
        assertEquals(4.00, domain.descuento, 0.01)
        assertEquals(domain.total, domain.subtotal - domain.descuento + domain.impuestos + domain.envio, 0.01)
    }

    @Test
    fun `CartViewModel detecta cambio de restaurante y permite reemplazar carrito`() {
        val cartVm = CartViewModel()
        val burger = ProductItem(
            id = 1,
            nombre = "Whopper Clásica",
            descripcion = "Burger",
            precio = 15.0,
            tipoComida = "Comida rápida",
            imagenUrl = "",
            restauranteId = 1,
            restauranteNombre = "Burger King"
        )
        val papas = burger.copy(id = 2, nombre = "Papas Fritas", precio = 8.0)
        val pizza = ProductItem(
            id = 3,
            nombre = "Pizza Pepperoni",
            descripcion = "Pizza",
            precio = 25.0,
            tipoComida = "Pizzas",
            imagenUrl = "",
            restauranteId = 2,
            restauranteNombre = "Pizza Hut"
        )

        assertFalse(cartVm.esDeOtroRestaurante(burger))
        assertNull(cartVm.restauranteActualNombre())

        cartVm.addToCart(burger)
        assertFalse(cartVm.esDeOtroRestaurante(papas))
        assertTrue(cartVm.esDeOtroRestaurante(pizza))
        assertEquals("Burger King", cartVm.restauranteActualNombre())

        cartVm.reemplazarConProducto(pizza)
        assertEquals(1, cartVm.cartItems.value.size)
        assertEquals(pizza.id, cartVm.cartItems.value.first().product.id)
        assertEquals("Pizza Hut", cartVm.restauranteActualNombre())
        assertFalse(cartVm.esDeOtroRestaurante(pizza))
        assertTrue(cartVm.esDeOtroRestaurante(burger))
    }

    @Test
    fun `OrderViewModel no consulta endpoints protegidos en init ni al limpiar`() {
        var getOrdersCalls = 0
        var getAddressesCalls = 0
        val fakeRepo = object : OrderRepository {
            override fun getOrdersStream(): Flow<List<OrderRecord>> = emptyFlow()
            override suspend fun getOrders(): Result<List<OrderRecord>> {
                getOrdersCalls++
                return Result.success(emptyList())
            }
            override suspend fun getOrderById(orderId: Int): Result<OrderRecord> = Result.failure(UnsupportedOperationException())
            override suspend fun createOrder(
                order: OrderRecord,
                entrega: DeliverySelection,
                restauranteId: Int,
                metodoPago: String,
                cuponId: Int?
            ): Result<OrderRecord> = Result.failure(UnsupportedOperationException())
            override suspend fun cancelOrder(orderId: Int): Result<Unit> = Result.success(Unit)
            override suspend fun getAllAdminOrders(): Result<List<AdminOrderDto>> = Result.success(emptyList())
            override suspend fun advanceOrderStatus(orderId: Int, nextStatus: String): Result<Unit> = Result.success(Unit)
            override suspend fun updateOrderReview(orderId: String, stars: Int, comment: String): Result<Unit> = Result.success(Unit)
            override suspend fun getPublicConfig(): Result<ConfigResponseDto> = Result.failure(UnsupportedOperationException())
            override suspend fun getRestaurants(): Result<List<RestaurantDto>> = Result.success(emptyList())
            override suspend fun getRestaurant(id: Int): Result<RestaurantDto> = Result.failure(UnsupportedOperationException())
            override suspend fun getAddresses(): Result<List<SavedAddress>> {
                getAddressesCalls++
                return Result.success(emptyList())
            }
        }

        val vm = OrderViewModel(fakeRepo)
        assertEquals(UiState.Empty, vm.ordersState.value)
        assertEquals(UiState.Empty, vm.addressesState.value)
        assertEquals(UiState.Empty, vm.adminOrdersState.value)
        assertEquals(0, getOrdersCalls)
        assertEquals(0, getAddressesCalls)

        vm.limpiar()
        assertEquals(UiState.Empty, vm.ordersState.value)
        assertEquals(UiState.Empty, vm.addressesState.value)
        assertEquals(0, getOrdersCalls)
        assertEquals(0, getAddressesCalls)
    }

    @Test
    fun `formatearSoles y formatearMonto siempre usan punto decimal sin importar Locale`() {
        val original = Locale.getDefault()
        try {
            listOf(Locale.US, Locale.GERMANY, Locale("es", "PE")).forEach { loc ->
                Locale.setDefault(loc)
                assertEquals("14.03", formatearMonto(14.03))
                assertEquals("S/ 14.03", formatearSoles(14.03))
                assertEquals("S/ 5.00", formatearSoles(5.0))
            }
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `PaginacionCatalogo restringe tamano entre 6 y 16 y pagina 106 productos correctamente`() {
        assertEquals(6, PaginacionCatalogo.sanitizarTamanoPagina(1))
        assertEquals(6, PaginacionCatalogo.sanitizarTamanoPagina(6))
        assertEquals(12, PaginacionCatalogo.sanitizarTamanoPagina(12))
        assertEquals(16, PaginacionCatalogo.sanitizarTamanoPagina(16))
        assertEquals(16, PaginacionCatalogo.sanitizarTamanoPagina(50))

        val items = (1..106).toList()

        val paginaMax = PaginacionCatalogo.paginar(items, paginaSolicitada = 1, porPagina = 25)
        assertEquals(16, paginaMax.tamanoPagina)
        assertEquals(7, paginaMax.totalPaginas)
        assertEquals(16, paginaMax.elementos.size)
        assertEquals(1, paginaMax.indiceInicio)
        assertEquals(16, paginaMax.indiceFin)

        val ultimaMax = PaginacionCatalogo.paginar(items, paginaSolicitada = 99, porPagina = 16)
        assertEquals(7, ultimaMax.paginaActual)
        assertEquals(10, ultimaMax.elementos.size)
        assertEquals(97, ultimaMax.indiceInicio)
        assertEquals(106, ultimaMax.indiceFin)

        val paginaMin = PaginacionCatalogo.paginar(items, paginaSolicitada = 2, porPagina = 3)
        assertEquals(6, paginaMin.tamanoPagina)
        assertEquals(18, paginaMin.totalPaginas)
        assertEquals(listOf(7, 8, 9, 10, 11, 12), paginaMin.elementos)

        val vacia = PaginacionCatalogo.paginar(emptyList<Int>(), paginaSolicitada = 3, porPagina = 12)
        assertEquals(1, vacia.paginaActual)
        assertEquals(1, vacia.totalPaginas)
        assertTrue(vacia.elementos.isEmpty())
        assertEquals(0, vacia.indiceInicio)
        assertEquals(0, vacia.indiceFin)

        assertEquals(listOf(1, 2, 3, 4, 5), PaginacionCatalogo.paginasVisibles(1, 9))
        assertEquals(listOf(3, 4, 5, 6, 7), PaginacionCatalogo.paginasVisibles(5, 9))
        assertEquals(listOf(5, 6, 7, 8, 9), PaginacionCatalogo.paginasVisibles(9, 9))
        assertEquals(listOf(1, 2, 3), PaginacionCatalogo.paginasVisibles(2, 3))
    }
}
