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
    @SerializedName("id") val id: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("email") val email: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("telefono") val telefono: String?,
    @SerializedName("es_estudiante") val esEstudiante: Boolean?
) {
    fun toDomain(): UserProfile {
        return UserProfile(
            name = nombre,
            email = email,
            phone = telefono ?: "987654321",
            isStudent = esEstudiante ?: false,
            isAdmin = rol.equals("admin", ignoreCase = true)
        )
    }
}

data class LoginResponseDto(
    @SerializedName("message") val message: String?,
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserDto
)

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
