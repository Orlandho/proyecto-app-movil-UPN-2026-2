package com.example.foodjeetapp.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.foodjeetapp.MainActivity
import com.example.foodjeetapp.R
import com.example.foodjeetapp.data.model.GeoPoint

/**
 * Intents de la app (REQ-SEM04-LOG-02).
 *
 * - Explícitos: abren una pantalla concreta de FoodJet nombrando su clase
 *   ([MainActivity]); los usa el acceso directo "Seguir pedido".
 * - Implícitos: piden al sistema "abre esta web", "muestra este punto",
 *   "comparte este texto", y Android elige la app (navegador, Google Maps,
 *   WhatsApp...). Si no hay ninguna instalada se devuelve false en vez de
 *   cerrar la app con ActivityNotFoundException.
 */
object IntentsFoodJet {

    const val EXTRA_PEDIDO_ID = "com.example.foodjeetapp.extra.PEDIDO_ID"

    /** Intent explícito hacia el seguimiento de un pedido. */
    fun seguirPedido(context: Context, pedidoId: Int): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(EXTRA_PEDIDO_ID, pedidoId)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    /** Pedido que pide abrir un Intent recibido por MainActivity, o null. */
    fun pedidoDe(intent: Intent?): Int? =
        intent?.getIntExtra(EXTRA_PEDIDO_ID, -1)?.takeIf { it > 0 }

    /**
     * Publica (o actualiza) el acceso directo "Seguir pedido #N" que aparece
     * al mantener pulsado el icono de la app. Lleva el Intent explícito.
     */
    fun publicarAccesoDirecto(context: Context, pedidoId: Int) {
        val acceso = ShortcutInfoCompat.Builder(context, "seguir-pedido")
            .setShortLabel("Pedido #$pedidoId")
            .setLongLabel("Seguir pedido #$pedidoId")
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(seguirPedido(context, pedidoId))
            .build()
        runCatching { ShortcutManagerCompat.pushDynamicShortcut(context, acceso) }
    }

    fun quitarAccesoDirecto(context: Context) {
        runCatching { ShortcutManagerCompat.removeDynamicShortcuts(context, listOf("seguir-pedido")) }
    }

    fun abrirWeb(context: Context, url: String): Boolean =
        lanzar(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    /** Abre el punto en la app de mapas instalada (Google Maps, Waze...). */
    fun abrirEnMapas(context: Context, punto: GeoPoint, etiqueta: String): Boolean {
        val coordenadas = "${punto.latitud},${punto.longitud}"
        val uri = Uri.parse("geo:$coordenadas?q=$coordenadas(${Uri.encode(etiqueta)})")
        return lanzar(context, Intent(Intent.ACTION_VIEW, uri)) ||
            abrirWeb(context, "https://www.google.com/maps/search/?api=1&query=$coordenadas")
    }

    fun compartir(context: Context, asunto: String, texto: String): Boolean {
        val envio = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, asunto)
            .putExtra(Intent.EXTRA_TEXT, texto)
        return lanzar(context, Intent.createChooser(envio, "Compartir pedido"))
    }

    /** Ajustes de la app, para volver a conceder un permiso denegado para siempre. */
    fun abrirAjustesDeLaApp(context: Context): Boolean =
        lanzar(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        )

    fun abrirAjustesDeUbicacion(context: Context): Boolean =
        lanzar(context, Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))

    private fun lanzar(context: Context, intent: Intent): Boolean = try {
        if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
