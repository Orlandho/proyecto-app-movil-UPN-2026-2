package com.example.foodjeetapp.data.di

import android.content.Context
import com.example.foodjeetapp.data.local.FoodJetDatabase
import com.example.foodjeetapp.data.local.SessionDataStore
import com.example.foodjeetapp.data.remote.RetrofitClient
import com.example.foodjeetapp.data.repository.*

/**
 * Service Locator centralizado para inyección de dependencias limpia y desacoplada.
 * Cumple con REQ-SEM06-LOG-01 y REQ-SEM06-LOG-03.
 */
object ServiceLocator {

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
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
}
