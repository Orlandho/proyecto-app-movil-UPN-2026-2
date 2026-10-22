package com.example.foodjeetapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateReviewRequestDto(
    @SerializedName("pedido_id") val pedidoId: Int,
    @SerializedName("puntuacion") val puntuacion: Int,
    @SerializedName("comentario") val comentario: String?
)

data class CreateReviewResponseDto(
    @SerializedName("message") val message: String?,
    @SerializedName("review") val review: ReviewDto?
)
