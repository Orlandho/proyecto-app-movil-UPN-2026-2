package com.example.foodjeetapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AddressDto(
    @SerializedName("id") val id: Int,
    @SerializedName("usuario_id") val usuarioId: Int,
    @SerializedName("direccion_detallada") val direccionDetallada: String,
    @SerializedName("referencia") val referencia: String?,
    @SerializedName("latitud") val latitud: Double?,
    @SerializedName("longitud") val longitud: Double?,
    @SerializedName("es_predeterminada") val esPredeterminada: Boolean?
)

data class CreateAddressRequestDto(
    @SerializedName("direccion_detallada") val direccionDetallada: String,
    @SerializedName("referencia") val referencia: String? = null,
    @SerializedName("es_predeterminada") val esPredeterminada: Boolean = true
)
