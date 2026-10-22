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
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP centralizado y constructor de Retrofit.
 * Cumple con REQ-SEM08-LOG-01, REQ-SEM07-INF-01 y REQ-SEM08-INF-01.
 *
 * Retrofit se construye una sola vez con la URL de producción; el servidor
 * real lo decide [ServidorInterceptor] en cada petición, así que cambiar de
 * servidor desde "Mi cuenta" no obliga a reiniciar la app.
 */
object RetrofitClient {

    /** Servidor activo. Lo fija [com.example.foodjeetapp.data.di.ServiceLocator] al arrancar. */
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
            // BASIC registra método, URL y código, pero no cuerpos: contraseñas
            // y tokens no deben llegar a Logcat. En release no se registra
            // nada, que además ahorra CPU (REQ-SEM01-LOG-01).
            level = if (debugLogging) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
            redactHeader("Cookie")
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(ServidorInterceptor(servidorActual))
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .retryOnConnectionFailure(true)
            .build()

        return Retrofit.Builder()
            .baseUrl(ServidorFoodJet.PREDETERMINADO.apiUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FoodJetApiService::class.java)
    }
}

/**
 * Reescribe esquema, host, puerto y prefijo de cada petición hacia el
 * servidor elegido, conservando la ruta relativa a `/api/` y la consulta.
 */
class ServidorInterceptor(private val servidorActual: () -> ServidorFoodJet) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val original = chain.request()
        val base = ServidorFoodJet.PREDETERMINADO.apiHttpUrl
        if (original.url.host != base.host) return chain.proceed(original)

        val destino = servidorActual().apiHttpUrl
        val prefijoBase = base.pathSegments.filter { it.isNotEmpty() }
        val relativos = original.url.pathSegments.drop(prefijoBase.size)
        val url = original.url.newBuilder()
            .scheme(destino.scheme)
            .host(destino.host)
            .port(destino.port)
            .encodedPath("/")
            .apply {
                destino.pathSegments.filter { it.isNotEmpty() }.forEach { addPathSegment(it) }
                relativos.forEach { addPathSegment(it) }
            }
            .build()
        return chain.proceed(original.newBuilder().url(url).build())
    }
}
