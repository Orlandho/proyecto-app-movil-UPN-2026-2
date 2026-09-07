package com.example.foodjeetapp.data.repository

import com.example.foodjeetapp.data.model.OrderRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Contrato de repositorio para la gestión de pedidos de FoodJet.
 * Cumple con REQ-SEM06-LOG-03 (Patrón Repository).
 */
interface OrderRepository {
    fun getOrdersStream(): Flow<List<OrderRecord>>
    suspend fun getOrders(): Result<List<OrderRecord>>
    suspend fun createOrder(order: OrderRecord): Result<OrderRecord>
    suspend fun updateOrderReview(orderId: String, stars: Int, comment: String): Result<Unit>
}

/**
 * Implementación de producción para el repositorio de pedidos.
 * Inicialmente emite una lista vacía real a la espera de la base de datos local Room y Cloud Firestore.
 */
class OrderRepositoryImpl : OrderRepository {

    // TODO: (REQ-SEM05-LOG-01 / REQ-SEM05-INF-02) Inyectar OrderDao de Room ORM para inserción, consulta y actualización de pedidos locales.
    // TODO: (REQ-SEM09-LOG-02) Integrar Cloud Firestore para base de datos documental en tiempo real con sincronización bidireccional.
    // TODO: (REQ-SEM08-LOG-03) Ejecutar consultas y transacciones en segundo plano con Dispatchers.IO.

    private val _ordersFlow = MutableStateFlow<List<OrderRecord>>(emptyList())

    override fun getOrdersStream(): Flow<List<OrderRecord>> {
        // En producción se consumirá el flujo reactivo de Room: orderDao.getOrdersFlow()
        return _ordersFlow.asStateFlow()
    }

    override suspend fun getOrders(): Result<List<OrderRecord>> {
        // TODO: Leer pedidos desde Room y sincronizar con Firestore:
        // val localOrders = orderDao.getAllOrders()
        // return Result.success(localOrders.toDomainList())
        return Result.success(_ordersFlow.value)
    }

    override suspend fun createOrder(order: OrderRecord): Result<OrderRecord> {
        // TODO: Guardar en Room (orderDao.insertOrder(order.toEntity()))
        // TODO: Enviar documento a colección 'orders' en Cloud Firestore.
        _ordersFlow.update { listOf(order) + it }
        return Result.success(order)
    }

    override suspend fun updateOrderReview(orderId: String, stars: Int, comment: String): Result<Unit> {
        // TODO: Actualizar campos reviewStars y reviewComment en Room y Firestore.
        _ordersFlow.update { current ->
            current.map {
                if (it.id == orderId) it.copy(reviewStars = stars, reviewComment = comment) else it
            }
        }
        return Result.success(Unit)
    }
}
