package com.example.foodjeetapp.data.remote.dto

import com.example.foodjeetapp.data.model.UserProfile
import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects (DTO) para autenticación y usuarios.
 * Cumple con REQ-SEM07-LOG-02 (tipado estricto e inmutable con @SerializedName).
 */
data class LoginRequestDto(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class UserDto(
    @SerializedName("id") val id: Int = 1,
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("name") val nameAlternate: String? = null,
    @SerializedName("email") val email: String,
    @SerializedName("rol") val rol: String? = null,
    @SerializedName("role") val roleAlternate: String? = null,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("es_estudiante") val esEstudiante: Boolean? = null
) {
    fun toDomain(): UserProfile {
        val userName = nombre ?: nameAlternate ?: "Usuario FoodJet"
        val userRole = rol ?: roleAlternate ?: "cliente"
        return UserProfile(
            name = userName,
            email = email,
            phone = telefono.orEmpty(),
            isStudent = esEstudiante ?: false,
            isAdmin = userRole.equals("admin", ignoreCase = true)
        )
    }
}

data class LoginResponseDto(
    @SerializedName("message") val message: String? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("accessToken") val accessTokenAlternate: String? = null,
    @SerializedName("user") val user: UserDto? = null
) {
    val effectiveToken: String get() = token ?: accessTokenAlternate ?: "jwt_demo_token"
}

data class RegisterRequestDto(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("email") val email: String,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("password") val password: String
)

data class RegisterResponseDto(
    @SerializedName("message") val message: String?,
    @SerializedName("userId") val userId: Int?
)

data class UserProfileResponseDto(
    @SerializedName("user") val user: UserDto
)

data class VerifyStudentResponseDto(
    @SerializedName("message") val message: String?,
    @SerializedName("es_estudiante") val esEstudiante: Boolean?
)
