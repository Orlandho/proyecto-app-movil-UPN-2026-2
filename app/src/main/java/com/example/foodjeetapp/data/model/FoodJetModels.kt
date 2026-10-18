package com.example.foodjeetapp.data.model

data class ProductItem(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    val precio: Double,
    val tipoComida: String,
    val imagenUrl: String,
    val descuentoEstudiante: Double = 0.0,
    val disponibilidad: Boolean = true,
    val tiempoEntrega: String = "30 minutos",
    val restauranteNombre: String = "Burger King",
    val restauranteId: Int = 1
) {
    fun getEffectivePrice(isStudent: Boolean): Double {
        return if (isStudent && descuentoEstudiante > 0.0) {
            precio - (precio * (descuentoEstudiante / 100.0))
        } else {
            precio
        }
    }
}

data class CartItem(
    val product: ProductItem,
    val quantity: Int
)

data class PromotionSlide(
    val id: Int,
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val buttonText: String
)

enum class OrderStatus(val label: String, val stepIndex: Int = 1, val clave: String = "") {
    PENDIENTE("Pendiente", 1, "pendiente"),
    CONFIRMADO("Confirmado", 1, "confirmado"),
    EN_PREPARACION("En preparación", 2, "en_preparacion"),
    EN_CAMINO("En camino", 3, "en_camino"),
    ENTREGADO("Entregado", 4, "entregado"),
    CANCELADO("Cancelado", 0, "cancelado");

    val isTerminal: Boolean get() = this == ENTREGADO || this == CANCELADO
    val isCancelable: Boolean get() = this == PENDIENTE || this == CONFIRMADO
}

data class GeoPoint(
    val latitud: Double,
    val longitud: Double
)

data class OrderHistoryEntry(
    val estado: String,
    val fecha: String,
    val origen: String
)

data class OrderRecord(
    val id: String,
    val fecha: String,
    val estado: OrderStatus,
    val items: List<CartItem>,
    val subtotal: Double,
    val impuestos: Double,
    val envio: Double = 5.00,
    val descuento: Double = 0.0,
    val total: Double,
    val paymentMethod: String = "Efectivo",
    val reviewStars: Int? = null,
    val reviewComment: String? = null,
    val numericId: Int = id.removePrefix("FJ-").toIntOrNull() ?: 0,
    val direccionEntrega: String? = null,
    val latitudEntrega: Double? = null,
    val longitudEntrega: Double? = null,
    val restauranteNombre: String = "FoodJet",
    val restauranteLatitud: Double? = null,
    val restauranteLongitud: Double? = null,
    val restauranteDireccion: String? = null,
    val repartidorLat: Double? = null,
    val repartidorLon: Double? = null,
    val etaMinutos: Int? = null,
    val distanciaMetros: Double? = null,
    val avanceReparto: Double? = null,
    val restanteMetros: Int? = null,
    val llegadaEstimada: String? = null,
    val historialEstados: List<OrderHistoryEntry> = emptyList(),
    val rutaPuntos: List<GeoPoint> = emptyList(),
    val restauranteId: Int? = null,
    /** Fecha completa de creación (ISO 8601), para la línea de tiempo. */
    val fechaIso: String? = null,
    /** Segundos que dura cada estado en el simulador del backend. Vacío si el pedido no tiene plan. */
    val planTiempos: Map<String, Double> = emptyMap(),
    /** Momento en que salió a reparto (ISO 8601). */
    val envioIniciadoEn: String? = null,
    /** Reloj del servidor menos el del teléfono, medido al recibir el pedido. */
    val desfaseRelojMs: Long = 0
)

/** Dirección de entrega guardada en el backend (`DeliveryAddress`). */
data class SavedAddress(
    val id: Int,
    val direccion: String,
    val referencia: String?,
    val latitud: Double?,
    val longitud: Double?,
    val predeterminada: Boolean
) {
    val punto: GeoPoint? get() =
        if (Geo.coordenadasValidas(latitud, longitud)) GeoPoint(latitud!!, longitud!!) else null
}

/**
 * Punto de entrega que eligió el cliente en el checkout. Sin [punto] solo
 * cuando no hubo mapa ni GPS: como en la web, el pedido sale con la
 * dirección escrita y el backend no traza ruta.
 */
data class DeliverySelection(
    val direccion: String,
    val referencia: String?,
    val punto: GeoPoint?,
    /** Dirección guardada que se reutiliza, o null si hay que crear una nueva. */
    val direccionGuardadaId: Int? = null
)

data class UserProfile(
    val name: String = "Orlando Dorival",
    val email: String = "orlando@foodjet.com",
    val phone: String = "987654321",
    val isStudent: Boolean = true,
    val isAdmin: Boolean = false
)
