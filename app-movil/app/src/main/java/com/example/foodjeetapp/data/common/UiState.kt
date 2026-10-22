package com.example.foodjeetapp.data.common

/**
 * Estado genérico inmutable de la interfaz de usuario para arquitectura MVVM.
 * Cumple con REQ-SEM06-LOG-04 y REQ-SEM06-VIS-02 (renderizado de vistas de carga,
 * contenido, error con reintento y estado vacío).
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val message: String, val cause: Throwable? = null) : UiState<Nothing>
    data object Empty : UiState<Nothing>
}
