package com.example.foodjeetapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodjeetapp.data.common.UiState
import com.example.foodjeetapp.data.di.ServiceLocator
import com.example.foodjeetapp.data.model.OrderRecord
import com.example.foodjeetapp.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la gestión de historial y creación de pedidos en el backend.
 * Cumple con REQ-SEM06-LOG-01, REQ-SEM06-LOG-02 y REQ-SEM06-LOG-04.
 */
class OrderViewModel(
    private val orderRepository: OrderRepository = ServiceLocator.orderRepository
) : ViewModel() {

    private val _ordersState = MutableStateFlow<UiState<List<OrderRecord>>>(UiState.Loading)
    val ordersState: StateFlow<UiState<List<OrderRecord>>> = _ordersState.asStateFlow()

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

    fun createOrder(
        order: OrderRecord,
        restauranteId: Int = 1,
        metodoPago: String = "efectivo",
        cuponId: Int? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = orderRepository.createOrder(order, restauranteId, metodoPago, cuponId)
            result.onSuccess {
                loadOrders()
                onSuccess()
            }.onFailure { error ->
                onError(error.message ?: "Error al crear el pedido en el servidor")
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
}
