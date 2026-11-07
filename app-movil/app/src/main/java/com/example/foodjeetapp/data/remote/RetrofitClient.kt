package com.example.foodjeetapp.data.remote

import android.content.Context
import android.content.pm.ApplicationInfo
import com.example.foodjeetapp.data.local.SessionDataStore
import com.example.foodjeetapp.data.remote.api.FoodJetApiService
import com.example.foodjeetapp.data.remote.interceptor.AuthInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Cliente Retrofit exclusivo para backend/ del mismo repositorio, puerto 3000.
 * Solo alterna entre la ruta de emulador Android y la ruta USB (adb reverse).
 */
object RetrofitClient {
    const val TEMPLATE_API_URL = "http://10.0.2.2:3000/api/"

    @Volatile
    var servidor: ServidorFoodJet = ServidorFoodJet.PREDETERMINADO

    @Volatile
    private var apiServiceInstance: FoodJetApiService? = null

    fun getApiService(context: Context): FoodJetApiService {
        return apiServiceInstance ?: synchronized(this) {
            apiServiceInstance ?: createApiService(
                authInterceptor = AuthInterceptor(SessionDataStore(context.applicationContext)),
                debugLogging = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
            ).also { apiServiceInstance = it }
        }
    }

    internal fun createApiService(
        authInterceptor: Interceptor,
        debugLogging: Boolean = false,
        servidorActual: () -> ServidorFoodJet = { servidor }
    ): FoodJetApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (debugLogging) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
            redactHeader("Cookie")
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            // Evita seguir redirecciones hacia otra API y enviar datos a hosts externos.
            .followRedirects(false)
            .followSslRedirects(false)
            .addInterceptor(ServidorInterceptor(servidorActual))
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .retryOnConnectionFailure(true)
            .build()

        return Retrofit.Builder()
            .baseUrl(TEMPLATE_API_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FoodJetApiService::class.java)
    }
}

/** Reencamina solo peticiones /api del backend local; rechaza otros orígenes. */
class ServidorInterceptor(
    private val servidorActual: () -> ServidorFoodJet
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val original = chain.request()
        val origen = original.url

        val origenPermitido = ServidorFoodJet.entries.any { local ->
            val url = local.apiHttpUrl
            origen.scheme == url.scheme && origen.host == url.host && origen.port == url.port
        }
        if (!origenPermitido || origen.pathSegments.firstOrNull() != "api") {
            throw IOException("Solo se permiten peticiones a la API local de FoodJet (puerto 3000)")
        }

        val destino = servidorActual().apiHttpUrl
        // Conserva exactamente el encodedPath y los query params de la petición.
        val url = origen.newBuilder()
            .scheme(destino.scheme)
            .host(destino.host)
            .port(destino.port)
            .build()

        return chain.proceed(original.newBuilder().url(url).build())
    }
}
