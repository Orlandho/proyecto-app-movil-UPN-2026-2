package com.example.foodjeetapp.data.remote.interceptor

import com.example.foodjeetapp.data.local.SessionDataStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor de OkHttp para inyección automática de cabecera de autenticación Bearer JWT.
 * Cumple con REQ-SEM08-INF-01.
 */
class AuthInterceptor(private val sessionDataStore: SessionDataStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Recuperar el token JWT de manera no bloqueante desde la caché en memoria (Bug 1)
        val token = sessionDataStore.getCachedToken() ?: runBlocking {
            withTimeoutOrNull(250) {
                sessionDataStore.getToken()
            }
        }

        val requestBuilder = original.newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
