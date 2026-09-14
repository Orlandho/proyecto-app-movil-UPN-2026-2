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

enum class OrderStatus(val label: String, val stepIndex: Int = 1) {
    PENDIENTE("Pendiente", 1),
    CONFIRMADO("Confirmado", 1),
    EN_PREPARACION("En preparación", 2),
    EN_CAMINO("En camino", 3),
    ENTREGADO("Entregado", 4),
    CANCELADO("Cancelado", 0);

    val isTerminal: Boolean get() = this == ENTREGADO || this == CANCELADO
    val isCancelable: Boolean get() = this == PENDIENTE || this == CONFIRMADO
}

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
    val numericId: Int = id.removePrefix("FJ-").toIntOrNull() ?: 0
)

data class UserProfile(
    val name: String = "Orlando Dorival",
    val email: String = "orlando@foodjet.com",
    val phone: String = "987654321",
    val isStudent: Boolean = true,
    val isAdmin: Boolean = false
)
