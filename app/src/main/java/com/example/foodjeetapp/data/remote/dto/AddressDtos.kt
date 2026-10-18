package com.example.foodjeetapp.data.remote.dto

import com.example.foodjeetapp.data.model.SavedAddress
import com.google.gson.annotations.SerializedName

data class AddressDto(
    @SerializedName("id") val id: Int,
    @SerializedName("usuario_id") val usuarioId: Int,
    @SerializedName("direccion_detallada") val direccionDetallada: String,
    @SerializedName("referencia") val referencia: String?,
    @SerializedName("latitud") val latitud: Double?,
    @SerializedName("longitud") val longitud: Double?,
    @SerializedName("es_predeterminada") val esPredeterminada: Boolean?
) {
    fun toDomain() = SavedAddress(
        id = id,
        direccion = direccionDetallada,
        referencia = referencia,
        latitud = latitud,
        longitud = longitud,
        predeterminada = esPredeterminada == true
    )
}

data class CreateAddressRequestDto(
    @SerializedName("direccion_detallada") val direccionDetallada: String,
    @SerializedName("referencia") val referencia: String? = null,
    @SerializedName("latitud") val latitud: Double? = null,
    @SerializedName("longitud") val longitud: Double? = null,
    @SerializedName("es_predeterminada") val esPredeterminada: Boolean = true
)

data class UpdateAddressRequestDto(
    @SerializedName("direccion_detallada") val direccionDetallada: String? = null,
    @SerializedName("referencia") val referencia: String? = null,
    @SerializedName("latitud") val latitud: Double? = null,
    @SerializedName("longitud") val longitud: Double? = null,
    @SerializedName("es_predeterminada") val esPredeterminada: Boolean? = null
)

data class DeleteAddressResponseDto(
    @SerializedName("message") val message: String?
)
