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

enum class OrderStatus(val label: String) {
    PENDIENTE("Pendiente"),
    EN_PREPARACION("En preparación"),
    EN_CAMINO("En camino"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado")
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
    val reviewComment: String? = null
)

data class UserProfile(
    val name: String = "Orlando Dorival",
    val email: String = "orlando@foodjet.com",
    val phone: String = "987654321",
    val isStudent: Boolean = true,
    val isAdmin: Boolean = false
)
