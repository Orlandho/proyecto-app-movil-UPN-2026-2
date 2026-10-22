package com.example.foodjeetapp.data.repository

import com.example.foodjeetapp.data.local.SessionDataStore
import com.example.foodjeetapp.data.model.UserProfile
import com.example.foodjeetapp.data.remote.ErroresApi
import com.example.foodjeetapp.data.remote.RetrofitClient
import com.example.foodjeetapp.data.remote.api.FoodJetApiService
import com.example.foodjeetapp.data.remote.dto.LoginRequestDto
import com.example.foodjeetapp.data.remote.dto.RegisterRequestDto
import com.example.foodjeetapp.data.remote.dto.UserDto
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
    suspend fun validarSesion(): ValidacionSesion
}

enum class ValidacionSesion { SIN_SESION, VALIDA, RECHAZADA, SIN_CONEXION }

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

    /**
     * Comprueba con `GET /api/auth/me` que el token guardado siga valiendo
     * (caduca a las 24 h y deja de valer si se rota JWT_SECRET). Si el
     * servidor lo rechaza se cierra la sesión; si responde, se refresca el
     * perfil con lo que dice el servidor. Sin red se conserva la sesión.
     */
    override suspend fun validarSesion(): ValidacionSesion = withContext(ioDispatcher) {
        val token = sessionDataStore.getToken() ?: return@withContext ValidacionSesion.SIN_SESION
        if (token == "demo_offline_token") {
            sessionDataStore.clearSession()
            return@withContext ValidacionSesion.RECHAZADA
        }
        try {
            val response = apiService.getProfile()
            val user = response.body()?.user
            when {
                response.isSuccessful && user != null -> {
                    sessionDataStore.saveSession(
                        token = token,
                        userId = user.id,
                        name = user.nombre ?: user.nameAlternate ?: "Usuario FoodJet",
                        email = user.email,
                        phone = user.telefono,
                        isStudent = user.esEstudiante ?: false,
                        isAdmin = (user.rol ?: user.roleAlternate).equals("admin", ignoreCase = true)
                    )
                    ValidacionSesion.VALIDA
                }
                response.code() in setOf(401, 403, 404) -> {
                    sessionDataStore.clearSession()
                    ValidacionSesion.RECHAZADA
                }
                else -> ValidacionSesion.SIN_CONEXION
            }
        } catch (e: Exception) {
            ValidacionSesion.SIN_CONEXION
        }
    }

    override suspend fun getCurrentUser(): UserProfile? = sessionDataStore.userProfileFlow.firstOrNull()

    override suspend fun login(email: String, password: String): Result<UserProfile> = withContext(ioDispatcher) {
        val trimmedEmail = email.trim()
        try {
            val response = apiService.login(LoginRequestDto(email = trimmedEmail, password = password))
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val user = data.user ?: UserDto(id = 1, nombre = trimmedEmail.substringBefore("@"), email = trimmedEmail)
                val token = data.effectiveToken
                // Persistir sesión y token JWT en Jetpack DataStore (REQ-SEM05-INF-01)
                sessionDataStore.saveSession(
                    token = token,
                    userId = user.id,
                    name = user.nombre ?: user.nameAlternate ?: "Usuario FoodJet",
                    email = user.email,
                    phone = user.telefono,
                    isStudent = user.esEstudiante ?: false,
                    isAdmin = (user.rol ?: user.roleAlternate).equals("admin", ignoreCase = true)
                )
                Result.success(user.toDomain())
            } else {
                val errorMsg = if (response.code() == 401) {
                    "Credenciales incorrectas. Verifica tu correo y contraseña."
                } else {
                    ErroresApi.mensaje(response, "iniciar sesión")
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val servidorActual = RetrofitClient.servidor.etiqueta
            val causa = e.localizedMessage ?: "Conexión rechazada"
            Result.failure(
                Exception(
                    "No se pudo conectar con el servidor ($servidorActual): $causa. " +
                    "Asegúrate de que el backend esté ejecutándose o cambia de servidor en la pantalla de inicio."
                )
            )
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
                    ErroresApi.mensaje(response, "crear la cuenta")
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val servidorActual = RetrofitClient.servidor.etiqueta
            val causa = e.localizedMessage ?: "Conexión rechazada"
            Result.failure(
                Exception(
                    "No se pudo conectar con el servidor ($servidorActual): $causa. " +
                    "Verifica que el backend esté ejecutándose."
                )
            )
        }
    }

    override suspend fun updateStudentStatus(isStudent: Boolean): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (isStudent) {
                val response = apiService.verifyStudent()
                if (response.isSuccessful || response.code() == 400) {
                    sessionDataStore.updateStudentStatus(true)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(ErroresApi.mensaje(response, "verificar condición de estudiante")))
                }
            } else {
                sessionDataStore.updateStudentStatus(false)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión al actualizar condición de estudiante: ${e.localizedMessage ?: "Sin conexión"}"))
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
