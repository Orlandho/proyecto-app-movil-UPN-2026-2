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

object FoodJetMockData {
    val promotions = listOf(
        PromotionSlide(
            id = 1,
            title = "¡Comida deliciosa a tu puerta!",
            subtitle = "Entrega rápida y confiable en toda la ciudad",
            imageUrl = "https://elcomercio.pe/resizer/v2/55QLUH7SV5E53AFZHMYNTJ4TTE.jpg?auth=bdffd2a0f4da7a109fb83ad9b94fcf63e5ca1c345be580dc898152c14dac10a0&width=1200&height=675&quality=75&smart=true",
            buttonText = "Ordenar ahora"
        ),
        PromotionSlide(
            id = 2,
            title = "Las mejores hamburguesas",
            subtitle = "Jugosas, frescas y entregadas en minutos",
            imageUrl = "https://images.unsplash.com/photo-1651843465180-5965076f7368?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=1920",
            buttonText = "Ver menú"
        ),
        PromotionSlide(
            id = 3,
            title = "Pizza caliente, directo a ti",
            subtitle = "Tu pizza favorita en 30 minutos o menos",
            imageUrl = "https://images.unsplash.com/photo-1678443238947-e58d71bf2e23?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=1920",
            buttonText = "Pedir pizza"
        )
    )

    val products = listOf(
        ProductItem(
            id = 1,
            nombre = "Hamburguesa Clásica",
            descripcion = "Doble carne, queso cheddar, lechuga, tomate y salsa especial de la casa.",
            precio = 8.50,
            tipoComida = "Comida rápida",
            imagenUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=800&q=80",
            descuentoEstudiante = 10.0,
            disponibilidad = true,
            tiempoEntrega = "30 minutos",
            restauranteNombre = "Burger King",
            restauranteId = 1
        ),
        ProductItem(
            id = 2,
            nombre = "Pizza Margarita",
            descripcion = "Salsa de tomate casera, mozzarella fresca de búfala y albahaca italiana.",
            precio = 12.00,
            tipoComida = "Pizzas",
            imagenUrl = "https://imag.bonviveur.com/pizza-margarita.jpg",
            descuentoEstudiante = 0.0,
            disponibilidad = true,
            tiempoEntrega = "1 hora",
            restauranteNombre = "Pizza Hut",
            restauranteId = 2
        ),
        ProductItem(
            id = 3,
            nombre = "Pizza Pepperoni",
            descripcion = "Salsa de tomate especiada, mozzarella derretida y abundante pepperoni crocante.",
            precio = 14.50,
            tipoComida = "Pizzas",
            imagenUrl = "https://images.unsplash.com/photo-1628840042765-356cda07504e?auto=format&fit=crop&w=800&q=80",
            descuentoEstudiante = 15.0,
            disponibilidad = true,
            tiempoEntrega = "1 hora",
            restauranteNombre = "Pizza Hut",
            restauranteId = 2
        ),
        ProductItem(
            id = 4,
            nombre = "Sushi Roll California",
            descripcion = "Cangrejo suave, aguacate cremoso, pepino japonés y sésamo tostado.",
            precio = 9.00,
            tipoComida = "Sushi",
            imagenUrl = "https://images.unsplash.com/photo-1579871494447-9811cf80d66c?auto=format&fit=crop&w=800&q=80",
            descuentoEstudiante = 20.0,
            disponibilidad = true,
            tiempoEntrega = "Más de 1 hora",
            restauranteNombre = "Sushi Club",
            restauranteId = 3
        ),
        ProductItem(
            id = 5,
            nombre = "Ensalada César",
            descripcion = "Lechuga romana fresca, crutones horneados, queso parmesano y aderezo César.",
            precio = 7.00,
            tipoComida = "Ensaladas",
            imagenUrl = "https://images.unsplash.com/photo-1550304943-4f24f54ddde9?auto=format&fit=crop&w=800&q=80",
            descuentoEstudiante = 0.0,
            disponibilidad = true,
            tiempoEntrega = "30 minutos",
            restauranteNombre = "Burger King",
            restauranteId = 1
        ),
        ProductItem(
            id = 6,
            nombre = "Refresco de Cola",
            descripcion = "Bebida gasificada fría, lata de 330ml para acompañar tus comidas.",
            precio = 2.00,
            tipoComida = "Bebidas",
            imagenUrl = "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?auto=format&fit=crop&w=800&q=80",
            descuentoEstudiante = 5.0,
            disponibilidad = true,
            tiempoEntrega = "30 minutos",
            restauranteNombre = "Burger King",
            restauranteId = 1
        )
    )

    val initialOrders = listOf(
        OrderRecord(
            id = "FJ-1002",
            fecha = "6 de Septiembre, 2026",
            estado = OrderStatus.ENTREGADO,
            items = listOf(
                CartItem(products[0], 2),
                CartItem(products[5], 2)
            ),
            subtotal = 21.00,
            impuestos = 3.78,
            envio = 5.00,
            descuento = 2.10,
            total = 27.68,
            paymentMethod = "Billetera Digital (Yape)",
            reviewStars = 5,
            reviewComment = "Excelente servicio y la hamburguesa llegó súper caliente."
        ),
        OrderRecord(
            id = "FJ-1001",
            fecha = "4 de Septiembre, 2026",
            estado = OrderStatus.ENTREGADO,
            items = listOf(
                CartItem(products[1], 1),
                CartItem(products[4], 1)
            ),
            subtotal = 19.00,
            impuestos = 3.42,
            envio = 5.00,
            descuento = 0.0,
            total = 27.42,
            paymentMethod = "Pago en Efectivo",
            reviewStars = null,
            reviewComment = null
        ),
        OrderRecord(
            id = "FJ-1000",
            fecha = "2 de Septiembre, 2026",
            estado = OrderStatus.CANCELADO,
            items = listOf(
                CartItem(products[3], 1)
            ),
            subtotal = 9.00,
            impuestos = 1.62,
            envio = 5.00,
            descuento = 1.80,
            total = 13.82,
            paymentMethod = "Tarjeta"
        )
    )
}
