package com.example.foodjeetapp.data.remote.dto

import com.example.foodjeetapp.data.model.*
import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects (DTO) para creación y consulta de pedidos.
 * Cumple con REQ-SEM07-LOG-02.
 */
data class OrderItemRequestDto(
    @SerializedName("productId") val productId: Int,
    @SerializedName("cantidad") val cantidad: Int
)

data class CreateOrderRequestDto(
    @SerializedName("restaurante_id") val restauranteId: Int,
    @SerializedName("direccion_entrega_id") val direccionEntregaId: Int,
    @SerializedName("cupon_id") val cuponId: Int?,
    @SerializedName("metodo_pago") val metodoPago: String,
    @SerializedName("items") val items: List<OrderItemRequestDto>
)

data class CreateOrderResponseDto(
    @SerializedName("message") val message: String?,
    @SerializedName("order") val order: OrderHeaderDto?
)

data class OrderHeaderDto(
    @SerializedName("id") val id: Int,
    @SerializedName("total") val total: Double,
    @SerializedName("estado") val estado: String,
    @SerializedName("fecha") val fecha: String?
)

data class OrderItemDetailDto(
    @SerializedName("id") val id: Int,
    @SerializedName("product_id") val productId: Int,
    @SerializedName("cantidad") val cantidad: Int,
    @SerializedName("precio_unitario") val precioUnitario: Double,
    @SerializedName("Product") val product: ProductDto?
)

data class ReviewDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("puntuacion") val puntuacion: Int?,
    @SerializedName("comentario") val comentario: String?
)

data class TransactionDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("metodo_pago") val metodoPago: String?,
    @SerializedName("monto") val monto: Double?,
    @SerializedName("estado_pago") val estadoPago: String?
)

data class OrderStatusHistoryDto(
    @SerializedName("estado") val estado: String?,
    @SerializedName("fecha") val fecha: String?,
    @SerializedName("origen") val origen: String?
)

data class TrackingProgressDto(
    @SerializedName("posicion") val posicion: List<Double>? = null,
    @SerializedName("avance") val avance: Double? = null,
    @SerializedName("restante_metros") val restanteMetros: Int? = null,
    @SerializedName("llegada_estimada") val llegadaEstimada: String? = null
)

data class OrderResponseDto(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("restaurante_id") val restauranteId: Int,
    @SerializedName("direccion_entrega_id") val direccionEntregaId: Int,
    @SerializedName("total") val total: Double,
    @SerializedName("impuestos") val impuestos: Double?,
    @SerializedName("costo_envio") val costoEnvio: Double?,
    @SerializedName("estado") val estado: String,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("Restaurant") val restaurant: RestaurantDto?,
    @SerializedName("DeliveryAddress") val deliveryAddress: AddressDto? = null,
    @SerializedName("Review") val review: ReviewDto?,
    @SerializedName("Transaction") val transaction: TransactionDto?,
    @SerializedName("OrderItem") val orderItems: List<OrderItemDetailDto>?,
    @SerializedName("ruta") val ruta: List<List<Double>>? = null,
    @SerializedName("distancia_metros") val distanciaMetros: Double? = null,
    @SerializedName("eta_minutos") val etaMinutos: Int? = null,
    @SerializedName("repartidor_lat") val repartidorLat: Double? = null,
    @SerializedName("repartidor_lon") val repartidorLon: Double? = null,
    @SerializedName("plan_tiempos") val planTiempos: Map<String, Any>? = null,
    @SerializedName("historial") val historial: List<OrderStatusHistoryDto>? = null,
    @SerializedName("seguimiento") val seguimiento: TrackingProgressDto? = null,
    @SerializedName("servidor_ahora") val servidorAhora: String? = null,
    @SerializedName("envio_iniciado_en") val envioIniciadoEn: String? = null
) {
    fun toDomain(recibidoEnMs: Long = System.currentTimeMillis()): OrderRecord {
        val domainStatus = when (Seguimiento.normalizarEstado(estado)) {
            "pendiente" -> OrderStatus.PENDIENTE
            "confirmado" -> OrderStatus.CONFIRMADO
            "en_preparacion", "preparando" -> OrderStatus.EN_PREPARACION
            "en_camino" -> OrderStatus.EN_CAMINO
            "entregado" -> OrderStatus.ENTREGADO
            "cancelado" -> OrderStatus.CANCELADO
            else -> OrderStatus.PENDIENTE
        }

        val cartItems = orderItems?.map { item ->
            val productDomain = item.product?.toDomain() ?: ProductItem(
                id = item.productId,
                nombre = "Producto #${item.productId}",
                descripcion = "",
                precio = item.precioUnitario,
                tipoComida = "General",
                imagenUrl = "",
                restauranteNombre = restaurant?.nombre ?: "FoodJet"
            )
            CartItem(product = productDomain, quantity = item.cantidad)
        } ?: emptyList()

        val subtotalCalculated = cartItems.sumOf { it.product.precio * it.quantity }
        val tax = impuestos ?: (subtotalCalculated * 0.18)
        val shipping = costoEnvio ?: 5.00

        val methodLabel = when (transaction?.metodoPago?.lowercase()?.trim()) {
            "cash" -> "Efectivo"
            "card" -> "Tarjeta"
            "wallet" -> "Billetera Digital"
            else -> "Pago Registrado"
        }

        val routePoints = ruta?.mapNotNull { coords ->
            if (coords.size >= 2) GeoPoint(coords[0], coords[1]) else null
        } ?: emptyList()

        val historyEntries = historial?.mapNotNull { h ->
            if (h.estado != null) {
                OrderHistoryEntry(
                    estado = h.estado,
                    fecha = h.fecha ?: "",
                    origen = h.origen ?: "sistema"
                )
            } else null
        } ?: emptyList()

        val currentRepartidorLat = seguimiento?.posicion?.getOrNull(0) ?: repartidorLat
        val currentRepartidorLon = seguimiento?.posicion?.getOrNull(1) ?: repartidorLon

        return OrderRecord(
            id = "FJ-$id",
            fecha = fecha.take(10),
            estado = domainStatus,
            items = cartItems,
            subtotal = subtotalCalculated,
            impuestos = tax,
            envio = shipping,
            total = total,
            paymentMethod = methodLabel,
            reviewStars = review?.puntuacion,
            reviewComment = review?.comentario,
            numericId = id,
            direccionEntrega = deliveryAddress?.direccionDetallada,
            latitudEntrega = deliveryAddress?.latitud,
            longitudEntrega = deliveryAddress?.longitud,
            restauranteNombre = restaurant?.nombre ?: "FoodJet",
            restauranteLatitud = restaurant?.latitud,
            restauranteLongitud = restaurant?.longitud,
            restauranteDireccion = restaurant?.direccion,
            repartidorLat = currentRepartidorLat,
            repartidorLon = currentRepartidorLon,
            etaMinutos = etaMinutos,
            distanciaMetros = distanciaMetros,
            avanceReparto = seguimiento?.avance,
            restanteMetros = seguimiento?.restanteMetros,
            llegadaEstimada = seguimiento?.llegadaEstimada,
            historialEstados = historyEntries,
            rutaPuntos = routePoints,
            restauranteId = restauranteId,
            fechaIso = fecha,
            planTiempos = planTiempos.orEmpty().mapNotNull { (estado, segundos) ->
                (segundos as? Number)?.toDouble()?.let { Seguimiento.normalizarEstado(estado) to it }
            }.toMap(),
            envioIniciadoEn = envioIniciadoEn,
            desfaseRelojMs = Seguimiento.parseIso(servidorAhora)?.let { it - recibidoEnMs } ?: 0L
        )
    }
}

data class CancelOrderResponseDto(
    @SerializedName("message") val message: String?,
    @SerializedName("order") val order: OrderHeaderDto?
)

data class UpdateOrderStatusRequestDto(
    @SerializedName("nuevo_estado") val nuevoEstado: String
)

data class UpdateOrderStatusResponseDto(
    @SerializedName("message") val message: String?,
    @SerializedName("order") val order: OrderHeaderDto?
)

data class AdminOrderDto(
    @SerializedName("id") val id: Int,
    @SerializedName("estado") val estado: String,
    @SerializedName("total") val total: Double,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("cliente") val cliente: String?,
    @SerializedName("restaurante") val restaurante: String?
)
