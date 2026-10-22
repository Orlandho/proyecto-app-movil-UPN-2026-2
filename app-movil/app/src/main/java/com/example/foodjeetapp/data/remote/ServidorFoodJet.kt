package com.example.foodjeetapp.data.remote

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Servidores contra los que puede trabajar la app.
 *
 * Todos apuntan al proxy Caddy de Pagina-Foodjet (puerto 8081 en local), no a
 * la API directa del 3000: el proxy sirve en el mismo origen la API y los
 * estáticos de la web (por ejemplo los QR de pago de cada local), igual que
 * los ve el navegador.
 *
 * - [apiUrl]: base de Retrofit.
 * - [webOrigen]: desde donde se descargan los estáticos que da la API con
 *   ruta relativa (`/img/qr/...`).
 * - [origenMapa]: origen con el que se carga la página del mapa. La key de
 *   Google Maps está restringida por sitio web (`https://foodjet.asen.pe` y
 *   `http://localhost:8081`), así que el mapa tiene que presentarse con uno
 *   de esos dos orígenes aunque el emulador llegue al PC por 10.0.2.2.
 */
enum class ServidorFoodJet(
    val etiqueta: String,
    val descripcion: String,
    val apiUrl: String,
    val webOrigen: String,
    val origenMapa: String
) {
    PRODUCCION(
        etiqueta = "Producción",
        descripcion = "foodjet.asen.pe (túnel de Cloudflare)",
        apiUrl = "https://foodjet.asen.pe/api/",
        webOrigen = "https://foodjet.asen.pe/",
        origenMapa = "https://foodjet.asen.pe/"
    ),
    LOCAL_EMULADOR(
        etiqueta = "Local · emulador",
        descripcion = "Docker en este PC, visto desde el emulador (10.0.2.2:8081)",
        apiUrl = "http://10.0.2.2:8081/api/",
        webOrigen = "http://10.0.2.2:8081/",
        origenMapa = "http://localhost:8081/"
    ),
    LOCAL_USB(
        etiqueta = "Local · celular por USB",
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
        val PREDETERMINADO = PRODUCCION

        fun desdeNombre(nombre: String?): ServidorFoodJet =
            entries.firstOrNull { it.name == nombre } ?: PREDETERMINADO
    }
}
