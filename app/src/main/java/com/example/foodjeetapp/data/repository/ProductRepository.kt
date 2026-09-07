package com.example.foodjeetapp.data.repository

import com.example.foodjeetapp.data.model.ProductItem
import com.example.foodjeetapp.data.model.PromotionSlide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Contrato de repositorio para el catálogo de productos de FoodJet.
 * Cumple con REQ-SEM06-LOG-03 (Patrón Repository como única fuente de verdad).
 */
interface ProductRepository {
    fun getProductsStream(): Flow<List<ProductItem>>
    suspend fun getProducts(): Result<List<ProductItem>>
    suspend fun getProductById(id: Int): Result<ProductItem?>
    fun getPromotions(): List<PromotionSlide>
}

/**
 * Implementación de producción para el repositorio de productos.
 * Retorna valores por defecto limpios (vacíos) a la espera de la integración del backend y base de datos local.
 */
class ProductRepositoryImpl : ProductRepository {

    // TODO: (REQ-SEM08-LOG-01) Integrar cliente HTTP Retrofit con motor OkHttp para llamadas a servicios web.
    // TODO: (REQ-SEM08-LOG-02) Inyectar interfaz de servicio Retrofit con endpoints GET/POST/PUT/DELETE suspendidos.
    // TODO: (REQ-SEM08-LOG-03) Ejecutar llamadas de red en segundo plano utilizando el despachador Dispatchers.IO.
    // TODO: (REQ-SEM05-INF-02) Inyectar ProductDao de Room ORM para persistencia y sincronización en base de datos SQLite.
    // TODO: (REQ-SEM08-INF-02) Implementar almacenamiento en caché local combinando Room y Retrofit para modo sin conexión.

    override fun getProductsStream(): Flow<List<ProductItem>> = flow {
        // Flujo observable reactivo (REQ-SEM05-LOG-02).
        // En ausencia del backend REST o base de datos local Room, emite una lista vacía real.
        emit(emptyList())
    }

    override suspend fun getProducts(): Result<List<ProductItem>> {
        // En producción se consumirá el endpoint REST con Retrofit y se persistirá en Room:
        // val remoteProducts = retrofitService.getProducts()
        // productDao.insertAll(remoteProducts.toEntityList())
        // return Result.success(productDao.getAllProducts().toDomainList())
        return Result.success(emptyList())
    }

    override suspend fun getProductById(id: Int): Result<ProductItem?> {
        // TODO: Consultar producto por identificador en Room o API REST remota.
        return Result.success(null)
    }

    override fun getPromotions(): List<PromotionSlide> {
        // Banners promocionales de la identidad visual FoodJet para el carrusel principal
        return listOf(
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
    }
}
