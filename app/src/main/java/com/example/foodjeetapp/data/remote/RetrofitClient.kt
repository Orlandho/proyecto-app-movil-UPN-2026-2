package com.example.foodjeetapp.data.remote

import android.content.Context
import com.example.foodjeetapp.data.local.SessionDataStore
import com.example.foodjeetapp.data.remote.api.FoodJetApiService
import com.example.foodjeetapp.data.remote.interceptor.AuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP centralizado y constructor de Retrofit.
 * Cumple con REQ-SEM08-LOG-01, REQ-SEM07-INF-01 y REQ-SEM08-INF-01.
 */
object RetrofitClient {

    /**
     * URL base para el emulador de Android Studio conectando al contenedor Node.js en la PC host.
     * En caso de dispositivo físico con reverse proxy por ADB, se usa "http://localhost:3000/api/".
     */
    var BASE_URL: String = "http://10.0.2.2:3000/api/"

    @Volatile
    private var apiServiceInstance: FoodJetApiService? = null

    fun getApiService(context: Context): FoodJetApiService {
        return apiServiceInstance ?: synchronized(this) {
            val sessionDataStore = SessionDataStore(context.applicationContext)

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(AuthInterceptor(sessionDataStore))
                .addInterceptor(loggingInterceptor)
                .retryOnConnectionFailure(true)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(FoodJetApiService::class.java)
            apiServiceInstance = service
            service
        }
    }
}
