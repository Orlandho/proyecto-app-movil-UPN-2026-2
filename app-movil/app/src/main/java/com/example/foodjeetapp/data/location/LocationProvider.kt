package com.example.foodjeetapp.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.foodjeetapp.data.model.Geo
import com.example.foodjeetapp.data.model.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/** Una lectura de posición con su radio de precisión en metros, si el proveedor lo da. */
data class LecturaUbicacion(val punto: GeoPoint, val precisionMetros: Double?)

/**
 * Ubicación del dispositivo y geocodificación (REQ-SEM10-LOG-01).
 *
 * Usa el LocationManager del sistema a través de LocationManagerCompat en vez
 * de FusedLocationProviderClient: este último vive en play-services-location,
 * una dependencia que no está en el catálogo de Gradle, y AGENTS.md prohíbe
 * tocar los archivos de Gradle. La política es la misma que la de Fused con
 * PRIORITY_BALANCED_POWER_ACCURACY: se prefiere la red (wifi/antenas, barata
 * en batería y suficiente para encontrar una puerta en ciudad) y solo se
 * recurre al GPS si la red no está disponible.
 *
 * Se pide una única lectura y se cancela al terminar o al salir de la
 * pantalla: no queda ningún listener activo consumiendo batería
 * (REQ-SEM04-INF-02).
 */
class LocationProvider(context: Context) {

    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val executor = Executors.newSingleThreadExecutor()

    fun tienePermisoPreciso(): Boolean = concedido(Manifest.permission.ACCESS_FINE_LOCATION)

    fun tienePermiso(): Boolean =
        tienePermisoPreciso() || concedido(Manifest.permission.ACCESS_COARSE_LOCATION)

    fun ubicacionActivada(): Boolean = LocationManagerCompat.isLocationEnabled(locationManager)

    private fun concedido(permiso: String) =
        ContextCompat.checkSelfPermission(appContext, permiso) == PackageManager.PERMISSION_GRANTED

    /** Proveedor con precisión equilibrada: red primero, GPS si no hay red. */
    private fun proveedorEquilibrado(): String? {
        val habilitados = locationManager.getProviders(true)
        return when {
            LocationManager.NETWORK_PROVIDER in habilitados -> LocationManager.NETWORK_PROVIDER
            LocationManager.GPS_PROVIDER in habilitados && tienePermisoPreciso() -> LocationManager.GPS_PROVIDER
            LocationManager.PASSIVE_PROVIDER in habilitados -> LocationManager.PASSIVE_PROVIDER
            else -> null
        }
    }

    /**
     * Una lectura fresca de la posición, o la última conocida si no llega a
     * tiempo. Null sin permiso, con la ubicación apagada o sin lectura.
     */
    @SuppressLint("MissingPermission")
    suspend fun ubicacionActual(tiempoMaximoMs: Long = 15_000): LecturaUbicacion? {
        if (!tienePermiso() || !ubicacionActivada()) return null
        val proveedor = proveedorEquilibrado() ?: return null

        val fresca = withTimeoutOrNull(tiempoMaximoMs) {
            suspendCancellableCoroutine<Location?> { cont ->
                val cancelacion = CancellationSignal()
                cont.invokeOnCancellation { cancelacion.cancel() }
                LocationManagerCompat.getCurrentLocation(
                    locationManager, proveedor, cancelacion, executor
                ) { location -> if (cont.isActive) cont.resume(location) }
            }
        }
        val lectura = fresca ?: ultimaConocida()
        return lectura?.let {
            LecturaUbicacion(GeoPoint(it.latitude, it.longitude), it.accuracy.takeIf { _ -> it.hasAccuracy() }?.toDouble())
        }
    }

    @SuppressLint("MissingPermission")
    private fun ultimaConocida(): Location? =
        locationManager.getProviders(true)
            .mapNotNull { runCatching { locationManager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }

    /** Dirección legible de un punto, o null si el teléfono no tiene geocodificador. */
    suspend fun direccionDe(punto: GeoPoint): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(appContext, Locale("es", "PE"))
        val resultado = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            withTimeoutOrNull(10_000) {
                suspendCancellableCoroutine<Address?> { cont ->
                    geocoder.getFromLocation(punto.latitud, punto.longitud, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(direcciones: MutableList<Address>) {
                            if (cont.isActive) cont.resume(direcciones.firstOrNull())
                        }

                        override fun onError(mensaje: String?) {
                            if (cont.isActive) cont.resume(null)
                        }
                    })
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocation(punto.latitud, punto.longitud, 1)?.firstOrNull() }.getOrNull()
            }
        }
        return resultado?.let(::textoDe)
    }

    /** Punto de una dirección escrita, buscando dentro de Lima como la web. */
    suspend fun buscarDireccion(texto: String): GeoPoint? {
        if (!Geocoder.isPresent() || texto.isBlank()) return null
        val geocoder = Geocoder(appContext, Locale("es", "PE"))
        val resultado = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            withTimeoutOrNull(10_000) {
                suspendCancellableCoroutine<Address?> { cont ->
                    geocoder.getFromLocationName(
                        texto, 1, Geo.LIMA_SUR, Geo.LIMA_OESTE, Geo.LIMA_NORTE, Geo.LIMA_ESTE,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(direcciones: MutableList<Address>) {
                                if (cont.isActive) cont.resume(direcciones.firstOrNull())
                            }

                            override fun onError(mensaje: String?) {
                                if (cont.isActive) cont.resume(null)
                            }
                        }
                    )
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching {
                    geocoder.getFromLocationName(texto, 1, Geo.LIMA_SUR, Geo.LIMA_OESTE, Geo.LIMA_NORTE, Geo.LIMA_ESTE)
                        ?.firstOrNull()
                }.getOrNull()
            }
        }
        return resultado?.let { GeoPoint(it.latitude, it.longitude) }
    }

    private fun textoDe(direccion: Address): String {
        val calle = listOfNotNull(direccion.thoroughfare, direccion.subThoroughfare).joinToString(" ")
        val partes = listOf(calle, direccion.subLocality ?: direccion.locality)
            .filter { !it.isNullOrBlank() }
        return partes.joinToString(", ").ifBlank { direccion.getAddressLine(0).orEmpty() }
    }
}
