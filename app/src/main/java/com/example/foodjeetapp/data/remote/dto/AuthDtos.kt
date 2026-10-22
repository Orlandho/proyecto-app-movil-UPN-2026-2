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
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String = "",
    @SerializedName("rol") val rol: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("es_estudiante") val esEstudiante: Boolean? = null,
    @SerializedName("esEstudiante") val esEstudianteCamel: Boolean? = null
) {
    fun toDomain(): UserProfile {
        val finalName = nombre ?: name ?: if (email.contains("@")) email.substringBefore("@").replaceFirstChar { it.uppercase() } else "Usuario FoodJet"
        val finalRole = rol ?: role ?: "cliente"
        val isStudentFinal = esEstudiante ?: esEstudianteCamel ?: false
        return UserProfile(
            name = finalName,
            email = email,
            phone = telefono ?: phone ?: "",
            isStudent = isStudentFinal,
            isAdmin = finalRole.equals("admin", ignoreCase = true)
        )
    }
}

data class LoginResponseDto(
    @SerializedName("message") val message: String? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("accessToken") val accessToken: String? = null,
    @SerializedName("user") val user: UserDto? = null
)

data class RegisterRequestDto(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("email") val email: String,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("password") val password: String
)

data class RegisterResponseDto(
    @SerializedName("message") val message: String? = null,
    @SerializedName("userId") val userId: Int? = null
)

data class UserProfileResponseDto(
    @SerializedName("user") val user: UserDto
)

data class VerifyStudentResponseDto(
    @SerializedName("message") val message: String? = null,
    @SerializedName("es_estudiante") val esEstudiante: Boolean? = null
)
