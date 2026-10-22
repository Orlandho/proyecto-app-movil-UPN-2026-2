package com.example.foodjeetapp.data.remote

import com.google.gson.JsonParser
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Mensajes de error legibles a partir de las respuestas del backend
 * (REQ-SEM08-LOG-04). El backend responde `{"error": "..."}` en 4xx y 5xx;
 * ese texto ya está pensado para el usuario, así que se muestra tal cual.
 */
object ErroresApi {

    /** El texto de `{"error": "..."}` o `{"message": "..."}`, si lo hay. */
    fun mensajeDelCuerpo(cuerpo: String?): String? {
        if (cuerpo.isNullOrBlank()) return null
        return runCatching {
            val json = JsonParser.parseString(cuerpo).asJsonObject
            listOf("error", "message")
                .firstNotNullOfOrNull { clave -> json.get(clave)?.takeIf { it.isJsonPrimitive }?.asString }
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    fun mensaje(response: Response<*>, accion: String): String {
        val delServidor = runCatching { mensajeDelCuerpo(response.errorBody()?.string()) }.getOrNull()
        if (delServidor != null) return delServidor
        return when (response.code()) {
            401 -> "Tu sesión no es válida o expiró. Inicia sesión de nuevo."
            403 -> "No tienes permiso para $accion."
            404 -> "No se encontró lo que buscabas al $accion."
            in 500..599 -> "El servidor tuvo un problema al $accion (${response.code()}). Intenta más tarde."
            else -> "No se pudo $accion (${response.code()})."
        }
    }

    fun mensajeDeRed(error: Throwable, accion: String): String = when (error) {
        is UnknownHostException -> "Sin conexión con el servidor al $accion. Revisa tu internet o el servidor elegido en Mi cuenta."
        is ConnectException -> "No se pudo conectar con ${RetrofitClient.servidor.etiqueta} al $accion. " +
            "Si es el servidor local, revisa que Docker esté levantado (docker compose up -d)."
        is SocketTimeoutException -> "El servidor tardó demasiado al $accion. Intenta de nuevo."
        is IOException -> "Error de red al $accion. Revisa tu conexión."
        else -> "No se pudo $accion: ${error.localizedMessage ?: error.javaClass.simpleName}"
    }
}
