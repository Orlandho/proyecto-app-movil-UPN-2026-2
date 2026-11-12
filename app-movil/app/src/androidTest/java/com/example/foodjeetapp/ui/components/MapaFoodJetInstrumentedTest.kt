package com.example.foodjeetapp.ui.components

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.foodjeetapp.MainActivity
import com.example.foodjeetapp.data.model.Geo
import com.example.foodjeetapp.data.model.GeoPoint
import com.example.foodjeetapp.data.remote.RetrofitClient
import com.example.foodjeetapp.data.remote.ServidorFoodJet
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class MapaFoodJetInstrumentedTest {

    private lateinit var servidorOriginal: ServidorFoodJet

    @Before
    fun setup() {
        servidorOriginal = RetrofitClient.servidor
        RetrofitClient.servidor = ServidorFoodJet.NODE_LOCAL_EMULADOR
        ConfigMapa.reiniciarParaPruebas()
    }

    @After
    fun teardown() {
        RetrofitClient.servidor = servidorOriginal
        ConfigMapa.reiniciarParaPruebas()
    }

    @Test
    fun mapaVisible_gestoPinchZoomYArrastre_noScrolleanPadre() {
        val scrollState = ScrollState(0)
        val controlador = ControladorMapa()
        var disponible: Boolean? = null
        val ultimoCentro = AtomicReference<GeoPoint?>(null)
        val listoLatch = CountDownLatch(1)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        Spacer(Modifier.height(120.dp))
                        MapaFoodJet(
                            datos = DatosMapa(
                                restaurante = PuntoMapa(GeoPoint(-12.1219, -77.0297), "Local", "Miraflores")
                            ),
                            modo = ModoMapa.SELECCION,
                            centroInicial = GeoPoint(-12.1310, -77.0220),
                            controlador = controlador,
                            onCentroCambiado = { ultimoCentro.set(it) },
                            onDisponibilidad = {
                                disponible = it
                                if (it) listoLatch.countDown()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                        )
                        Spacer(Modifier.height(1200.dp))
                    }
                }
            }

            assertTrue("El mapa de Google debe llegar a Listo", listoLatch.await(15, TimeUnit.SECONDS))
            assertEquals(true, disponible)

            val wv = esperarWebView(scenario)
            assertNotNull("MapaWebView debe estar montado", wv)
            val bounds = IntArray(2)
            var width = 0
            var height = 0
            scenario.onActivity {
                wv!!.getLocationOnScreen(bounds)
                width = wv.width
                height = wv.height
            }
            assertTrue("WebView width debe ser > 0, fue $width", width > 100)
            assertTrue("WebView height debe ser > 0, fue $height", height > 100)

            val domDims = evaluarJs(scenario, wv!!, "[document.getElementById('mapa').clientWidth, document.getElementById('mapa').clientHeight]")
            assertTrue("DOM #mapa debe tener dimensiones > 0, fue $domDims", !domDims.contains("[0") && !domDims.contains(",0]"))

            val zoomInicial = evaluarJs(scenario, wv, "FoodJet.zoomActual()").toDoubleOrNull()
            assertNotNull("zoomActual debe ser numérico", zoomInicial)

            // 1. Pinch-out (dos dedos separándose) en el centro del mapa
            val cx = bounds[0] + width / 2f
            val cy = bounds[1] + height / 2f
            inyectarPinchOut(cx, cy, radioInicial = 70f, radioFinal = 320f)
            SystemClock.sleep(1500)

            assertEquals("El scroll padre no debe moverse durante pinch-zoom", 0, scrollState.value)
            val zoomDespuesPinch = evaluarJs(scenario, wv, "FoodJet.zoomActual()").toDoubleOrNull()
            assertNotNull(zoomDespuesPinch)
            assertTrue(
                "Pinch-out debe acercar el zoom (antes=$zoomInicial, después=$zoomDespuesPinch)",
                zoomDespuesPinch!! > zoomInicial!!
            )

            // 2. Arrastre vertical dentro del mapa: mueve el mapa sin scrollear el Column padre
            val centroAntes = ultimoCentro.get()
            inyectarArrastre(cx, cy + 80f, cx, cy - 80f)
            SystemClock.sleep(1500)

            assertEquals("El scroll padre no debe moverse durante arrastre vertical del mapa", 0, scrollState.value)
            val centroDespues = ultimoCentro.get()
            assertNotNull("onCentroCambiado debe reportar nuevo centro", centroDespues)
            if (centroAntes != null) {
                assertTrue(
                    "El centro del mapa debe haberse movido tras arrastrar",
                    Geo.distanciaMetros(centroAntes, centroDespues!!) > 10.0
                )
            }
        }
    }

    @Test
    fun cascadaDeKeys_y_cambioDeServidor_recuperanMapaListoYEjecutanIrAPendiente() {
        // Key 0 inválida (fuerza gm_authFailure) + keys válidas desde /api/config
        ConfigMapa.reiniciarParaPruebas(manifestKeyPrueba = "AIzaInvalidKeyForCascadeTest0000")
        val controlador = ControladorMapa()
        val objetivo = GeoPoint(37.4220, -122.0840)
        controlador.irA(objetivo, zoom = 16)

        var servidorState by mutableStateOf(ServidorFoodJet.NODE_LOCAL_EMULADOR)
        val listos = java.util.concurrent.CopyOnWriteArrayList<Boolean>()
        val primerListoLatch = CountDownLatch(1)
        val segundoListoLatch = CountDownLatch(1)
        var esperandoSegundo = false

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    RetrofitClient.servidor = servidorState
                    Box(Modifier.fillMaxSize()) {
                        MapaFoodJet(
                            datos = DatosMapa(
                                restaurante = PuntoMapa(GeoPoint(-12.1219, -77.0297), "Local", "Miraflores")
                            ),
                            modo = ModoMapa.SELECCION,
                            centroInicial = GeoPoint(-12.1310, -77.0220),
                            controlador = controlador,
                            onDisponibilidad = { ok ->
                                listos.add(ok)
                                if (ok) {
                                    if (!esperandoSegundo) primerListoLatch.countDown()
                                    else segundoListoLatch.countDown()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        )
                    }
                }
            }

            assertTrue("Cascada de keys debe caer a la key válida del backend", primerListoLatch.await(20, TimeUnit.SECONDS))
            SystemClock.sleep(1000)

            val wv1 = esperarWebView(scenario)!!
            val zoomTrasIrA = evaluarJs(scenario, wv1, "FoodJet.zoomActual()").toDoubleOrNull()
            assertEquals("ControladorMapa.irA pendiente debe aplicar zoom=16 tras cascada", 16.0, zoomTrasIrA ?: -1.0, 0.5)

            // Ahora cambiamos el servidor en vivo (sin cambiar indiceKey tras reiniciar)
            ConfigMapa.reiniciarParaPruebas()
            esperandoSegundo = true
            scenario.onActivity {
                RetrofitClient.servidor = ServidorFoodJet.NODE_LOCAL_USB
                servidorState = ServidorFoodJet.NODE_LOCAL_USB
            }

            assertTrue("Al cambiar de servidor el mapa debe recargarse y volver a Listo", segundoListoLatch.await(20, TimeUnit.SECONDS))
        }
    }

    @Test
    fun seguimientoZoomClamp_y_fallbackAEsquemaCuandoTodasLasKeysFallan() {
        val listoLatch = CountDownLatch(1)
        var disponible: Boolean? = null

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    Box(Modifier.fillMaxSize()) {
                        MapaFoodJet(
                            datos = DatosMapa(
                                restaurante = PuntoMapa(GeoPoint(-12.1219, -77.0297), "Local", "Miraflores"),
                                destino = PuntoMapa(GeoPoint(-12.1310, -77.0220), "Entrega", "Av. Principal"),
                                pendiente = listOf(GeoPoint(-12.1219, -77.0297), GeoPoint(-12.1310, -77.0220))
                            ),
                            modo = ModoMapa.SEGUIMIENTO,
                            onDisponibilidad = {
                                disponible = it
                                if (it) listoLatch.countDown()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        )
                    }
                }
            }

            assertTrue("Modo seguimiento debe cargar", listoLatch.await(20, TimeUnit.SECONDS))
            assertEquals(true, disponible)

            val wv = esperarWebView(scenario)!!
            val z0 = evaluarJs(scenario, wv, "FoodJet.zoomActual()").toDoubleOrNull()!!
            evaluarJs(scenario, wv, "FoodJet.zoom(1)")
            SystemClock.sleep(300)
            val zUp = evaluarJs(scenario, wv, "FoodJet.zoomActual()").toDoubleOrNull()!!
            assertTrue("FoodJet.zoom(1) debe incrementar el zoom", zUp > z0)

            evaluarJs(scenario, wv, "FoodJet.zoom(-2)")
            SystemClock.sleep(300)
            val zDown = evaluarJs(scenario, wv, "FoodJet.zoomActual()").toDoubleOrNull()!!
            assertTrue("FoodJet.zoom(-2) debe reducir el zoom", zDown < zUp)

            // Clamp inferior/superior [2, 21]
            evaluarJs(scenario, wv, "FoodJet.zoom(50)")
            SystemClock.sleep(200)
            assertEquals(21.0, evaluarJs(scenario, wv, "FoodJet.zoomActual()").toDouble(), 0.1)
            evaluarJs(scenario, wv, "FoodJet.zoom(-50)")
            SystemClock.sleep(200)
            assertEquals(2.0, evaluarJs(scenario, wv, "FoodJet.zoomActual()").toDouble(), 0.1)
        }

        // Ahora verificamos fallback a RutaEsquematica cuando la única key es rechazada
        ConfigMapa.fijarCacheParaPruebas(
            ServidorFoodJet.NODE_LOCAL_EMULADOR,
            listOf("AIzaInvalidOnlyKeyForFallback000")
        )
        val noDisponibleLatch = CountDownLatch(1)
        var estadoFallback: Boolean? = null

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    Box(Modifier.fillMaxSize()) {
                        MapaFoodJet(
                            datos = DatosMapa(
                                restaurante = PuntoMapa(GeoPoint(-12.1219, -77.0297), "Local", "Miraflores"),
                                destino = PuntoMapa(GeoPoint(-12.1310, -77.0220), "Entrega", "Av. Principal")
                            ),
                            modo = ModoMapa.SEGUIMIENTO,
                            onDisponibilidad = {
                                estadoFallback = it
                                if (!it) noDisponibleLatch.countDown()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        )
                    }
                }
            }

            assertTrue("Debe caer a NoDisponible cuando todas las keys fallan", noDisponibleLatch.await(20, TimeUnit.SECONDS))
            assertEquals(false, estadoFallback)
        }
    }

    private fun esperarWebView(scenario: ActivityScenario<MainActivity>): WebView? {
        var found: WebView? = null
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (found == null && SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity { activity ->
                found = buscarWebView(activity.window.decorView)
            }
            if (found == null) SystemClock.sleep(100)
        }
        return found
    }

    private fun buscarWebView(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                buscarWebView(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    private fun evaluarJs(scenario: ActivityScenario<MainActivity>, webView: WebView, js: String): String {
        val latch = CountDownLatch(1)
        val out = AtomicReference("")
        scenario.onActivity {
            webView.evaluateJavascript(js) { res ->
                out.set(res ?: "")
                latch.countDown()
            }
        }
        assertTrue("Timeout evaluando JS: $js", latch.await(5, TimeUnit.SECONDS))
        return out.get()
    }

    private fun inyectarArrastre(x0: Float, y0: Float, x1: Float, y1: Float) {
        val ui = InstrumentationRegistry.getInstrumentation().uiAutomation
        val downTime = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x0, y0, 0).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
        }
        ui.injectInputEvent(down, true)
        down.recycle()

        val steps = 10
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            val evTime = downTime + i * 20L
            val move = MotionEvent.obtain(
                downTime, evTime, MotionEvent.ACTION_MOVE,
                x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, 0
            ).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
            ui.injectInputEvent(move, true)
            move.recycle()
            SystemClock.sleep(20)
        }

        val upTime = downTime + (steps + 1) * 20L
        val up = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, x1, y1, 0).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
        }
        ui.injectInputEvent(up, true)
        up.recycle()
    }

    private fun inyectarPinchOut(cx: Float, cy: Float, radioInicial: Float, radioFinal: Float) {
        val ui = InstrumentationRegistry.getInstrumentation().uiAutomation
        val downTime = SystemClock.uptimeMillis()

        fun obtenerEventoMulti(
            eventTime: Long,
            action: Int,
            x0: Float,
            y0: Float,
            x1: Float,
            y1: Float
        ): MotionEvent {
            val props = arrayOf(
                MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_FINGER },
                MotionEvent.PointerProperties().apply { id = 1; toolType = MotionEvent.TOOL_TYPE_FINGER }
            )
            val coords = arrayOf(
                MotionEvent.PointerCoords().apply { x = x0; y = y0; pressure = 1f; size = 1f },
                MotionEvent.PointerCoords().apply { x = x1; y = y1; pressure = 1f; size = 1f }
            )
            return MotionEvent.obtain(
                downTime, eventTime, action, 2, props, coords,
                0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0
            )
        }

        val props0 = arrayOf(MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_FINGER })
        val coords0 = arrayOf(MotionEvent.PointerCoords().apply { x = cx - radioInicial; y = cy; pressure = 1f; size = 1f })
        val down0 = MotionEvent.obtain(
            downTime, downTime, MotionEvent.ACTION_DOWN, 1, props0, coords0,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0
        )
        ui.injectInputEvent(down0, true)
        down0.recycle()

        SystemClock.sleep(20)
        val pDown = obtenerEventoMulti(
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            cx - radioInicial, cy, cx + radioInicial, cy
        )
        ui.injectInputEvent(pDown, true)
        pDown.recycle()

        val steps = 20
        for (i in 1..steps) {
            SystemClock.sleep(20)
            val r = radioInicial + (radioFinal - radioInicial) * (i.toFloat() / steps)
            val evTime = SystemClock.uptimeMillis()
            val move = obtenerEventoMulti(evTime, MotionEvent.ACTION_MOVE, cx - r, cy, cx + r, cy)
            ui.injectInputEvent(move, true)
            move.recycle()
        }

        SystemClock.sleep(20)
        val endTime = SystemClock.uptimeMillis()
        val pUp = obtenerEventoMulti(
            endTime,
            MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            cx - radioFinal, cy, cx + radioFinal, cy
        )
        ui.injectInputEvent(pUp, true)
        pUp.recycle()

        SystemClock.sleep(20)
        val coordsUp0 = arrayOf(MotionEvent.PointerCoords().apply { x = cx - radioFinal; y = cy; pressure = 1f; size = 1f })
        val up0 = MotionEvent.obtain(
            downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 1, props0, coordsUp0,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0
        )
        ui.injectInputEvent(up0, true)
        up0.recycle()
    }
}
