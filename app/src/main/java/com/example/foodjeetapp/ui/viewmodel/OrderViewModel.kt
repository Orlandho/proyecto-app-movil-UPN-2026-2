package com.example.foodjeetapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodjeetapp.data.common.UiState
import com.example.foodjeetapp.data.di.ServiceLocator
import com.example.foodjeetapp.data.model.DeliverySelection
import com.example.foodjeetapp.data.model.OrderRecord
import com.example.foodjeetapp.data.model.OrderStatus
import com.example.foodjeetapp.data.model.SavedAddress
import com.example.foodjeetapp.data.remote.dto.AdminOrderDto
import com.example.foodjeetapp.data.remote.dto.RestaurantDto
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

    companion object {
        /** Consulta del seguimiento mientras el repartidor está en camino. */
        const val INTERVALO_EN_CAMINO_MS = 3_000L
        /** En los demás estados el pedido cambia cada varios minutos. */
        const val INTERVALO_EN_ESPERA_MS = 8_000L
        /** Tope de la espera tras fallos seguidos (sin red, servidor caído). */
        const val INTERVALO_MAXIMO_MS = 30_000L

        /**
         * Cada cuánto consultar el seguimiento (REQ-SEM04-INF-02). Un pedido
         * terminado no se consulta más; tras errores la espera se duplica,
         * para no gastar batería y datos contra un servidor que no responde.
         */
        fun intervaloDeSondeo(estado: OrderStatus?, fallosSeguidos: Int): Long? {
            if (estado?.isTerminal == true) return null
            val base = if (estado == OrderStatus.EN_CAMINO) INTERVALO_EN_CAMINO_MS else INTERVALO_EN_ESPERA_MS
            if (fallosSeguidos <= 0) return base
            return (base shl fallosSeguidos.coerceAtMost(4)).coerceAtMost(INTERVALO_MAXIMO_MS)
        }
    }

    private val _ordersState = MutableStateFlow<UiState<List<OrderRecord>>>(UiState.Loading)
    val ordersState: StateFlow<UiState<List<OrderRecord>>> = _ordersState.asStateFlow()

    // Estado del pedido actualmente en seguimiento en vivo
    private val _currentTrackingOrder = MutableStateFlow<OrderRecord?>(null)
    val currentTrackingOrder: StateFlow<OrderRecord?> = _currentTrackingOrder.asStateFlow()

    private val _trackingError = MutableStateFlow<String?>(null)
    val trackingError: StateFlow<String?> = _trackingError.asStateFlow()

    // Estado para el panel de administración
    private val _adminOrdersState = MutableStateFlow<UiState<List<AdminOrderDto>>>(UiState.Loading)
    val adminOrdersState: StateFlow<UiState<List<AdminOrderDto>>> = _adminOrdersState.asStateFlow()

    // Direcciones guardadas del cliente para el checkout
    private val _addressesState = MutableStateFlow<UiState<List<SavedAddress>>>(UiState.Loading)
    val addressesState: StateFlow<UiState<List<SavedAddress>>> = _addressesState.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val restaurantes = mutableMapOf<Int, RestaurantDto>()

    private var trackingJob: Job? = null
    private var trackedOrderId: Int? = null

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

    fun loadAddresses() {
        viewModelScope.launch {
            _addressesState.value = UiState.Loading
            orderRepository.getAddresses()
                .onSuccess { list -> _addressesState.value = if (list.isEmpty()) UiState.Empty else UiState.Success(list) }
                .onFailure { error -> _addressesState.value = UiState.Error(error.message ?: "No se pudieron cargar tus direcciones", error) }
        }
    }

    /** Local de un pedido (para el QR de pago), con caché en memoria. */
    suspend fun restaurante(id: Int): Result<RestaurantDto> {
        restaurantes[id]?.let { return Result.success(it) }
        return orderRepository.getRestaurant(id).onSuccess { restaurantes[id] = it }
    }

    /**
     * Elige qué pedido se sigue. No empieza a consultar: eso lo hace
     * [reanudarSeguimiento] cuando la pantalla de seguimiento está visible.
     */
    fun seguirPedido(orderId: Int) {
        if (trackedOrderId != orderId) {
            pausarSeguimiento()
            trackedOrderId = orderId
            _trackingError.value = null
            if (_currentTrackingOrder.value?.numericId != orderId) _currentTrackingOrder.value = null
        }
    }

    /** Arranca el sondeo; se llama al volver a primer plano (ON_START). */
    fun reanudarSeguimiento() {
        val orderId = trackedOrderId ?: return
        if (trackingJob?.isActive == true) return
        trackingJob = viewModelScope.launch {
            var fallos = 0
            while (isActive) {
                orderRepository.getOrderById(orderId)
                    .onSuccess { order ->
                        fallos = 0
                        _trackingError.value = null
                        _currentTrackingOrder.value = order
                    }
                    .onFailure { error ->
                        fallos++
                        _trackingError.value = error.message
                    }
                val espera = intervaloDeSondeo(_currentTrackingOrder.value?.estado, fallos) ?: break
                delay(espera)
            }
        }
    }

    /** Detiene el sondeo; se llama al pasar a segundo plano (ON_STOP) o al salir. */
    fun pausarSeguimiento() {
        trackingJob?.cancel()
        trackingJob = null
    }

    fun setTrackingOrder(order: OrderRecord) {
        if (order.numericId > 0) seguirPedido(order.numericId)
        _currentTrackingOrder.value = order
    }

    fun createOrder(
        order: OrderRecord,
        entrega: DeliverySelection,
        restauranteId: Int,
        metodoPago: String = "cash",
        cuponId: Int? = null,
        onSuccess: (OrderRecord) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = orderRepository.createOrder(order, entrega, restauranteId, metodoPago, cuponId)
            _isSubmitting.value = false
            result.onSuccess { freshOrder ->
                seguirPedido(freshOrder.numericId)
                _currentTrackingOrder.value = freshOrder
                loadOrders()
                loadAddresses()
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
                pausarSeguimiento()
                // El estado y su hora los da el servidor, no se suponen aquí.
                orderRepository.getOrderById(orderId).onSuccess { _currentTrackingOrder.value = it }
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
                // Lo normal es un 409: el simulador o alguien más ya movió el
                // pedido. Se recarga para que el panel muestre el estado real.
                loadAdminOrders()
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

    /** Al cerrar sesión o cambiar de servidor: nada del usuario anterior queda en pantalla. */
    fun limpiar() {
        pausarSeguimiento()
        trackedOrderId = null
        _currentTrackingOrder.value = null
        _addressesState.value = UiState.Loading
        _adminOrdersState.value = UiState.Loading
        restaurantes.clear()
        loadOrders()
    }

    override fun onCleared() {
        super.onCleared()
        pausarSeguimiento()
    }
}
