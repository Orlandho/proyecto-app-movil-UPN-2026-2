package com.example.foodjeetapp.data.repository

import com.example.foodjeetapp.data.model.OrderRecord
import com.example.foodjeetapp.data.model.OrderStatus
import com.example.foodjeetapp.data.remote.api.FoodJetApiService
import com.example.foodjeetapp.data.remote.dto.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Contrato de repositorio para la gestión de pedidos de FoodJet.
 * Cumple con REQ-SEM06-LOG-03 (Patrón Repository).
 */
interface OrderRepository {
    fun getOrdersStream(): Flow<List<OrderRecord>>
    suspend fun getOrders(): Result<List<OrderRecord>>
    suspend fun getOrderById(orderId: Int): Result<OrderRecord>
    suspend fun createOrder(
        order: OrderRecord,
        restauranteId: Int = 1,
        metodoPago: String = "cash",
        cuponId: Int? = null
    ): Result<OrderRecord>
    suspend fun cancelOrder(orderId: Int): Result<Unit>
    suspend fun getAllAdminOrders(): Result<List<AdminOrderDto>>
    suspend fun advanceOrderStatus(orderId: Int, nextStatus: String): Result<Unit>
    suspend fun updateOrderReview(orderId: String, stars: Int, comment: String): Result<Unit>
}

/**
 * Implementación de producción para el repositorio de pedidos.
 * Conectado en tiempo real a los endpoints de órdenes, direcciones y reseñas del backend Node.js.
 * Cumple con REQ-SEM08-LOG-01, REQ-SEM08-LOG-02 y REQ-SEM08-LOG-03.
 */
class OrderRepositoryImpl(
    private val apiService: FoodJetApiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : OrderRepository {

    private val _ordersFlow = MutableStateFlow<List<OrderRecord>>(emptyList())

    override fun getOrdersStream(): Flow<List<OrderRecord>> = _ordersFlow.asStateFlow()

    override suspend fun getOrders(): Result<List<OrderRecord>> = withContext(ioDispatcher) {
        try {
            val response = apiService.getMyOrders()
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                val domainOrders = dtos.map { it.toDomain() }
                _ordersFlow.value = domainOrders
                Result.success(domainOrders)
            } else {
                Result.failure(Exception("Error al consultar pedidos (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión al cargar pedidos: ${e.localizedMessage}", e))
        }
    }

    override suspend fun getOrderById(orderId: Int): Result<OrderRecord> = withContext(ioDispatcher) {
        try {
            val response = apiService.getOrderById(orderId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception("Error al consultar pedido ($orderId): ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión al consultar pedido: ${e.localizedMessage}", e))
        }
    }

    override suspend fun createOrder(
        order: OrderRecord,
        restauranteId: Int,
        metodoPago: String,
        cuponId: Int?
    ): Result<OrderRecord> = withContext(ioDispatcher) {
        try {
            // 1. Obtener dirección de entrega válida del usuario
            var addressId = 1
            val addressesResponse = apiService.getAddresses()
            if (addressesResponse.isSuccessful && !addressesResponse.body().isNullOrEmpty()) {
                val defaultAddress = addressesResponse.body()!!.firstOrNull { it.esPredeterminada == true }
                    ?: addressesResponse.body()!!.first()
                addressId = defaultAddress.id
            } else {
                // Si el usuario aún no tiene dirección registrada en PostgreSQL, crear la primera
                val newAddressResponse = apiService.createAddress(
                    CreateAddressRequestDto(
                        direccionDetallada = "Av. Principal 123, Miraflores, Lima",
                        referencia = "Cerca al parque",
                        esPredeterminada = true
                    )
                )
                if (newAddressResponse.isSuccessful && newAddressResponse.body() != null) {
                    addressId = newAddressResponse.body()!!.id
                }
            }

            // 2. Mapear items del carrito al formato esperado por el backend
            val orderItemsDto = order.items.map { cartItem ->
                OrderItemRequestDto(
                    productId = cartItem.product.id,
                    cantidad = cartItem.quantity
                )
            }

            val validRestId = order.items.firstOrNull()?.product?.restauranteId ?: restauranteId

            // Mapeo canónico estricto requerido por el backend: cash, card, wallet
            val canonicalMethod = when (metodoPago.lowercase().trim()) {
                "card", "tarjeta" -> "card"
                "wallet", "billetera", "billetera digital", "yape", "plin" -> "wallet"
                else -> "cash"
            }

            val request = CreateOrderRequestDto(
                restauranteId = validRestId,
                direccionEntregaId = addressId,
                cuponId = cuponId,
                metodoPago = canonicalMethod,
                items = orderItemsDto
            )

            // 3. Crear orden en backend Node.js / PostgreSQL
            val response = apiService.createOrder(request)
            if (response.isSuccessful && response.body() != null) {
                val createdId = response.body()!!.order?.id ?: 1
                val freshOrder = getOrderById(createdId).getOrNull() ?: order.copy(
                    id = "FJ-$createdId",
                    numericId = createdId,
                    estado = if (canonicalMethod == "cash") OrderStatus.PENDIENTE else OrderStatus.CONFIRMADO,
                    total = response.body()!!.order?.total ?: order.total
                )
                // Refrescar el flujo de órdenes con los datos frescos del servidor
                getOrders()
                Result.success(freshOrder)
            } else {
                Result.failure(Exception("Error al procesar el pedido (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Falla de red al enviar el pedido: ${e.localizedMessage}", e))
        }
    }

    override suspend fun cancelOrder(orderId: Int): Result<Unit> = withContext(ioDispatcher) {
        try {
            val response = apiService.cancelOrder(orderId)
            if (response.isSuccessful) {
                getOrders()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al cancelar pedido (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de red al cancelar pedido: ${e.localizedMessage}", e))
        }
    }

    override suspend fun getAllAdminOrders(): Result<List<AdminOrderDto>> = withContext(ioDispatcher) {
        try {
            val response = apiService.getAllOrders()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al consultar pedidos de administración (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de red: ${e.localizedMessage}", e))
        }
    }

    override suspend fun advanceOrderStatus(orderId: Int, nextStatus: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val response = apiService.updateOrderStatus(orderId, UpdateOrderStatusRequestDto(nextStatus))
            if (response.isSuccessful) {
                getOrders()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al actualizar estado (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.localizedMessage}", e))
        }
    }

    override suspend fun updateOrderReview(
        orderId: String,
        stars: Int,
        comment: String
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            val numericId = orderId.removePrefix("FJ-").toIntOrNull() ?: 1
            val response = apiService.createReview(
                CreateReviewRequestDto(
                    pedidoId = numericId,
                    puntuacion = stars,
                    comentario = comment
                )
            )
            if (response.isSuccessful) {
                getOrders() // Refrescar para reflejar la calificación
                Result.success(Unit)
            } else {
                Result.failure(Exception("No se pudo registrar la calificación (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Falla de conexión al enviar la reseña: ${e.localizedMessage}", e))
        }
    }
}
