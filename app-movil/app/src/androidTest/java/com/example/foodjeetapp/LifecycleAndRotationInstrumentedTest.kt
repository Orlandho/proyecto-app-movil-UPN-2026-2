package com.example.foodjeetapp

import android.Manifest
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.foodjeetapp.data.common.UiState
import com.example.foodjeetapp.data.di.ServiceLocator
import com.example.foodjeetapp.data.model.CartItem
import com.example.foodjeetapp.data.model.GeoPoint
import com.example.foodjeetapp.data.model.ProductItem
import com.example.foodjeetapp.data.remote.RetrofitClient
import com.example.foodjeetapp.data.remote.ServidorFoodJet
import com.example.foodjeetapp.ui.IntentsFoodJet
import com.example.foodjeetapp.ui.components.ConfigMapa
import com.example.foodjeetapp.ui.components.ControladorMapa
import com.example.foodjeetapp.ui.components.DatosMapa
import com.example.foodjeetapp.ui.components.MapaFoodJet
import com.example.foodjeetapp.ui.components.ModoMapa
import com.example.foodjeetapp.ui.components.PuntoMapa
import com.example.foodjeetapp.ui.screens.CheckoutScreen
import com.example.foodjeetapp.ui.theme.FoodJetTheme
import com.example.foodjeetapp.ui.viewmodel.CartViewModel
import com.example.foodjeetapp.ui.viewmodel.HomeViewModel
import com.example.foodjeetapp.ui.viewmodel.OrderViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * Pruebas instrumentadas de ciclo de vida, rotación (`ActivityScenario.recreate()`),
 * intents explícitos, sondeo de seguimiento, liberación de WebView y estado sensible
 * del formulario de entrega (Semanas 2 y 3).
 */
@RunWith(AndroidJUnit4::class)
class LifecycleAndRotationInstrumentedTest {

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

    // 1. Rotación durante navegación entre pantallas principales
    @Test
    fun escenario01_rotacionDuranteNavegacion_conservaPantallaYViewModel() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var homeVmIdAntes = 0
            scenario.onActivity { activity ->
                homeVmIdAntes = System.identityHashCode(ViewModelProvider(activity)[HomeViewModel::class.java])
            }

            assertTrue(
                "Debe poder navegar a la pestaña Explorar",
                esperarYClickTexto("Explorar")
            )
            assertTrue(
                "La pantalla Explorar debe quedar visible antes de rotar",
                esperarTextoVisible("Explora todo el menú")
            )

            scenario.recreate()

            var homeVmIdDespues = 0
            scenario.onActivity { activity ->
                homeVmIdDespues = System.identityHashCode(ViewModelProvider(activity)[HomeViewModel::class.java])
            }

            assertEquals("HomeViewModel debe conservarse tras recreate()", homeVmIdAntes, homeVmIdDespues)
            assertTrue(
                "La ruta Explorar debe conservarse tras recreate()",
                esperarTextoVisible("Explora todo el menú")
            )
        }
    }

    // 2. Rotación con elementos en el carrito
    @Test
    fun escenario02_rotacionConCarrito_conservaProductosYCantidades() {
        val productoA = ProductItem(
            id = 501,
            nombre = "Hamburguesa Clásica Test",
            descripcion = "Prueba de rotación",
            precio = 18.5,
            imagenUrl = "",
            restauranteId = 1,
            restauranteNombre = "FoodJet Local",
            tipoComida = "Hamburguesas",
            tiempoEntrega = "30 min"
        )
        val productoB = ProductItem(
            id = 502,
            nombre = "Papas Crocantes Test",
            descripcion = "Prueba de rotación",
            precio = 9.0,
            imagenUrl = "",
            restauranteId = 1,
            restauranteNombre = "FoodJet Local",
            tipoComida = "Acompañamientos",
            tiempoEntrega = "30 min"
        )

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val cartVm = ViewModelProvider(activity)[CartViewModel::class.java]
                cartVm.clearCart()
                cartVm.addToCart(productoA)
                cartVm.addToCart(productoA)
                cartVm.addToCart(productoB)
            }

            esperarCondicion {
                var count = 0
                scenario.onActivity { activity ->
                    count = ViewModelProvider(activity)[CartViewModel::class.java]
                        .cartItems.value.sumOf { it.quantity }
                }
                count == 3
            }

            scenario.recreate()

            var itemsTrasRotar = emptyList<CartItem>()
            esperarCondicion {
                scenario.onActivity { activity ->
                    itemsTrasRotar = ViewModelProvider(activity)[CartViewModel::class.java].cartItems.value
                }
                itemsTrasRotar.sumOf { it.quantity } == 3
            }

            assertEquals(2, itemsTrasRotar.size)
            assertEquals(2, itemsTrasRotar.first { it.product.id == 501 }.quantity)
            assertEquals(1, itemsTrasRotar.first { it.product.id == 502 }.quantity)

            // Limpiar carrito para no afectar pruebas posteriores
            scenario.onActivity { activity ->
                ViewModelProvider(activity)[CartViewModel::class.java].clearCart()
            }
        }
    }

    // 3. Rotación durante seguimiento de pedido
    @Test
    fun escenario03_rotacionDuranteSeguimiento_conservaPedidoYReanudaSondeo() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = IntentsFoodJet.seguirPedido(context, 77)

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            assertTrue(
                "El seguimiento del pedido #77 debe iniciar sondeo activo",
                esperarCondicion {
                    var ok = false
                    scenario.onActivity { activity ->
                        val vm = ViewModelProvider(activity)[OrderViewModel::class.java]
                        ok = vm.currentTrackedOrderId == 77 && vm.isTrackingPollingActive
                    }
                    ok
                }
            )

            scenario.recreate()

            assertTrue(
                "Tras recreate(), el seguimiento debe seguir en pedido #77 con sondeo activo",
                esperarCondicion {
                    var ok = false
                    scenario.onActivity { activity ->
                        val vm = ViewModelProvider(activity)[OrderViewModel::class.java]
                        ok = vm.currentTrackedOrderId == 77 && vm.isTrackingPollingActive
                    }
                    ok
                }
            )
            assertTrue(
                "La pantalla de seguimiento debe seguir mostrando #77",
                esperarTextoVisible("#77")
            )
        }
    }

    // 4. Rotación en formulario de entrega: conserva datos de entrega y NO persiste datos sensibles de tarjeta
    @Test
    fun escenario04_rotacionEnFormularioEntrega_conservaEntregaYNoPersisteTarjeta() {
        // Sin keys de mapa para evitar WebView innecesario y enfocar el test en el estado del formulario.
        ConfigMapa.fijarCacheParaPruebas(ServidorFoodJet.NODE_LOCAL_EMULADOR, emptyList())

        val carritoPrueba = listOf(
            CartItem(
                product = ProductItem(
                    id = 601,
                    nombre = "Combo Test",
                    descripcion = "Combo para checkout",
                    precio = 25.0,
                    imagenUrl = "",
                    restauranteId = 1,
                    restauranteNombre = "FoodJet Local",
                    tipoComida = "Combos",
                    tiempoEntrega = "30 min"
                ),
                quantity = 1
            )
        )

        val registroRef = AtomicReference<SaveableStateRegistry>()
        var mostrarFormulario by mutableStateOf(true)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val registroInicial = SaveableStateRegistry(restoredValues = null) { true }
                registroRef.set(registroInicial)
                activity.setContent {
                    FoodJetTheme {
                        if (mostrarFormulario) {
                            val reg = registroRef.get()
                            CompositionLocalProvider(LocalSaveableStateRegistry provides reg) {
                                CheckoutScreen(
                                    cartItems = carritoPrueba,
                                    isStudent = false,
                                    isLoggedIn = true,
                                    initialName = "Ana Pérez",
                                    initialPhone = "999888777",
                                    addressesState = UiState.Empty,
                                    isSubmitting = false,
                                    onRequireLogin = {},
                                    onRetryAddresses = {},
                                    onBackToMenu = {},
                                    onConfirmar = { _, _, _ -> }
                                )
                            }
                        }
                    }
                }
            }

            assertTrue("Debe mostrarse campo Dirección de entrega", esperarTextoVisible("Dirección de entrega"))

            // Ingresar dirección y referencia no sensibles
            assertTrue(
                "Debe escribir dirección de entrega",
                escribirEnCampoPorEtiqueta("Dirección de entrega", "Av. Larco 450, Miraflores")
            )
            assertTrue(
                "Debe escribir referencia",
                escribirEnCampoPorEtiqueta("Referencia", "Depto 302 torre B")
            )

            // Seleccionar pago con tarjeta e ingresar datos sensibles
            assertTrue("Debe poder desplazarse a Tarjeta", desplazarYClickTexto("Crédito o débito"))
            Thread.sleep(400)
            assertTrue(
                "Debe escribir número de tarjeta",
                escribirEnCampoPorEtiqueta("Número de tarjeta", "4111111111111111")
            )
            assertTrue(
                "Debe escribir vencimiento MM/AA",
                escribirEnCampoPorEtiqueta("MM/AA", "1229")
            )
            assertTrue(
                "Debe escribir CVC",
                escribirEnCampoPorEtiqueta("CVC", "987")
            )

            // Simular guardado real de SavedInstanceState antes de recrear la pantalla
            val estadoGuardadoRef = AtomicReference<Map<String, List<Any?>>>()
            scenario.onActivity {
                estadoGuardadoRef.set(registroRef.get().performSave())
            }
            val estadoGuardado = estadoGuardadoRef.get()
            val textoSerializado = estadoGuardado.values.flatten().joinToString(" | ") { it.toString() }

            assertTrue(
                "La dirección de entrega debe guardarse en SaveableStateRegistry",
                textoSerializado.contains("Av. Larco 450, Miraflores")
            )
            assertTrue(
                "La referencia debe guardarse en SaveableStateRegistry",
                textoSerializado.contains("Depto 302 torre B")
            )
            assertFalse(
                "El número de tarjeta NO debe serializarse en SavedInstanceState",
                textoSerializado.contains("4111111111111111")
            )
            assertFalse(
                "El vencimiento de tarjeta NO debe serializarse en SavedInstanceState",
                textoSerializado.contains("12/29") || textoSerializado.contains("1229")
            )
            assertFalse(
                "El CVC de tarjeta NO debe serializarse en SavedInstanceState",
                textoSerializado.contains("987")
            )

            // Recrear la composición (desmontar + restaurar únicamente desde SaveableStateRegistry)
            scenario.onActivity { mostrarFormulario = false }
            Thread.sleep(250)
            scenario.onActivity {
                registroRef.set(SaveableStateRegistry(restoredValues = estadoGuardado) { true })
                mostrarFormulario = true
            }

            // Primero verificar en la sección visible tras restaurar que el método 'card' se restauró
            // pero el número de tarjeta, MM/AA y CVC quedaron vacíos (no persistidos).
            assertTrue(
                "El método de pago 'Tarjeta' debe permanecer seleccionado tras recrear",
                esperarTextoVisible("Número de tarjeta")
            )
            assertFalse(
                "El número de tarjeta NO debe quedar en pantalla tras recrear",
                hayTextoEnPantalla("4111111111111111")
            )
            assertFalse(
                "El vencimiento NO debe quedar en pantalla tras recrear",
                hayTextoEnPantalla("12/29")
            )
            assertFalse(
                "El CVC NO debe quedar en pantalla tras recrear",
                hayTextoEnPantalla("987")
            )

            // Desplazar hacia arriba en el LazyColumn para comprobar la restauración de la dirección
            assertTrue(
                "La dirección de entrega debe restaurarse en la UI tras recrear",
                desplazarArribaYEsperarTexto("Av. Larco 450, Miraflores")
            )
        }
    }

    // 5. Paso a segundo plano y regreso a primer plano detiene y reanuda sondeo
    @Test
    fun escenario05_segundoPlanoYPrimerPlano_detieneYReanudaSondeo() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = IntentsFoodJet.seguirPedido(context, 88)

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            assertTrue(
                "El sondeo debe estar activo en primer plano (RESUMED)",
                esperarCondicion {
                    var active = false
                    scenario.onActivity { activity ->
                        active = ViewModelProvider(activity)[OrderViewModel::class.java].isTrackingPollingActive
                    }
                    active
                }
            )

            // Segundo plano: ON_PAUSE -> ON_STOP
            scenario.moveToState(Lifecycle.State.CREATED)
            assertTrue(
                "El sondeo debe detenerse al pasar a segundo plano (ON_STOP)",
                esperarCondicion {
                    var active = true
                    scenario.onActivity { activity ->
                        active = ViewModelProvider(activity)[OrderViewModel::class.java].isTrackingPollingActive
                    }
                    !active
                }
            )

            // Regreso a primer plano: ON_START -> ON_RESUME
            scenario.moveToState(Lifecycle.State.RESUMED)
            assertTrue(
                "El sondeo debe reanudarse al regresar a primer plano (ON_START)",
                esperarCondicion {
                    var active = false
                    scenario.onActivity { activity ->
                        active = ViewModelProvider(activity)[OrderViewModel::class.java].isTrackingPollingActive
                    }
                    active
                }
            )
        }
    }

    // 6. Apertura mediante Intent explícito de pedido
    @Test
    fun escenario06_intentExplicitoAbrePedidoCorrecto() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = IntentsFoodJet.seguirPedido(context, 91)

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            assertTrue(
                "OrderViewModel debe seguir exactamente el pedido #91 recibido por Intent explícito",
                esperarCondicion {
                    var id: Int? = null
                    scenario.onActivity { activity ->
                        id = ViewModelProvider(activity)[OrderViewModel::class.java].currentTrackedOrderId
                    }
                    id == 91
                }
            )
            assertTrue("Debe mostrarse el identificador #91 en pantalla", esperarTextoVisible("#91"))
        }
    }

    // 7. Recrear la Activity tras un Intent explícito sin repetir navegación
    @Test
    fun escenario07_recrearTrasIntentExplicito_noRepiteNavegacion() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = IntentsFoodJet.seguirPedido(context, 92)

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            assertTrue("Debe abrir seguimiento del pedido #92", esperarTextoVisible("#92"))

            // El usuario regresa al Inicio saliendo de la pantalla de seguimiento
            assertTrue("Debe poder volver a Inicio desde Tracking", esperarYClickDescripcionOTexto("Volver"))
            assertTrue(
                "Al salir de Tracking el sondeo debe detenerse",
                esperarCondicion {
                    var active = true
                    scenario.onActivity { activity ->
                        active = ViewModelProvider(activity)[OrderViewModel::class.java].isTrackingPollingActive
                    }
                    !active
                }
            )

            // Recrear la Activity NO debe volver a navegar automáticamente a Tracking #92
            scenario.recreate()
            assertTrue(
                "Tras recreate() debe permanecer en Inicio sin reabrir Tracking",
                esperarTextoVisible("Explorar")
            )
            var pollingTrasRecrear = true
            scenario.onActivity { activity ->
                pollingTrasRecrear = ViewModelProvider(activity)[OrderViewModel::class.java].isTrackingPollingActive
            }
            assertFalse("No debe reactivarse el sondeo tras recreate() fuera de Tracking", pollingTrasRecrear)
        }
    }

    // 8. Salir de pantalla con WebView (mapa) libera recursos
    @Test
    fun escenario08_salirDePantallaConWebView_liberaRecursos() {
        ConfigMapa.fijarCacheParaPruebas(ServidorFoodJet.NODE_LOCAL_EMULADOR, listOf("test-key-lifecycle"))
        val controlador = ControladorMapa()
        var mostrarMapa by mutableStateOf(true)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    FoodJetTheme {
                        if (mostrarMapa) {
                            MapaFoodJet(
                                datos = DatosMapa(
                                    restaurante = PuntoMapa(GeoPoint(-12.1219, -77.0297), "Local", "Miraflores")
                                ),
                                modo = ModoMapa.SEGUIMIENTO,
                                controlador = controlador
                            )
                        }
                    }
                }
            }

            assertTrue(
                "El WebView del mapa debe crearse y vincularse al controlador",
                esperarCondicion { controlador.vista != null }
            )

            // Simular salida de pantalla retirando MapaFoodJet de la composición
            scenario.onActivity { mostrarMapa = false }

            assertTrue(
                "Al salir de la pantalla, onRelease debe desvincular el controlador y retirar el WebView",
                esperarCondicion {
                    var webViewsRestantes = -1
                    scenario.onActivity { activity ->
                        webViewsRestantes = contarWebViews(activity.window.decorView)
                    }
                    controlador.vista == null && !controlador.listo && webViewsRestantes == 0
                }
            )
            assertNull("El controlador no debe retener referencia al WebView destruido", controlador.vista)
        }
    }

    // 9. Salir y volver a la pantalla de seguimiento detiene y reactiva sondeo limpiamente
    @Test
    fun escenario09_salirYVolverASeguimiento_detieneYReactivaSondeo() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = IntentsFoodJet.seguirPedido(context, 95)

        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            assertTrue(
                "El sondeo debe activarse al entrar a seguimiento #95",
                esperarCondicion {
                    var active = false
                    scenario.onActivity { activity ->
                        active = ViewModelProvider(activity)[OrderViewModel::class.java].isTrackingPollingActive
                    }
                    active
                }
            )

            // Salir de seguimiento
            assertTrue("Debe salir de seguimiento al tocar Volver", esperarYClickDescripcionOTexto("Volver"))
            assertTrue(
                "El sondeo debe detenerse al salir de seguimiento",
                esperarCondicion {
                    var active = true
                    scenario.onActivity { activity ->
                        active = ViewModelProvider(activity)[OrderViewModel::class.java].isTrackingPollingActive
                    }
                    !active
                }
            )

            // Volver a entrar mediante nuevo Intent explícito (singleTop -> onNewIntent)
            scenario.onActivity { activity ->
                activity.startActivity(IntentsFoodJet.seguirPedido(activity, 95))
            }

            assertTrue(
                "El sondeo debe reactivarse limpiamente al reingresar a seguimiento #95",
                esperarCondicion {
                    var ok = false
                    scenario.onActivity { activity ->
                        val vm = ViewModelProvider(activity)[OrderViewModel::class.java]
                        ok = vm.currentTrackedOrderId == 95 && vm.isTrackingPollingActive
                    }
                    ok
                }
            )
        }
    }

    // 10. Permiso de ubicación durante transición de ciclo de vida (sin crash y estado coherente)
    @Test
    fun escenario10_permisoUbicacionEnTransicionDeCicloDeVida_estadoCoherenteSinCrash() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Transición de ciclo de vida equivalente a diálogo del sistema de permisos (ON_PAUSE/ON_STOP -> ON_RESUME)
            scenario.moveToState(Lifecycle.State.STARTED)
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)

            // Conceder permiso en tiempo de ejecución y verificar estado coherente en LocationProvider tras recrear
            instrumentation.uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

            var tienePermisoAntes = false
            scenario.onActivity {
                tienePermisoAntes = ServiceLocator.locationProvider.tienePermiso()
            }
            assertTrue("LocationProvider debe detectar el permiso concedido", tienePermisoAntes)

            scenario.recreate()

            var tienePermisoDespues = false
            scenario.onActivity { activity ->
                assertNotNull(activity)
                tienePermisoDespues = ServiceLocator.locationProvider.tienePermiso()
            }
            assertTrue(
                "Tras recreate() el estado del permiso de ubicación debe mantenerse coherente",
                tienePermisoDespues
            )
        }
    }

    // --- Utilidades de inspección UI basadas en AccessibilityNodeInfo (sin dependencias extra) ---

    private fun contarWebViews(view: View): Int {
        if (view is WebView) return 1
        if (view !is ViewGroup) return 0
        var total = 0
        for (i in 0 until view.childCount) {
            total += contarWebViews(view.getChildAt(i))
        }
        return total
    }

    private fun esperarCondicion(
        timeoutMs: Long = 8_000L,
        condicion: () -> Boolean
    ): Boolean {
        val limite = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < limite) {
            if (condicion()) return true
            Thread.sleep(100)
        }
        return condicion()
    }

    private fun rootNode(): AccessibilityNodeInfo? =
        InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow

    private fun hayTextoEnPantalla(texto: String): Boolean {
        val root = rootNode() ?: return false
        return buscarNodo(root) { nodo ->
            nodo.text?.toString()?.contains(texto, ignoreCase = true) == true ||
                nodo.contentDescription?.toString()?.contains(texto, ignoreCase = true) == true
        } != null
    }

    private fun esperarTextoVisible(texto: String, timeoutMs: Long = 8_000L): Boolean =
        esperarCondicion(timeoutMs) { hayTextoEnPantalla(texto) }

    private fun esperarYClickTexto(texto: String, timeoutMs: Long = 8_000L): Boolean {
        return esperarCondicion(timeoutMs) {
            val root = rootNode() ?: return@esperarCondicion false
            val nodo = buscarNodo(root) { n ->
                n.text?.toString()?.equals(texto, ignoreCase = true) == true ||
                    n.text?.toString()?.contains(texto, ignoreCase = true) == true
            } ?: return@esperarCondicion false
            clickearOAncestro(nodo)
        }
    }

    private fun esperarYClickDescripcionOTexto(texto: String, timeoutMs: Long = 8_000L): Boolean {
        return esperarCondicion(timeoutMs) {
            val root = rootNode() ?: return@esperarCondicion false
            val nodo = buscarNodo(root) { n ->
                n.contentDescription?.toString()?.contains(texto, ignoreCase = true) == true ||
                    n.text?.toString()?.contains(texto, ignoreCase = true) == true
            } ?: return@esperarCondicion false
            clickearOAncestro(nodo)
        }
    }

    private fun desplazarYClickTexto(texto: String, timeoutMs: Long = 8_000L): Boolean {
        val limite = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < limite) {
            val root = rootNode()
            if (root != null) {
                val nodo = buscarNodo(root) { n ->
                    n.text?.toString()?.contains(texto, ignoreCase = true) == true
                }
                if (nodo != null && clickearOAncestro(nodo)) return true
                val scrollable = buscarNodo(root) { it.isScrollable }
                scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            }
            Thread.sleep(200)
        }
        return false
    }

    private fun desplazarArribaYEsperarTexto(texto: String, timeoutMs: Long = 8_000L): Boolean {
        val limite = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < limite) {
            if (hayTextoEnPantalla(texto)) return true
            val root = rootNode()
            val scrollable = root?.let { buscarNodo(it) { n -> n.isScrollable } }
            scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            Thread.sleep(200)
        }
        return hayTextoEnPantalla(texto)
    }

    private fun escribirEnCampoPorEtiqueta(etiqueta: String, valor: String, timeoutMs: Long = 8_000L): Boolean {
        val limite = System.currentTimeMillis() + timeoutMs
        var intentosSinScroll = 0
        while (System.currentTimeMillis() < limite) {
            val root = rootNode()
            if (root != null) {
                val editable = buscarCampoEditablePorEtiqueta(root, etiqueta)
                if (editable != null) {
                    editable.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                    val args = Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, valor)
                    }
                    if (editable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
                        Thread.sleep(200)
                        return true
                    }
                }
                if (intentosSinScroll >= 3) {
                    val scrollable = buscarNodo(root) { it.isScrollable }
                    scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                }
                intentosSinScroll++
            }
            Thread.sleep(200)
        }
        return false
    }

    private fun buscarCampoEditablePorEtiqueta(root: AccessibilityNodeInfo, etiqueta: String): AccessibilityNodeInfo? {
        return buscarNodo(root) { n ->
            if (!n.isEditable) return@buscarNodo false
            val hint = n.hintText?.toString().orEmpty()
            val text = n.text?.toString().orEmpty()
            val desc = n.contentDescription?.toString().orEmpty()
            if (hint.contains(etiqueta, ignoreCase = true) ||
                text.contains(etiqueta, ignoreCase = true) ||
                desc.contains(etiqueta, ignoreCase = true)
            ) {
                return@buscarNodo true
            }
            // En Material 3 OutlinedTextField el label suele aparecer como nodo hijo de texto dentro del nodo editable.
            buscarNodo(n) { hijo ->
                hijo.text?.toString()?.contains(etiqueta, ignoreCase = true) == true
            } != null
        }
    }

    private fun clickearOAncestro(nodo: AccessibilityNodeInfo): Boolean {
        var actual: AccessibilityNodeInfo? = nodo
        while (actual != null) {
            if (actual.isClickable && actual.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true
            }
            actual = actual.parent
        }
        return false
    }

    private fun buscarNodo(
        raiz: AccessibilityNodeInfo,
        predicado: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (predicado(raiz)) return raiz
        for (i in 0 until raiz.childCount) {
            val hijo = raiz.getChild(i) ?: continue
            val encontrado = buscarNodo(hijo, predicado)
            if (encontrado != null) return encontrado
        }
        return null
    }
}
