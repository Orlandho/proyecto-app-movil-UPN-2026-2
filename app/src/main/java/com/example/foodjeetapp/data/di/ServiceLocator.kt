package com.example.foodjeetapp.data.di

import android.content.Context
import com.example.foodjeetapp.data.local.FoodJetDatabase
import com.example.foodjeetapp.data.local.SessionDataStore
import com.example.foodjeetapp.data.location.LocationProvider
import com.example.foodjeetapp.data.remote.RetrofitClient
import com.example.foodjeetapp.data.remote.ServidorFoodJet
import com.example.foodjeetapp.data.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Service Locator centralizado para inyección de dependencias limpia y desacoplada.
 * Cumple con REQ-SEM06-LOG-01 y REQ-SEM06-LOG-03.
 *
 * Solo guarda el contexto de aplicación: retener una Activity aquí la
 * filtraría tras cada giro de pantalla (REQ-SEM04-LOG-01).
 */
object ServiceLocator {

    private var appContext: Context? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        // Arranque asíncrono no bloqueante: se sincroniza reactivamente con DataStore
        // sin bloquear el hilo principal durante el primer pase de dibujo de Compose.
        scope.launch {
            try {
                sessionDataStore.servidorFlow.collect { servidorGuardado ->
                    RetrofitClient.servidor = servidorGuardado
                }
            } catch (_: Exception) { }
        }
    }

    private fun requireContext(): Context {
        return appContext ?: throw IllegalStateException("ServiceLocator no ha sido inicializado. Llama a ServiceLocator.initialize(context) primero.")
    }

    val sessionDataStore: SessionDataStore by lazy {
        SessionDataStore(requireContext())
    }

    val database: FoodJetDatabase by lazy {
        FoodJetDatabase.getInstance(requireContext())
    }

    val apiService by lazy {
        RetrofitClient.getApiService(requireContext())
    }

    val locationProvider: LocationProvider by lazy {
        LocationProvider(requireContext())
    }

    val productRepository: ProductRepository by lazy {
        ProductRepositoryImpl(
            apiService = apiService,
            productDao = database.productDao()
        )
    }

    val userRepository: UserRepository by lazy {
        UserRepositoryImpl(
            apiService = apiService,
            sessionDataStore = sessionDataStore
        )
    }

    val orderRepository: OrderRepository by lazy {
        OrderRepositoryImpl(
            apiService = apiService
        )
    }

    /**
     * Pasa a trabajar contra otro servidor. La sesión y el catálogo en caché
     * pertenecen al servidor anterior (otro JWT_SECRET, otros ids), así que
     * se descartan.
     */
    suspend fun cambiarServidor(servidor: ServidorFoodJet) = withContext(Dispatchers.IO) {
        sessionDataStore.guardarServidor(servidor)
        sessionDataStore.clearSession()
        database.productDao().clearAll()
        RetrofitClient.servidor = servidor
    }
}
