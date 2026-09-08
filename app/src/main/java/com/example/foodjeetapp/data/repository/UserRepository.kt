package com.example.foodjeetapp.data.repository

import com.example.foodjeetapp.data.local.SessionDataStore
import com.example.foodjeetapp.data.model.UserProfile
import com.example.foodjeetapp.data.remote.api.FoodJetApiService
import com.example.foodjeetapp.data.remote.dto.LoginRequestDto
import com.example.foodjeetapp.data.remote.dto.RegisterRequestDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

/**
 * Contrato de repositorio para autenticación y perfil de usuario de FoodJet.
 * Cumple con REQ-SEM06-LOG-03 (Patrón Repository).
 */
interface UserRepository {
    fun getCurrentUserStream(): Flow<UserProfile?>
    suspend fun getCurrentUser(): UserProfile?
    suspend fun login(email: String, password: String): Result<UserProfile>
    suspend fun register(name: String, email: String, phone: String, password: String): Result<UserProfile>
    suspend fun updateStudentStatus(isStudent: Boolean): Result<Unit>
    suspend fun logout(): Result<Unit>
}

/**
 * Implementación de producción para el repositorio de usuario y autenticación.
 * Integra Retrofit 2 para autenticación contra el backend Node.js y DataStore para persistencia de sesión.
 * Cumple con REQ-SEM05-INF-01, REQ-SEM08-LOG-01, REQ-SEM08-LOG-03 y REQ-SEM08-LOG-04.
 */
class UserRepositoryImpl(
    private val apiService: FoodJetApiService,
    private val sessionDataStore: SessionDataStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : UserRepository {

    override fun getCurrentUserStream(): Flow<UserProfile?> = sessionDataStore.userProfileFlow

    override suspend fun getCurrentUser(): UserProfile? = sessionDataStore.userProfileFlow.firstOrNull()

    override suspend fun login(email: String, password: String): Result<UserProfile> = withContext(ioDispatcher) {
        try {
            val response = apiService.login(LoginRequestDto(email = email.trim(), password = password))
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                // Persistir sesión y token JWT en Jetpack DataStore (REQ-SEM05-INF-01)
                sessionDataStore.saveSession(
                    token = data.token,
                    userId = data.user.id,
                    name = data.user.nombre,
                    email = data.user.email,
                    phone = data.user.telefono,
                    isStudent = data.user.esEstudiante ?: false,
                    isAdmin = data.user.rol.equals("admin", ignoreCase = true)
                )
                Result.success(data.user.toDomain())
            } else {
                val errorMsg = if (response.code() == 401) {
                    "Credenciales incorrectas. Verifica tu correo y contraseña."
                } else {
                    "Error al iniciar sesión (${response.code()})"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión al servidor: ${e.localizedMessage}", e))
        }
    }

    override suspend fun register(
        name: String,
        email: String,
        phone: String,
        password: String
    ): Result<UserProfile> = withContext(ioDispatcher) {
        try {
            val response = apiService.register(
                RegisterRequestDto(
                    nombre = name.trim(),
                    email = email.trim(),
                    telefono = phone.trim(),
                    password = password
                )
            )
            if (response.isSuccessful) {
                // Iniciar sesión inmediatamente para obtener el token JWT
                login(email, password)
            } else {
                val errorMsg = if (response.code() == 409) {
                    "El correo ya se encuentra registrado."
                } else {
                    "Error al crear la cuenta (${response.code()})"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión al servidor: ${e.localizedMessage}", e))
        }
    }

    override suspend fun updateStudentStatus(isStudent: Boolean): Result<Unit> = withContext(ioDispatcher) {
        try {
            sessionDataStore.updateStudentStatus(isStudent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> = withContext(ioDispatcher) {
        try {
            sessionDataStore.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
