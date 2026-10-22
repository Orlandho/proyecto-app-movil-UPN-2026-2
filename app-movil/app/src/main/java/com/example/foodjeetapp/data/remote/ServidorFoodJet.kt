package com.example.foodjeetapp.data.remote

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Servidores contra los que puede trabajar la app.
 *
 * Incluye el backend local en Node.js + MySQL (puerto 3000),
 * los contenedores de Docker (puerto 8081 con proxy Caddy)
 * y el entorno de producción (Cloudflare).
 *
 * - [apiUrl]: base de Retrofit.
 * - [webOrigen]: desde donde se descargan los estáticos que da la API con
 *   ruta relativa (`/img/qr/...`).
 * - [origenMapa]: origen con el que se carga la página del mapa.
 */
enum class ServidorFoodJet(
    val etiqueta: String,
    val descripcion: String,
    val apiUrl: String,
    val webOrigen: String,
    val origenMapa: String
) {
    NODE_LOCAL_EMULADOR(
        etiqueta = "Backend MySQL · Emulador (3000)",
        descripcion = "Node.js local en este PC (10.0.2.2:3000)",
        apiUrl = "http://10.0.2.2:3000/api/",
        webOrigen = "http://10.0.2.2:3000/",
        origenMapa = "http://localhost:3000/"
    ),
    NODE_LOCAL_USB(
        etiqueta = "Backend MySQL · Celular USB (3000)",
        descripcion = "Node.js local en este PC con «adb reverse tcp:3000 tcp:3000»",
        apiUrl = "http://localhost:3000/api/",
        webOrigen = "http://localhost:3000/",
        origenMapa = "http://localhost:3000/"
    ),
    PRODUCCION(
        etiqueta = "Producción",
        descripcion = "foodjet.asen.pe (túnel de Cloudflare)",
        apiUrl = "https://foodjet.asen.pe/api/",
        webOrigen = "https://foodjet.asen.pe/",
        origenMapa = "https://foodjet.asen.pe/"
    ),
    LOCAL_EMULADOR(
        etiqueta = "Docker Caddy · Emulador (8081)",
        descripcion = "Docker en este PC, visto desde el emulador (10.0.2.2:8081)",
        apiUrl = "http://10.0.2.2:8081/api/",
        webOrigen = "http://10.0.2.2:8081/",
        origenMapa = "http://localhost:8081/"
    ),
    LOCAL_USB(
        etiqueta = "Docker Caddy · Celular por USB (8081)",
        descripcion = "Docker en este PC con «adb reverse tcp:8081 tcp:8081»",
        apiUrl = "http://localhost:8081/api/",
        webOrigen = "http://localhost:8081/",
        origenMapa = "http://localhost:8081/"
    );

    val apiHttpUrl: HttpUrl get() = apiUrl.toHttpUrl()

    /** URL absoluta de un recurso que la API da con ruta relativa, o null. */
    fun recurso(ruta: String?): String? {
        val limpia = ruta?.trim().orEmpty()
        if (limpia.isEmpty()) return null
        if (limpia.startsWith("http://") || limpia.startsWith("https://")) return limpia
        return webOrigen.trimEnd('/') + "/" + limpia.trimStart('/')
    }

    companion object {
        val PREDETERMINADO = NODE_LOCAL_EMULADOR

        fun desdeNombre(nombre: String?): ServidorFoodJet = when (nombre) {
            "NODE_LOCAL_EMULADOR", "BACKEND_LOCAL_EMULADOR" -> NODE_LOCAL_EMULADOR
            "NODE_LOCAL_USB", "BACKEND_LOCAL_USB" -> NODE_LOCAL_USB
            else -> entries.firstOrNull { it.name == nombre } ?: PREDETERMINADO
        }
    }
}
