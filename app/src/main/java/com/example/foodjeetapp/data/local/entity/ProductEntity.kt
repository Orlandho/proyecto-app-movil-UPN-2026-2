package com.example.foodjeetapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.foodjeetapp.data.model.ProductItem

/**
 * Entidad relacional local para almacenamiento en Room SQLite.
 * Cumple con REQ-SEM05-INF-02.
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val descripcion: String,
    val precio: Double,
    val tipoComida: String,
    val imagenUrl: String,
    val descuentoEstudiante: Double,
    val disponibilidad: Boolean,
    val tiempoEntrega: String,
    val restauranteNombre: String,
    val restauranteId: Int
) {
    fun toDomain(): ProductItem {
        return ProductItem(
            id = id,
            nombre = nombre,
            descripcion = descripcion,
            precio = precio,
            tipoComida = tipoComida,
            imagenUrl = imagenUrl,
            descuentoEstudiante = descuentoEstudiante,
            disponibilidad = disponibilidad,
            tiempoEntrega = tiempoEntrega,
            restauranteNombre = restauranteNombre,
            restauranteId = restauranteId
        )
    }

    companion object {
        fun fromDomain(product: ProductItem): ProductEntity {
            return ProductEntity(
                id = product.id,
                nombre = product.nombre,
                descripcion = product.descripcion,
                precio = product.precio,
                tipoComida = product.tipoComida,
                imagenUrl = product.imagenUrl,
                descuentoEstudiante = product.descuentoEstudiante,
                disponibilidad = product.disponibilidad,
                tiempoEntrega = product.tiempoEntrega,
                restauranteNombre = product.restauranteNombre,
                restauranteId = product.restauranteId
            )
        }
    }
}
