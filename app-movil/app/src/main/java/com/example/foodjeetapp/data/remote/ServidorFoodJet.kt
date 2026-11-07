package com.example.foodjeetapp.data.remote

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Dos formas de acceder al mismo backend Node.js + MySQL de este repositorio (backend/).
 * No existen destinos de producción, Docker/Caddy ni servidores configurables externos.
 *
 * 10.0.2.2 es el alias del host para el emulador Android.
 * localhost requiere adb reverse tcp:3000 tcp:3000 en un teléfono físico por USB.
 */
enum class ServidorFoodJet(
    val etiqueta: String,
    val descripcion: String,
    val apiUrl: String,
    val webOrigen: String,
    val origenMapa: String
) {
    NODE_LOCAL_EMULADOR(
        etiqueta = "Backend del repositorio · Emulador",
        descripcion = "Node.js en este PC (10.0.2.2:3000)",
        apiUrl = "http://10.0.2.2:3000/api/",
        webOrigen = "http://10.0.2.2:3000/",
        origenMapa = "http://10.0.2.2:3000/"
    ),
    NODE_LOCAL_USB(
        etiqueta = "Backend del repositorio · Celular USB",
        descripcion = "Node.js en este PC con adb reverse tcp:3000 tcp:3000",
        apiUrl = "http://localhost:3000/api/",
        webOrigen = "http://localhost:3000/",
        origenMapa = "http://localhost:3000/"
    );

    val apiHttpUrl: HttpUrl get() = apiUrl.toHttpUrl()

    /** Resuelve recursos del backend local, sin abrir URLs de servidores externos. */
    fun recurso(ruta: String?): String? {
        val limpia = ruta?.trim().orEmpty()
        if (limpia.isEmpty() || '\\' in limpia) return null
        val absoluta = limpia.toHttpUrlOrNull()
        if (absoluta != null) {
            val local = apiHttpUrl
            return if (absoluta.scheme == local.scheme &&
                absoluta.host == local.host && absoluta.port == local.port
            ) limpia else null
        }
        if (limpia.contains("://") || limpia.startsWith("//")) return null
        val path = limpia.trimStart('/')
        if (path.isBlank() || path.split('/').any { it == "." || it == ".." }) return null
        return webOrigen.trimEnd('/') + "/" + path
    }

    companion object {
        val PREDETERMINADO = NODE_LOCAL_EMULADOR

        /** Migra configuraciones previas sin permitir reconectarse a Docker o producción. */
        fun desdeNombre(nombre: String?): ServidorFoodJet = when (nombre) {
            "NODE_LOCAL_USB", "BACKEND_LOCAL_USB", "LOCAL_USB" -> NODE_LOCAL_USB
            else -> NODE_LOCAL_EMULADOR
        }
    }
}
