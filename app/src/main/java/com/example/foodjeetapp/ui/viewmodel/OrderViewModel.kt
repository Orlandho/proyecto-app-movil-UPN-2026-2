package com.example.foodjeetapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodjeetapp.data.common.UiState
import com.example.foodjeetapp.data.model.OrderRecord
import com.example.foodjeetapp.data.repository.OrderRepository
import com.example.foodjeetapp.data.repository.OrderRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la gestión de historial y creación de pedidos.
 * Cumple con REQ-SEM06-LOG-01, REQ-SEM06-LOG-02 y REQ-SEM06-LOG-04.
 */
class OrderViewModel(
    private val orderRepository: OrderRepository = OrderRepositoryImpl()
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

    fun createOrder(order: OrderRecord, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = orderRepository.createOrder(order)
            result.onSuccess {
                loadOrders()
                onSuccess()
            }
        }
    }

    fun submitReview(orderId: String, stars: Int, comment: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            orderRepository.updateOrderReview(orderId, stars, comment)
            loadOrders()
            onDone()
        }
    }
}
