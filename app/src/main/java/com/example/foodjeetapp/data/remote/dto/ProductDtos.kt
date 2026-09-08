package com.example.foodjeetapp.data.remote.dto

import com.example.foodjeetapp.data.local.entity.ProductEntity
import com.example.foodjeetapp.data.model.ProductItem
import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects (DTO) para productos y restaurantes.
 * Cumple con REQ-SEM07-LOG-02.
 */
data class RestaurantDto(
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("estado_afiliacion") val estadoAfiliacion: String?,
    @SerializedName("qr_pago") val qrPago: String?,
    @SerializedName("tiempo_entrega") val tiempoEntrega: String?,
    @SerializedName("calificacion_promedio") val calificacionPromedio: Double?
)

data class ProductDto(
    @SerializedName("id") val id: Int,
    @SerializedName("restaurante_id") val restauranteId: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("precio") val precio: Double,
    @SerializedName("tipo_comida") val tipoComida: String?,
    @SerializedName("imagen_url") val imagenUrl: String?,
    @SerializedName("descuento_estudiante") val descuentoEstudiante: Double?,
    @SerializedName("disponibilidad") val disponibilidad: Boolean?,
    @SerializedName("Restaurant") val restaurant: RestaurantDto?
) {
    fun toDomain(): ProductItem {
        return ProductItem(
            id = id,
            nombre = nombre,
            descripcion = descripcion ?: "",
            precio = precio,
            tipoComida = tipoComida ?: "General",
            imagenUrl = imagenUrl ?: "",
            descuentoEstudiante = descuentoEstudiante ?: 0.0,
            disponibilidad = disponibilidad ?: true,
            tiempoEntrega = restaurant?.tiempoEntrega ?: "30 minutos",
            restauranteNombre = restaurant?.nombre ?: "FoodJet Express",
            restauranteId = restauranteId
        )
    }

    fun toEntity(): ProductEntity {
        return ProductEntity(
            id = id,
            nombre = nombre,
            descripcion = descripcion ?: "",
            precio = precio,
            tipoComida = tipoComida ?: "General",
            imagenUrl = imagenUrl ?: "",
            descuentoEstudiante = descuentoEstudiante ?: 0.0,
            disponibilidad = disponibilidad ?: true,
            tiempoEntrega = restaurant?.tiempoEntrega ?: "30 minutos",
            restauranteNombre = restaurant?.nombre ?: "FoodJet Express",
            restauranteId = restauranteId
        )
    }
}
