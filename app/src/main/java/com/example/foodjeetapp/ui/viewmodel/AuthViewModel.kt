package com.example.foodjeetapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodjeetapp.data.di.ServiceLocator
import com.example.foodjeetapp.data.model.UserProfile
import com.example.foodjeetapp.data.repository.UserRepository
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

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Observar la sesión persistida en DataStore
        viewModelScope.launch {
            userRepository.getCurrentUserStream().collect { profile ->
                _currentUser.value = profile
                _isLoggedIn.value = profile != null
            }
        }
    }

    fun login(email: String, password: String, onSuccess: (String) -> Unit, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _errorMessage.value = null
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
            _errorMessage.value = null
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
        }
    }

    fun verifyStudent(onSuccess: () -> Unit, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = userRepository.updateStudentStatus(true)
            result.onSuccess {
                _currentUser.value = _currentUser.value?.copy(isStudent = true)
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "No se pudo verificar el estado de estudiante")
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            userRepository.logout()
            _isLoggedIn.value = false
            _currentUser.value = null
            onSuccess()
        }
    }
}
