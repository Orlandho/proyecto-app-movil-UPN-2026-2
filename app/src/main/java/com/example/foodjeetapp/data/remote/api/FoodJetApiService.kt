package com.example.foodjeetapp.data.remote.api

import com.example.foodjeetapp.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Interfaz de servicio REST Retrofit para FoodJet.
 * Cumple con REQ-SEM08-LOG-02 (métodos HTTP suspendidos vinculados a corrutinas).
 */
interface FoodJetApiService {

    // 1. Autenticación y Perfil
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): Response<LoginResponseDto>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): Response<RegisterResponseDto>

    @GET("auth/me")
    suspend fun getProfile(): Response<UserProfileResponseDto>

    // 2. Catálogo de Productos
    @GET("products")
    suspend fun getProducts(): Response<List<ProductDto>>

    // 3. Pedidos
    @GET("orders/my-orders")
    suspend fun getMyOrders(): Response<List<OrderResponseDto>>

    @GET("orders/{id}")
    suspend fun getOrderById(@Path("id") id: Int): Response<OrderResponseDto>

    @POST("orders")
    suspend fun createOrder(@Body body: CreateOrderRequestDto): Response<CreateOrderResponseDto>

    @PUT("orders/{id}/cancel")
    suspend fun cancelOrder(@Path("id") id: Int): Response<CancelOrderResponseDto>

    // 3.1 Administración de Pedidos (Panel de Operaciones)
    @GET("orders")
    suspend fun getAllOrders(): Response<List<AdminOrderDto>>

    @PUT("orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") id: Int,
        @Body body: UpdateOrderStatusRequestDto
    ): Response<UpdateOrderStatusResponseDto>

    // 4. Direcciones de Entrega
    @GET("addresses")
    suspend fun getAddresses(): Response<List<AddressDto>>

    @POST("addresses")
    suspend fun createAddress(@Body body: CreateAddressRequestDto): Response<AddressDto>

    // 5. Calificaciones y Reseñas
    @POST("reviews")
    suspend fun createReview(@Body body: CreateReviewRequestDto): Response<CreateReviewResponseDto>

    // 6. Favoritos Sincronizados
    @GET("favorites")
    suspend fun getFavorites(): Response<List<ProductDto>>

    @POST("favorites/{productId}/toggle")
    suspend fun toggleFavorite(@Path("productId") productId: Int): Response<ToggleFavoriteResponseDto>

    // 7. Verificación de Estudiante
    @POST("users/verify-student")
    suspend fun verifyStudent(): Response<VerifyStudentResponseDto>
}
