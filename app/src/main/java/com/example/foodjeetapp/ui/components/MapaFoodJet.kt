package com.example.foodjeetapp.ui.components

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.foodjeetapp.data.di.ServiceLocator
import com.example.foodjeetapp.data.model.GeoPoint
import com.example.foodjeetapp.data.remote.RetrofitClient
import com.example.foodjeetapp.data.remote.ServidorFoodJet
import com.example.foodjeetapp.ui.theme.FoodJetDark
import com.example.foodjeetapp.ui.theme.FoodJetGray
import com.example.foodjeetapp.ui.theme.FoodJetPrimary
import com.example.foodjeetapp.ui.theme.FoodJetPrimaryDark
import com.example.foodjeetapp.ui.theme.FoodJetSuccess
import com.google.gson.Gson

/** Un punto con nombre para el mapa: marcador con ventana de información. */
data class PuntoMapa(val punto: GeoPoint, val titulo: String, val detalle: String = "")

/** Lo que se dibuja en el mapa (REQ-SEM10-VIS-01). */
data class DatosMapa(
    val restaurante: PuntoMapa? = null,
    val destino: PuntoMapa? = null,
    val repartidor: PuntoMapa? = null,
    val recorrida: List<GeoPoint> = emptyList(),
    val pendiente: List<GeoPoint> = emptyList(),
    /** Área circular de cobertura alrededor de un punto, en metros. */
    val cobertura: Pair<GeoPoint, Double>? = null
)

enum class ModoMapa { SEGUIMIENTO, SELECCION }

/**
 * Keys de Google Maps del servidor activo. Se piden a `GET /api/config` una
 * vez por servidor y se recuerdan: el mapa del checkout y el del seguimiento
 * no repiten la consulta (REQ-SEM07-LOG-03).
 */
object ConfigMapa {
    private var servidor: ServidorFoodJet? = null
    private var keys: List<String>? = null

    suspend fun keys(): List<String> {
        val actual = RetrofitClient.servidor
        keys?.takeIf { servidor == actual }?.let { return it }
        val leidas = ServiceLocator.orderRepository.getPublicConfig().getOrNull()?.keysDeMapa() ?: return emptyList()
        servidor = actual
        keys = leidas
        return leidas
    }
}

private class PuenteMapa(
    private val webView: WebView,
    private val alListo: () -> Unit,
    private val alFallar: (String) -> Unit,
    private val alRechazarKey: () -> Unit,
    private val alCentro: (GeoPoint) -> Unit
) {
    // Los métodos llegan por el hilo del puente de JavaScript: se pasan al principal.
    @JavascriptInterface fun listo() { webView.post(alListo) }
    @JavascriptInterface fun keyRechazada() { webView.post(alRechazarKey) }
    @JavascriptInterface fun noDisponible(motivo: String?) { webView.post { alFallar(motivo ?: "Mapa no disponible") } }
    @JavascriptInterface fun centro(lat: Double, lng: Double) { webView.post { alCentro(GeoPoint(lat, lng)) } }
}

private sealed interface EstadoMapa {
    data object Cargando : EstadoMapa
    data object Listo : EstadoMapa
    data class NoDisponible(val motivo: String) : EstadoMapa
}

/**
 * Mapa interactivo de Google Maps (REQ-SEM10-VIS-01 y REQ-SEM10-VIS-02).
 *
 * Es Google Maps JavaScript en un WebView, el mismo mapa que la web, en vez
 * del SDK de Maps para Android: ese SDK es otra dependencia de Gradle (que
 * AGENTS.md no deja tocar) y exigiría una key nueva restringida a la app. Así
 * se reutiliza la key que entrega el backend en `/api/config`.
 *
 * Si no hay key, Google la rechaza o no hay red, se muestra [RutaEsquematica]:
 * los mismos puntos y la misma ruta, sin fondo de calles.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapaFoodJet(
    datos: DatosMapa,
    modifier: Modifier = Modifier,
    modo: ModoMapa = ModoMapa.SEGUIMIENTO,
    centroInicial: GeoPoint? = null,
    controlador: ControladorMapa? = null,
    /** En modo selección, el punto bajo el pin central cuando el mapa deja de moverse. */
    onCentroCambiado: (GeoPoint) -> Unit = {},
    /** Avisa si hay mapa de Google (true) o solo el esquema (false). */
    onDisponibilidad: (Boolean) -> Unit = {}
) {
    var keys by remember { mutableStateOf<List<String>?>(null) }
    // Posición en la cascada de keys (principal, secundaria). Cambiarla crea otra página.
    var indiceKey by remember { mutableIntStateOf(0) }
    var estado by remember { mutableStateOf<EstadoMapa>(EstadoMapa.Cargando) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val gson = remember { Gson() }
    val servidor = RetrofitClient.servidor
    val alCentro by rememberUpdatedState(onCentroCambiado)
    val alDisponibilidad by rememberUpdatedState(onDisponibilidad)

    LaunchedEffect(servidor) {
        val leidas = ConfigMapa.keys()
        keys = leidas
        indiceKey = 0
        if (leidas.isEmpty()) estado = EstadoMapa.NoDisponible("El servidor no tiene key de Google Maps")
    }
    LaunchedEffect(estado) {
        when (estado) {
            EstadoMapa.Listo -> alDisponibilidad(true)
            is EstadoMapa.NoDisponible -> alDisponibilidad(false)
            EstadoMapa.Cargando -> Unit
        }
    }

    // Un WebView fuera de pantalla sigue ejecutando JavaScript y animaciones:
    // se pausa con la Activity y se destruye al salir del árbol (REQ-SEM04-LOG-01).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webView) {
        val vista = webView
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> vista?.onPause()
                Lifecycle.Event.ON_RESUME -> vista?.onResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(datos, estado, webView) {
        val vista = webView ?: return@LaunchedEffect
        if (estado != EstadoMapa.Listo) return@LaunchedEffect
        vista.evaluateJavascript("FoodJet.datos(${gson.toJson(aJs(datos))});", null)
    }

    Box(modifier = modifier.clip(RoundedCornerShape(14.dp))) {
        val listaKeys = keys
        if (listaKeys != null && indiceKey < listaKeys.size && estado !is EstadoMapa.NoDisponible) key(indiceKey) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "Mapa de la entrega" },
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        addJavascriptInterface(
                            PuenteMapa(
                                webView = this,
                                alListo = { estado = EstadoMapa.Listo },
                                alFallar = { motivo -> estado = EstadoMapa.NoDisponible(motivo) },
                                alRechazarKey = {
                                    // Siguiente key de la cascada, o esquema si no quedan.
                                    if (indiceKey + 1 < listaKeys.size) {
                                        indiceKey++
                                        estado = EstadoMapa.Cargando
                                    } else {
                                        estado = EstadoMapa.NoDisponible("Google Maps rechazó la key del servidor")
                                    }
                                },
                                alCentro = { alCentro(it) }
                            ),
                            "Android"
                        )
                        val html = context.assets.open("mapa_foodjet.html").bufferedReader().use { it.readText() }
                        val centro = centroInicial ?: datos.destino?.punto ?: datos.restaurante?.punto
                            ?: com.example.foodjeetapp.data.model.Geo.LIMA_CENTRO
                        val config = mapOf(
                            "key" to listaKeys[indiceKey],
                            "modo" to if (modo == ModoMapa.SELECCION) "seleccion" else "seguimiento",
                            "centro" to mapOf("lat" to centro.latitud, "lng" to centro.longitud),
                            "zoom" to if (modo == ModoMapa.SELECCION) 17 else 15
                        )
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String?) {
                                view.evaluateJavascript("FoodJet.iniciar(${gson.toJson(config)});", null)
                            }
                        }
                        // El origen tiene que ser uno de los sitios autorizados en la key.
                        loadDataWithBaseURL(servidor.origenMapa, html, "text/html", "utf-8", null)
                        webView = this
                        controlador?.vista = this
                    }
                },
                onRelease = { vista ->
                    if (controlador?.vista === vista) controlador.vista = null
                    vista.removeJavascriptInterface("Android")
                    vista.stopLoading()
                    vista.destroy()
                    webView = null
                }
            )
        } else {
            RutaEsquematica(datos = datos, modifier = Modifier.fillMaxSize())
        }

        if (modo == ModoMapa.SELECCION && estado == EstadoMapa.Listo) {
            // Pin fijo en el centro: se mueve el mapa debajo, como en la web.
            Icon(
                Icons.Default.Place,
                contentDescription = null,
                tint = FoodJetPrimaryDark,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-18).dp)
                    .size(40.dp)
            )
        }

        when (val actual = estado) {
            EstadoMapa.Cargando -> if (keys == null || keys!!.isNotEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(28.dp),
                    color = FoodJetPrimary
                )
            }
            EstadoMapa.Listo -> ControlesMapa(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                onAcercar = { webView?.evaluateJavascript("FoodJet.zoom(1);", null) },
                onAlejar = { webView?.evaluateJavascript("FoodJet.zoom(-1);", null) },
                onCentrar = { webView?.evaluateJavascript("FoodJet.encuadrar();", null) }
            )
            is EstadoMapa.NoDisponible -> Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ) {
                Text(
                    text = "Esquema sin mapa · ${actual.motivo}",
                    fontSize = 10.sp,
                    color = FoodJetGray,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/** Permite mover el mapa desde fuera, por ejemplo al punto del GPS o de una dirección buscada. */
class ControladorMapa {
    internal var vista: WebView? = null

    fun irA(punto: GeoPoint, zoom: Int = 17) {
        vista?.evaluateJavascript("FoodJet.irA(${punto.latitud}, ${punto.longitud}, $zoom);", null)
    }
}

private fun aJs(datos: DatosMapa): Map<String, Any?> {
    fun punto(p: PuntoMapa?) = p?.let {
        mapOf("lat" to it.punto.latitud, "lng" to it.punto.longitud, "titulo" to it.titulo, "detalle" to it.detalle)
    }
    fun ruta(r: List<GeoPoint>) = r.map { listOf(it.latitud, it.longitud) }
    return mapOf(
        "restaurante" to punto(datos.restaurante),
        "destino" to punto(datos.destino),
        "repartidor" to punto(datos.repartidor),
        "recorrida" to ruta(datos.recorrida),
        "pendiente" to ruta(datos.pendiente),
        "cobertura" to datos.cobertura?.let { (centro, radio) ->
            mapOf("lat" to centro.latitud, "lng" to centro.longitud, "radioMetros" to radio)
        }
    )
}

@Composable
private fun ControlesMapa(
    modifier: Modifier,
    onAcercar: () -> Unit,
    onAlejar: () -> Unit,
    onCentrar: () -> Unit
) {
    // Botones de 48 dp: el área táctil mínima de las pautas de accesibilidad.
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            Triple(Icons.Default.Add, "Acercar mapa", onAcercar),
            Triple(Icons.Default.Remove, "Alejar mapa", onAlejar),
            Triple(Icons.Default.CenterFocusStrong, "Centrar en la ruta", onCentrar)
        ).forEach { (icono, descripcion, accion) ->
            FilledTonalIconButton(
                onClick = accion,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            ) {
                Icon(icono, contentDescription = descripcion, tint = FoodJetDark)
            }
        }
    }
}

/**
 * Esquema de la entrega sin mapa de fondo: proyecta local, destino,
 * repartidor y ruta en el rectángulo disponible. Solo dibuja datos que
 * vinieron del backend; si no hay ninguno, lo dice.
 */
@Composable
fun RutaEsquematica(datos: DatosMapa, modifier: Modifier = Modifier) {
    val puntos = listOfNotNull(datos.restaurante?.punto, datos.destino?.punto, datos.repartidor?.punto) +
        datos.recorrida + datos.pendiente
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        if (puntos.isEmpty()) {
            Text(
                "Aún no hay ubicaciones que mostrar",
                fontSize = 12.sp,
                color = FoodJetGray,
                fontWeight = FontWeight.Medium
            )
            return@Box
        }
        val gris = FoodJetGray
        val naranja = FoodJetPrimary
        val oscuro = FoodJetDark
        val verde = FoodJetSuccess
        Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            val minLat = puntos.minOf { it.latitud }
            val maxLat = puntos.maxOf { it.latitud }
            val minLon = puntos.minOf { it.longitud }
            val maxLon = puntos.maxOf { it.longitud }
            val rangoLat = (maxLat - minLat).takeIf { it > 1e-9 } ?: 1e-3
            val rangoLon = (maxLon - minLon).takeIf { it > 1e-9 } ?: 1e-3
            val escala = minOf(size.width / rangoLon.toFloat(), size.height / rangoLat.toFloat())
            val ancho = rangoLon.toFloat() * escala
            val alto = rangoLat.toFloat() * escala
            fun xy(p: GeoPoint) = Offset(
                (size.width - ancho) / 2 + ((p.longitud - minLon).toFloat() * escala),
                (size.height - alto) / 2 + ((maxLat - p.latitud).toFloat() * escala)
            )
            fun trazo(ruta: List<GeoPoint>, color: Color) {
                if (ruta.size < 2) return
                val path = Path().apply {
                    moveTo(xy(ruta.first()).x, xy(ruta.first()).y)
                    ruta.drop(1).forEach { lineTo(xy(it).x, xy(it).y) }
                }
                drawPath(path, color, style = Stroke(width = 8f, cap = StrokeCap.Round))
            }
            trazo(datos.recorrida, gris.copy(alpha = 0.6f))
            trazo(datos.pendiente, naranja)
            datos.restaurante?.let { drawCircle(oscuro, 14f, xy(it.punto)) }
            datos.destino?.let { drawCircle(naranja, 14f, xy(it.punto)) }
            datos.repartidor?.let {
                drawCircle(Color.White, 13f, xy(it.punto))
                drawCircle(verde, 10f, xy(it.punto))
            }
        }
    }
}
