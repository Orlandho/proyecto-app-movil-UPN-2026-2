package com.example.foodjeetapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodjeetapp.data.model.UserProfile
import com.example.foodjeetapp.data.repository.UserRepository
import com.example.foodjeetapp.data.repository.UserRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la gestión de autenticación y estado del usuario.
 * Cumple con REQ-SEM06-LOG-01 y REQ-SEM06-LOG-02.
 */
class AuthViewModel(
    private val userRepository: UserRepository = UserRepositoryImpl()
) : ViewModel() {

    private val _currentUser = MutableStateFlow<UserProfile?>(
        UserProfile(
            name = "Orlando Dorival",
            email = "orlando@foodjet.com",
            phone = "987654321",
            isStudent = true,
            isAdmin = false
        )
    )
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun login(email: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            userRepository.login(email, "password123")
            val profile = UserProfile(
                name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                email = email,
                phone = "987654321",
                isStudent = true
            )
            _currentUser.value = profile
            _isLoggedIn.value = true
            onSuccess(profile.name)
        }
    }

    fun register(name: String, email: String, phone: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            userRepository.register(name, email, phone, "password123")
            _currentUser.value = UserProfile(
                name = name,
                email = email,
                phone = phone,
                isStudent = false
            )
            _isLoggedIn.value = true
            onSuccess()
        }
    }

    fun verifyStudent(onSuccess: () -> Unit) {
        viewModelScope.launch {
            userRepository.updateStudentStatus(true)
            _currentUser.value = _currentUser.value?.copy(isStudent = true)
            onSuccess()
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
