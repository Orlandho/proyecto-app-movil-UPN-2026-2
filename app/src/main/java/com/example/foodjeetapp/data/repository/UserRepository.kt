package com.example.foodjeetapp.data.repository

import com.example.foodjeetapp.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
 * Mantiene la sesión en memoria para la interfaz actual y declara los puntos de conexión a Firebase y DataStore.
 */
class UserRepositoryImpl : UserRepository {

    // TODO: (REQ-SEM09-LOG-01) Integrar Firebase Authentication para registro, inicio de sesión seguro y gestión de tokens.
    // TODO: (REQ-SEM05-INF-01) Implementar Jetpack DataStore Preferences para la persistencia asíncrona de credenciales y estado del usuario.
    // TODO: (REQ-SEM09-INF-02) Validar reglas de seguridad en Firebase basadas en el identificador único UID del usuario.

    private val _currentUserFlow = MutableStateFlow<UserProfile?>(
        UserProfile(
            name = "Orlando Dorival",
            email = "orlando@foodjet.com",
            phone = "987654321",
            isStudent = true,
            isAdmin = false
        )
    )

    override fun getCurrentUserStream(): Flow<UserProfile?> = _currentUserFlow.asStateFlow()

    override suspend fun getCurrentUser(): UserProfile? = _currentUserFlow.value

    override suspend fun login(email: String, password: String): Result<UserProfile> {
        // TODO: Invocar FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
        val user = UserProfile(name = email.substringBefore("@"), email = email, phone = "987654321", isStudent = true)
        _currentUserFlow.value = user
        return Result.success(user)
    }

    override suspend fun register(name: String, email: String, phone: String, password: String): Result<UserProfile> {
        // TODO: Invocar FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
        val user = UserProfile(name = name, email = email, phone = phone, isStudent = false)
        _currentUserFlow.value = user
        return Result.success(user)
    }

    override suspend fun updateStudentStatus(isStudent: Boolean): Result<Unit> {
        // TODO: Persistir nuevo estado de carnet en DataStore Preferences y Firestore
        _currentUserFlow.update { it?.copy(isStudent = isStudent) }
        return Result.success(Unit)
    }

    override suspend fun logout(): Result<Unit> {
        // TODO: Invocar FirebaseAuth.getInstance().signOut() y limpiar DataStore
        _currentUserFlow.value = null
        return Result.success(Unit)
    }
}
