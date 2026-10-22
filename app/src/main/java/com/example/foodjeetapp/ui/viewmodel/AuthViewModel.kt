package com.example.foodjeetapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodjeetapp.data.di.ServiceLocator
import com.example.foodjeetapp.data.model.UserProfile
import com.example.foodjeetapp.data.remote.RetrofitClient
import com.example.foodjeetapp.data.remote.ServidorFoodJet
import com.example.foodjeetapp.data.repository.UserRepository
import com.example.foodjeetapp.data.repository.ValidacionSesion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la gestión de autenticación y estado del usuario en tiempo real.
 * Cumple con REQ-SEM06-LOG-01 y REQ-SEM06-LOG-02.
 */
class AuthViewModel(
    private val userRepository: UserRepository = ServiceLocator.userRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _servidor = MutableStateFlow(RetrofitClient.servidor)
    val servidor: StateFlow<ServidorFoodJet> = _servidor.asStateFlow()

    /** El servidor rechazó la sesión guardada (token caducado o de otro servidor). */
    private val _sesionRechazada = MutableStateFlow(false)
    val sesionRechazada: StateFlow<Boolean> = _sesionRechazada.asStateFlow()

    init {
        // Observar la sesión persistida en DataStore
        viewModelScope.launch {
            userRepository.getCurrentUserStream().collect { profile ->
                _currentUser.value = profile
                _isLoggedIn.value = profile != null
            }
        }
        viewModelScope.launch {
            if (userRepository.validarSesion() == ValidacionSesion.RECHAZADA) _sesionRechazada.value = true
        }
    }

    fun sesionRechazadaAtendida() {
        _sesionRechazada.value = false
    }

    /** Cambia de servidor. Cierra la sesión: el token de un servidor no vale en otro. */
    fun cambiarServidor(nuevo: ServidorFoodJet, onDone: () -> Unit) {
        if (nuevo == _servidor.value) return
        viewModelScope.launch {
            ServiceLocator.cambiarServidor(nuevo)
            _servidor.value = nuevo
            _currentUser.value = null
            _isLoggedIn.value = false
            onDone()
        }
    }

    fun login(email: String, password: String, onSuccess: (String) -> Unit, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = userRepository.login(email, password)
                result.onSuccess { profile ->
                    _currentUser.value = profile
                    _isLoggedIn.value = true
                    onSuccess(profile.name)
                }.onFailure { error ->
                    val msg = error.message ?: "Error al iniciar sesión"
                    _errorMessage.value = msg
                    onError(msg)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(
        name: String,
        email: String,
        phone: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = userRepository.register(name, email, phone, password)
                result.onSuccess { profile ->
                    _currentUser.value = profile
                    _isLoggedIn.value = true
                    onSuccess()
                }.onFailure { error ->
                    val msg = error.message ?: "Error al registrar la cuenta"
                    _errorMessage.value = msg
                    onError(msg)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun verifyStudent(onSuccess: () -> Unit, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = userRepository.updateStudentStatus(true)
            result.onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "No se pudo verificar el carnet") }
        }
    }

    fun logout(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            userRepository.logout()
            _currentUser.value = null
            _isLoggedIn.value = false
            onSuccess()
        }
    }
}
