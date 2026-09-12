package com.example.foodjeetapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodjeetapp.data.common.UiState
import com.example.foodjeetapp.data.di.ServiceLocator
import com.example.foodjeetapp.data.model.OrderRecord
import com.example.foodjeetapp.data.remote.dto.AdminOrderDto
import com.example.foodjeetapp.data.repository.OrderRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel para la gestión de historial, creación y seguimiento en tiempo real de pedidos.
 * Cumple con REQ-SEM06-LOG-01, REQ-SEM06-LOG-02 y REQ-SEM06-LOG-04.
 */
class OrderViewModel(
    private val orderRepository: OrderRepository = ServiceLocator.orderRepository
) : ViewModel() {

    private val _ordersState = MutableStateFlow<UiState<List<OrderRecord>>>(UiState.Loading)
    val ordersState: StateFlow<UiState<List<OrderRecord>>> = _ordersState.asStateFlow()

    // Estado del pedido actualmente en seguimiento en vivo
    private val _currentTrackingOrder = MutableStateFlow<OrderRecord?>(null)
    val currentTrackingOrder: StateFlow<OrderRecord?> = _currentTrackingOrder.asStateFlow()

    // Estado para el panel de administración
    private val _adminOrdersState = MutableStateFlow<UiState<List<AdminOrderDto>>>(UiState.Loading)
    val adminOrdersState: StateFlow<UiState<List<AdminOrderDto>>> = _adminOrdersState.asStateFlow()

    private var trackingJob: Job? = null

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _ordersState.value = UiState.Loading
            val result = orderRepository.getOrders()
            result.onSuccess { list ->
                if (list.isEmpty()) {
                    _ordersState.value = UiState.Empty
                } else {
                    _ordersState.value = UiState.Success(list)
                }
            }.onFailure { error ->
                _ordersState.value = UiState.Error(
                    message = error.message ?: "Error al recuperar los pedidos",
                    cause = error
                )
            }
        }
    }

    fun startTrackingOrder(orderId: Int) {
        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            while (isActive) {
                val result = orderRepository.getOrderById(orderId)
                result.onSuccess { order ->
                    _currentTrackingOrder.value = order
                    // Si el pedido llegó a un estado terminal (entregado o cancelado), finalizamos el sondeo
                    if (order.estado.isTerminal) {
                        return@launch
                    }
                }
                delay(3000)
            }
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
    }

    fun setTrackingOrder(order: OrderRecord) {
        _currentTrackingOrder.value = order
        if (order.numericId > 0) {
            startTrackingOrder(order.numericId)
        }
    }

    fun createOrder(
        order: OrderRecord,
        restauranteId: Int = 1,
        metodoPago: String = "cash",
        cuponId: Int? = null,
        onSuccess: (OrderRecord) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = orderRepository.createOrder(order, restauranteId, metodoPago, cuponId)
            result.onSuccess { freshOrder ->
                _currentTrackingOrder.value = freshOrder
                if (freshOrder.numericId > 0) {
                    startTrackingOrder(freshOrder.numericId)
                }
                loadOrders()
                onSuccess(freshOrder)
            }.onFailure { error ->
                onError(error.message ?: "Error al crear el pedido en el servidor")
            }
        }
    }

    fun cancelCurrentOrder(
        orderId: Int,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = orderRepository.cancelOrder(orderId)
            result.onSuccess {
                stopTracking()
                // Actualizar inmediatamente el estado en memoria
                _currentTrackingOrder.value?.let { current ->
                    if (current.numericId == orderId) {
                        _currentTrackingOrder.value = current.copy(
                            estado = com.example.foodjeetapp.data.model.OrderStatus.CANCELADO
                        )
                    }
                }
                loadOrders()
                onSuccess()
            }.onFailure { error ->
                onError(error.message ?: "No se pudo cancelar el pedido")
            }
        }
    }

    fun loadAdminOrders() {
        viewModelScope.launch {
            _adminOrdersState.value = UiState.Loading
            val result = orderRepository.getAllAdminOrders()
            result.onSuccess { list ->
                if (list.isEmpty()) {
                    _adminOrdersState.value = UiState.Empty
                } else {
                    _adminOrdersState.value = UiState.Success(list)
                }
            }.onFailure { error ->
                _adminOrdersState.value = UiState.Error(
                    message = error.message ?: "Error al cargar órdenes de administración",
                    cause = error
                )
            }
        }
    }

    fun advanceOrderStatus(
        orderId: Int,
        nextStatus: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = orderRepository.advanceOrderStatus(orderId, nextStatus)
            result.onSuccess {
                loadAdminOrders()
                loadOrders()
                onSuccess()
            }.onFailure { error ->
                onError(error.message ?: "Error al avanzar estado del pedido")
            }
        }
    }

    fun submitReview(
        orderId: String,
        stars: Int,
        comment: String,
        onDone: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = orderRepository.updateOrderReview(orderId, stars, comment)
            result.onSuccess {
                loadOrders()
                onDone()
            }.onFailure { error ->
                onError(error.message ?: "Error al enviar la reseña")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}
