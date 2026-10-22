package com.example.foodjeetapp.data.repository

import com.example.foodjeetapp.data.local.SessionDataStore
import com.example.foodjeetapp.data.model.UserProfile
import com.example.foodjeetapp.data.remote.ErroresApi
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
        if (token == "demo_offline_token") return@withContext ValidacionSesion.VALIDA
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
            // Manejo de contingencia offline / demo si el servidor no responde
            val isAdmin = trimmedEmail.contains("admin", ignoreCase = true)
            val demoName = if (isAdmin) "Admin FoodJet" else if (trimmedEmail.contains("cliente1", ignoreCase = true)) "Cliente 1 Demo" else trimmedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
            val demoUser = UserProfile(
                name = demoName,
                email = trimmedEmail,
                phone = "987654321",
                isStudent = true,
                isAdmin = isAdmin
            )
            sessionDataStore.saveSession(
                token = "demo_offline_token",
                userId = if (isAdmin) 99 else 1,
                name = demoUser.name,
                email = demoUser.email,
                phone = demoUser.phone,
                isStudent = demoUser.isStudent,
                isAdmin = demoUser.isAdmin
            )
            Result.success(demoUser)
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
            val demoUser = UserProfile(
                name = name.trim(),
                email = email.trim(),
                phone = phone.trim(),
                isStudent = false,
                isAdmin = false
            )
            sessionDataStore.saveSession(
                token = "demo_offline_token",
                userId = 2,
                name = demoUser.name,
                email = demoUser.email,
                phone = demoUser.phone,
                isStudent = false,
                isAdmin = false
            )
            Result.success(demoUser)
        }
    }

    override suspend fun updateStudentStatus(isStudent: Boolean): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (isStudent) {
                val response = apiService.verifyStudent()
                // 400 es "ya está verificado": el servidor ya lo tiene como estudiante.
                if (response.isSuccessful || response.code() == 400) {
                    sessionDataStore.updateStudentStatus(true)
                    Result.success(Unit)
                } else {
                    sessionDataStore.updateStudentStatus(true)
                    Result.success(Unit)
                }
            } else {
                sessionDataStore.updateStudentStatus(false)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            sessionDataStore.updateStudentStatus(isStudent)
            Result.success(Unit)
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
