package com.example.foodjeetapp.data.repository

import com.example.foodjeetapp.data.model.DeliverySelection
import com.example.foodjeetapp.data.model.Geo
import com.example.foodjeetapp.data.model.OrderRecord
import com.example.foodjeetapp.data.model.SavedAddress
import com.example.foodjeetapp.data.remote.ErroresApi
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
        entrega: DeliverySelection,
        restauranteId: Int,
        metodoPago: String = "cash",
        cuponId: Int? = null
    ): Result<OrderRecord>
    suspend fun cancelOrder(orderId: Int): Result<Unit>
    suspend fun getAllAdminOrders(): Result<List<AdminOrderDto>>
    suspend fun advanceOrderStatus(orderId: Int, nextStatus: String): Result<Unit>
    suspend fun updateOrderReview(orderId: String, stars: Int, comment: String): Result<Unit>
    suspend fun getPublicConfig(): Result<ConfigResponseDto>
    suspend fun getRestaurants(): Result<List<RestaurantDto>>
    suspend fun getRestaurant(id: Int): Result<RestaurantDto>
    suspend fun getAddresses(): Result<List<SavedAddress>>
}

/**
 * Busca entre las direcciones guardadas una equivalente a la elegida: mismo
 * texto y, si se marcó un punto, a menos de [Geo.MISMA_UBICACION_M]. Es la
 * misma regla que `buscarDireccionGuardada` de la web: si el pin se movió se
 * crea otra dirección en vez de corregir la vieja, porque los pedidos
 * anteriores apuntan a ella y su historial debe seguir diciendo dónde se
 * entregaron.
 */
internal fun buscarDireccionGuardada(guardadas: List<SavedAddress>, entrega: DeliverySelection): SavedAddress? {
    val normalizar = { texto: String -> texto.trim().replace(Regex("\\s+"), " ").lowercase() }
    val buscado = normalizar(entrega.direccion)
    if (buscado.isEmpty()) return null
    val marcado = entrega.punto
    return guardadas.firstOrNull { guardada ->
        if (normalizar(guardada.direccion) != buscado) return@firstOrNull false
        if (marcado == null) return@firstOrNull true
        val punto = guardada.punto
        punto != null && Geo.distanciaMetros(punto, marcado) <= Geo.MISMA_UBICACION_M
    }
}

/**
 * Implementación de producción para el repositorio de pedidos.
 * Conectado en tiempo real a los endpoints de órdenes, direcciones y reseñas del backend Node.js.
 * Cumple con REQ-SEM08-LOG-01, REQ-SEM08-LOG-02, REQ-SEM08-LOG-03 y REQ-SEM08-LOG-04.
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
            val dtos = response.body()
            if (response.isSuccessful && dtos != null) {
                val domainOrders = dtos.map { it.toDomain() }
                _ordersFlow.value = domainOrders
                Result.success(domainOrders)
            } else {
                Result.failure(Exception(ErroresApi.mensaje(response, "consultar tus pedidos")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErroresApi.mensajeDeRed(e, "cargar tus pedidos"), e))
        }
    }

    override suspend fun getOrderById(orderId: Int): Result<OrderRecord> = withContext(ioDispatcher) {
        try {
            val response = apiService.getOrderById(orderId)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.toDomain())
            } else {
                Result.failure(Exception(ErroresApi.mensaje(response, "consultar el pedido #$orderId")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErroresApi.mensajeDeRed(e, "consultar el pedido"), e))
        }
    }

    override suspend fun createOrder(
        order: OrderRecord,
        entrega: DeliverySelection,
        restauranteId: Int,
        metodoPago: String,
        cuponId: Int?
    ): Result<OrderRecord> = withContext(ioDispatcher) {
        try {
            // 1. Dirección del propio cliente: la elegida, una guardada
            //    equivalente o una nueva con el punto marcado en el mapa.
            val direccionId = resolverDireccion(entrega).getOrElse { return@withContext Result.failure(it) }

            // 2. Mapeo canónico estricto requerido por el backend: cash, card, wallet
            val canonicalMethod = when (metodoPago.lowercase().trim()) {
                "card", "tarjeta" -> "card"
                "wallet", "billetera", "billetera digital", "yape", "plin" -> "wallet"
                else -> "cash"
            }

            val request = CreateOrderRequestDto(
                restauranteId = restauranteId,
                direccionEntregaId = direccionId,
                cuponId = cuponId,
                metodoPago = canonicalMethod,
                items = order.items.map { OrderItemRequestDto(productId = it.product.id, cantidad = it.quantity) }
            )

            // 3. El backend recalcula precios, descuento, IGV y envío: el
            //    pedido que se muestra es el que devuelve, no el del carrito.
            val response = apiService.createOrder(request)
            val createdId = response.body()?.order?.id
            if (!response.isSuccessful || createdId == null) {
                return@withContext Result.failure(Exception(ErroresApi.mensaje(response, "registrar el pedido")))
            }
            val freshOrder = getOrderById(createdId).getOrElse { return@withContext Result.failure(it) }
            getOrders()
            Result.success(freshOrder)
        } catch (e: Exception) {
            Result.failure(Exception(ErroresApi.mensajeDeRed(e, "enviar el pedido"), e))
        }
    }

    private suspend fun resolverDireccion(entrega: DeliverySelection): Result<Int> {
        entrega.direccionGuardadaId?.let { return Result.success(it) }

        val guardadas = getAddresses().getOrElse { return Result.failure(it) }
        buscarDireccionGuardada(guardadas, entrega)?.let { return Result.success(it.id) }

        val response = apiService.createAddress(
            CreateAddressRequestDto(
                direccionDetallada = entrega.direccion.trim(),
                referencia = entrega.referencia?.trim()?.takeIf { it.isNotEmpty() },
                latitud = entrega.punto?.latitud,
                longitud = entrega.punto?.longitud,
                esPredeterminada = guardadas.isEmpty()
            )
        )
        val creada = response.body()
        return if (response.isSuccessful && creada != null) {
            Result.success(creada.id)
        } else {
            Result.failure(Exception(ErroresApi.mensaje(response, "guardar la dirección de entrega")))
        }
    }

    override suspend fun getAddresses(): Result<List<SavedAddress>> = withContext(ioDispatcher) {
        try {
            val response = apiService.getAddresses()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body.map { it.toDomain() })
            } else {
                Result.failure(Exception(ErroresApi.mensaje(response, "consultar tus direcciones")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErroresApi.mensajeDeRed(e, "consultar tus direcciones"), e))
        }
    }

    override suspend fun cancelOrder(orderId: Int): Result<Unit> = withContext(ioDispatcher) {
        try {
            val response = apiService.cancelOrder(orderId)
            if (response.isSuccessful) {
                getOrders()
                Result.success(Unit)
            } else {
                Result.failure(Exception(ErroresApi.mensaje(response, "cancelar el pedido")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErroresApi.mensajeDeRed(e, "cancelar el pedido"), e))
        }
    }

    override suspend fun getAllAdminOrders(): Result<List<AdminOrderDto>> = withContext(ioDispatcher) {
        try {
            val response = apiService.getAllOrders()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception(ErroresApi.mensaje(response, "consultar los pedidos del sistema")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErroresApi.mensajeDeRed(e, "consultar los pedidos del sistema"), e))
        }
    }

    override suspend fun advanceOrderStatus(orderId: Int, nextStatus: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val response = apiService.updateOrderStatus(orderId, UpdateOrderStatusRequestDto(nextStatus))
            if (response.isSuccessful) {
                getOrders()
                Result.success(Unit)
            } else {
                Result.failure(Exception(ErroresApi.mensaje(response, "actualizar el estado")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErroresApi.mensajeDeRed(e, "actualizar el estado"), e))
        }
    }

    override suspend fun updateOrderReview(
        orderId: String,
        stars: Int,
        comment: String
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            val numericId = orderId.removePrefix("FJ-").toIntOrNull()
                ?: return@withContext Result.failure(IllegalArgumentException("Pedido inválido: $orderId"))
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
                Result.failure(Exception(ErroresApi.mensaje(response, "registrar la calificación")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErroresApi.mensajeDeRed(e, "enviar la reseña"), e))
        }
    }

    override suspend fun getPublicConfig(): Result<ConfigResponseDto> = withContext(ioDispatcher) {
        try {
            val response = apiService.getConfig()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception(ErroresApi.mensaje(response, "obtener la configuración")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErroresApi.mensajeDeRed(e, "obtener la configuración"), e))
        }
    }

    override suspend fun getRestaurants(): Result<List<RestaurantDto>> = withContext(ioDispatcher) {
        try {
            val response = apiService.getRestaurants()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception(ErroresApi.mensaje(response, "consultar los locales")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErroresApi.mensajeDeRed(e, "consultar los locales"), e))
        }
    }

    override suspend fun getRestaurant(id: Int): Result<RestaurantDto> = withContext(ioDispatcher) {
        try {
            val response = apiService.getRestaurantById(id)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception(ErroresApi.mensaje(response, "consultar el local")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErroresApi.mensajeDeRed(e, "consultar el local"), e))
        }
    }
}
