package com.example.foodjeetapp.data.repository

import com.example.foodjeetapp.data.local.dao.ProductDao
import com.example.foodjeetapp.data.model.ProductItem
import com.example.foodjeetapp.data.model.PromotionSlide
import com.example.foodjeetapp.data.remote.api.FoodJetApiService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

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
 * Integra Retrofit 2 para el consumo REST y Room ORM para la persistencia local offline-first.
 * Cumple con REQ-SEM08-LOG-01, REQ-SEM08-LOG-03, REQ-SEM08-INF-02 y REQ-SEM05-INF-02.
 */
class ProductRepositoryImpl(
    private val apiService: FoodJetApiService,
    private val productDao: ProductDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ProductRepository {

    override fun getProductsStream(): Flow<List<ProductItem>> {
        // Flujo observable desde Room SQLite (REQ-SEM05-LOG-02)
        return productDao.getAllProductsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getProducts(): Result<List<ProductItem>> = withContext(ioDispatcher) {
        try {
            val response = apiService.getProducts()
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                // Guardar en la base de datos local Room (Caché Offline)
                productDao.insertAll(dtos.map { it.toEntity() })
                Result.success(dtos.map { it.toDomain() })
            } else {
                // Si la API responde con error, intentamos servir los datos locales de Room
                val cached = productDao.getAllProducts()
                if (cached.isNotEmpty()) {
                    Result.success(cached.map { it.toDomain() })
                } else {
                    Result.failure(Exception("Error al cargar productos: ${response.code()} ${response.message()}"))
                }
            }
        } catch (e: Exception) {
            // Manejo de contingencia offline: si no hay red, leer desde Room SQLite
            val cached = productDao.getAllProducts()
            if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toDomain() })
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getProductById(id: Int): Result<ProductItem?> = withContext(ioDispatcher) {
        try {
            val local = productDao.getProductById(id)
            if (local != null) {
                Result.success(local.toDomain())
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getPromotions(): List<PromotionSlide> {
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
