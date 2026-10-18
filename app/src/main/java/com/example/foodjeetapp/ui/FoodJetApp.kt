package com.example.foodjeetapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.foodjeetapp.data.common.UiState
import com.example.foodjeetapp.data.di.ServiceLocator
import com.example.foodjeetapp.data.model.*
import com.example.foodjeetapp.data.remote.RetrofitClient
import com.example.foodjeetapp.ui.components.FoodJetBottomBar
import com.example.foodjeetapp.ui.components.FoodJetTopBar
import com.example.foodjeetapp.ui.screens.*
import com.example.foodjeetapp.ui.viewmodel.AuthViewModel
import com.example.foodjeetapp.ui.viewmodel.CartViewModel
import com.example.foodjeetapp.ui.viewmodel.HomeViewModel
import com.example.foodjeetapp.ui.viewmodel.OrderViewModel
import kotlinx.coroutines.launch

/** Rutas del NavHost (REQ-SEM03-VIS-03). */
object Rutas {
    const val HOME = "home"
    const val MENU = "menu"
    const val FAVORITOS = "favorites"
    const val PEDIDOS = "orders"
    const val CHECKOUT = "checkout"
    const val DASHBOARD = "dashboard"
    const val ARG_PEDIDO = "orderId"
    const val SEGUIMIENTO = "tracking/{$ARG_PEDIDO}"

    fun seguimiento(pedidoId: Int) = "tracking/$pedidoId"

    /** Pantallas de flujo completo, sin barra superior ni inferior. */
    val SIN_BARRAS = setOf(CHECKOUT, SEGUIMIENTO, DASHBOARD)
}

/** Pago con billetera pendiente de confirmar en el diálogo del QR. */
private data class PagoQrPendiente(
    val entrega: DeliverySelection,
    val cuponId: Int?,
    val restauranteId: Int,
    val restauranteNombre: String,
    val qrUrl: String?
)

/**
 * Contenedor principal de la aplicación FoodJet.
 * Implementa el patrón MVVM (REQ-SEM06-LOG-01) y retiene estado con ViewModels (REQ-SEM06-LOG-02).
 * Consume flujos de estado inmutables con collectAsStateWithLifecycle (REQ-SEM06-VIS-01).
 * Navega con Compose Navigation (NavHost) pasando el id del pedido como argumento (REQ-SEM03-VIS-03).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodJetApp(
    homeViewModel: HomeViewModel = viewModel(),
    cartViewModel: CartViewModel = viewModel(),
    orderViewModel: OrderViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    pedidoSolicitado: Int? = null,
    onPedidoAtendido: () -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    fun avisar(mensaje: String) {
        coroutineScope.launch { snackbarHostState.showSnackbar(mensaje) }
    }

    // Estados expuestos por los ViewModels (Arquitectura MVVM limpia)
    val productsState by homeViewModel.productsState.collectAsStateWithLifecycle()
    val favorites by homeViewModel.favorites.collectAsStateWithLifecycle()

    val cartItems by cartViewModel.cartItems.collectAsStateWithLifecycle()
    val ordersState by orderViewModel.ordersState.collectAsStateWithLifecycle()
    val currentTrackingOrder by orderViewModel.currentTrackingOrder.collectAsStateWithLifecycle()
    val trackingError by orderViewModel.trackingError.collectAsStateWithLifecycle()
    val adminOrdersState by orderViewModel.adminOrdersState.collectAsStateWithLifecycle()
    val addressesState by orderViewModel.addressesState.collectAsStateWithLifecycle()
    val isSubmitting by orderViewModel.isSubmitting.collectAsStateWithLifecycle()

    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val servidor by authViewModel.servidor.collectAsStateWithLifecycle()

    val activeUser = currentUser ?: UserProfile(name = "Invitado", email = "", phone = "", isStudent = false)

    val backStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = backStackEntry?.destination?.route ?: Rutas.HOME

    // Estados de Hojas y Modales
    var showCartSheet by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var pagoQr by remember { mutableStateOf<PagoQrPendiente?>(null) }
    var reviewTargetOrder by remember { mutableStateOf<OrderRecord?>(null) }
    var showMiCuentaDialog by remember { mutableStateOf(false) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cartCount = cartItems.sumOf { it.quantity }

    // Intent explícito del acceso directo "Seguir pedido #N".
    LaunchedEffect(pedidoSolicitado) {
        val pedidoId = pedidoSolicitado ?: return@LaunchedEffect
        orderViewModel.seguirPedido(pedidoId)
        navController.navigate(Rutas.seguimiento(pedidoId)) { launchSingleTop = true }
        onPedidoAtendido()
    }

    fun irA(ruta: String) {
        navController.navigate(ruta) {
            popUpTo(Rutas.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun abrirSeguimiento(pedido: OrderRecord) {
        orderViewModel.setTrackingOrder(pedido)
        navController.navigate(Rutas.seguimiento(pedido.numericId)) { launchSingleTop = true }
    }

    fun registrarPedido(entrega: DeliverySelection, restauranteId: Int, metodo: String, cuponId: Int?) {
        val borrador = OrderRecord(
            id = "FJ-NUEVO",
            fecha = "",
            estado = OrderStatus.PENDIENTE,
            items = cartItems,
            subtotal = cartViewModel.getSubtotal(activeUser.isStudent),
            impuestos = cartViewModel.getTaxAmount(activeUser.isStudent),
            total = cartViewModel.getTotal(activeUser.isStudent),
            paymentMethod = metodo
        )
        orderViewModel.createOrder(
            order = borrador,
            entrega = entrega,
            restauranteId = restauranteId,
            metodoPago = metodo,
            cuponId = cuponId,
            onSuccess = { creado ->
                cartViewModel.clearCart()
                IntentsFoodJet.publicarAccesoDirecto(context, creado.numericId)
                navController.navigate(Rutas.seguimiento(creado.numericId)) {
                    popUpTo(Rutas.HOME)
                }
                avisar("¡Pedido #${creado.numericId} registrado!")
            },
            onError = ::avisar
        )
    }

    fun limpiarSesion() {
        orderViewModel.limpiar()
        homeViewModel.retry()
        cartViewModel.clearCart()
        IntentsFoodJet.quitarAccesoDirecto(context)
        navController.navigate(Rutas.HOME) { popUpTo(0) }
    }

    val sesionRechazada by authViewModel.sesionRechazada.collectAsStateWithLifecycle()
    LaunchedEffect(sesionRechazada) {
        if (!sesionRechazada) return@LaunchedEffect
        authViewModel.sesionRechazadaAtendida()
        limpiarSesion()
        avisar("Tu sesión expiró. Inicia sesión de nuevo.")
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (rutaActual !in Rutas.SIN_BARRAS) {
                FoodJetTopBar(
                    cartCount = cartCount,
                    favoritesCount = favorites.size,
                    isLoggedIn = isLoggedIn,
                    userName = activeUser.name,
                    onLogoClick = { irA(Rutas.HOME) },
                    onCartClick = { showCartSheet = true },
                    onFavoritesClick = { irA(Rutas.FAVORITOS) },
                    onProfileClick = {
                        if (isLoggedIn) showMiCuentaDialog = true else showLoginDialog = true
                    }
                )
            }
        },
        bottomBar = {
            if (rutaActual !in Rutas.SIN_BARRAS) {
                FoodJetBottomBar(
                    currentRoute = rutaActual,
                    favoritesCount = favorites.size,
                    onNavigate = { route ->
                        if (route == "profile") {
                            if (isLoggedIn) showMiCuentaDialog = true else showLoginDialog = true
                        } else {
                            irA(route)
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(navController = navController, startDestination = Rutas.HOME) {
                composable(Rutas.HOME) {
                    HomeScreen(
                        productsState = productsState,
                        promotions = homeViewModel.getPromotions(),
                        isStudent = activeUser.isStudent,
                        favorites = favorites,
                        onFavoriteToggle = { id ->
                            if (!isLoggedIn) {
                                showLoginDialog = true
                                return@HomeScreen
                            }
                            val wasFav = favorites.contains(id)
                            homeViewModel.toggleFavorite(id)
                            avisar(if (wasFav) "Eliminado de favoritos" else "¡Añadido a favoritos! ❤️")
                        },
                        onAddToCart = { product ->
                            cartViewModel.addToCart(product)
                            avisar("¡${product.nombre} añadido al carrito!")
                        },
                        onNavigateToMenu = { irA(Rutas.MENU) },
                        onRetry = { homeViewModel.retry() }
                    )
                }
                composable(Rutas.MENU) {
                    HomeScreen(
                        productsState = productsState,
                        promotions = homeViewModel.getPromotions(),
                        isStudent = activeUser.isStudent,
                        favorites = favorites,
                        onFavoriteToggle = { id ->
                            if (!isLoggedIn) {
                                showLoginDialog = true
                                return@HomeScreen
                            }
                            homeViewModel.toggleFavorite(id)
                        },
                        onAddToCart = { product ->
                            cartViewModel.addToCart(product)
                            avisar("¡${product.nombre} añadido al carrito!")
                        },
                        onNavigateToMenu = {},
                        onRetry = { homeViewModel.retry() }
                    )
                }
                composable(Rutas.FAVORITOS) {
                    val favoriteProducts = (productsState as? UiState.Success<List<ProductItem>>)
                        ?.data?.filter { favorites.contains(it.id) }.orEmpty()
                    FavoritesScreen(
                        favoriteProducts = favoriteProducts,
                        isStudent = activeUser.isStudent,
                        onRemoveFavorite = { id ->
                            homeViewModel.toggleFavorite(id)
                            avisar("Eliminado de favoritos")
                        },
                        onAddToCart = { product ->
                            cartViewModel.addToCart(product)
                            avisar("¡${product.nombre} añadido al carrito!")
                        },
                        onExploreMenu = { irA(Rutas.HOME) }
                    )
                }
                composable(Rutas.PEDIDOS) {
                    LaunchedEffect(isLoggedIn) { if (isLoggedIn) orderViewModel.loadOrders() }
                    OrderHistoryScreen(
                        ordersState = ordersState,
                        isLoggedIn = isLoggedIn,
                        onLogin = { showLoginDialog = true },
                        onRetry = { orderViewModel.loadOrders() },
                        onLeaveReview = { order -> reviewTargetOrder = order },
                        onTrackOrder = ::abrirSeguimiento,
                        onBackToMenu = { irA(Rutas.HOME) }
                    )
                }
                composable(Rutas.CHECKOUT) {
                    LaunchedEffect(isLoggedIn) { if (isLoggedIn) orderViewModel.loadAddresses() }
                    CheckoutScreen(
                        cartItems = cartItems,
                        isStudent = activeUser.isStudent,
                        isLoggedIn = isLoggedIn,
                        initialName = activeUser.name.takeIf { isLoggedIn }.orEmpty(),
                        initialPhone = activeUser.phone,
                        addressesState = addressesState,
                        isSubmitting = isSubmitting,
                        onRequireLogin = { showLoginDialog = true },
                        onRetryAddresses = { orderViewModel.loadAddresses() },
                        onBackToMenu = { navController.popBackStack() },
                        onConfirmar = { metodo, cuponId, entrega ->
                            val restauranteId = cartItems.firstOrNull()?.product?.restauranteId
                            if (restauranteId == null) {
                                avisar("Tu carrito está vacío")
                                return@CheckoutScreen
                            }
                            if (metodo == "wallet") {
                                // Como en la web: se muestra el QR real del local antes de registrar.
                                coroutineScope.launch {
                                    orderViewModel.restaurante(restauranteId)
                                        .onSuccess { local ->
                                            pagoQr = PagoQrPendiente(
                                                entrega = entrega,
                                                cuponId = cuponId,
                                                restauranteId = restauranteId,
                                                restauranteNombre = local.nombre ?: "el local",
                                                qrUrl = servidor.recurso(local.qrPago)
                                            )
                                        }
                                        .onFailure { avisar(it.message ?: "No se pudo obtener el QR del local") }
                                }
                            } else {
                                registrarPedido(entrega, restauranteId, metodo, cuponId)
                            }
                        }
                    )
                }
                composable(
                    route = Rutas.SEGUIMIENTO,
                    arguments = listOf(navArgument(Rutas.ARG_PEDIDO) { type = NavType.IntType })
                ) { entry ->
                    val pedidoId = entry.arguments?.getInt(Rutas.ARG_PEDIDO) ?: 0
                    SeguimientoMientrasVisible(orderViewModel, pedidoId)
                    TrackingScreen(
                        order = currentTrackingOrder?.takeIf { it.numericId == pedidoId },
                        pedidoId = pedidoId,
                        error = trackingError,
                        onCancelOrder = { orderId ->
                            orderViewModel.cancelCurrentOrder(
                                orderId = orderId,
                                onSuccess = { avisar("Pedido cancelado") },
                                onError = ::avisar
                            )
                        },
                        onBackToMenu = { irA(Rutas.HOME) },
                        onViewHistory = { irA(Rutas.PEDIDOS) }
                    )
                }
                composable(Rutas.DASHBOARD) {
                    AdminDashboardScreen(
                        adminName = activeUser.name,
                        adminEmail = activeUser.email,
                        ordersState = adminOrdersState,
                        onAdvanceOrderStatus = { orderId, nextStatus ->
                            orderViewModel.advanceOrderStatus(
                                orderId = orderId,
                                nextStatus = nextStatus,
                                onSuccess = { avisar("Pedido #$orderId actualizado a ${nextStatus.replace('_', ' ')}") },
                                onError = ::avisar
                            )
                        },
                        onRefresh = { orderViewModel.loadAdminOrders() },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    // Modal BottomSheet del Carrito
    if (showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = bottomSheetState
        ) {
            CartSheet(
                cartItems = cartItems,
                isStudent = activeUser.isStudent,
                onIncreaseQty = { productId -> cartViewModel.increaseQuantity(productId) },
                onDecreaseQty = { productId -> cartViewModel.decreaseQuantity(productId) },
                onProceedToCheckout = {
                    showCartSheet = false
                    if (isLoggedIn) {
                        navController.navigate(Rutas.CHECKOUT) { launchSingleTop = true }
                    } else {
                        avisar("Inicia sesión para hacer tu pedido")
                        showLoginDialog = true
                    }
                },
                onClose = { showCartSheet = false }
            )
        }
    }

    // Diálogo de Login
    if (showLoginDialog) {
        LoginDialog(
            servidor = servidor,
            onCambiarServidor = { nuevo ->
                authViewModel.cambiarServidor(nuevo) {
                    limpiarSesion()
                    avisar("Ahora usas ${nuevo.etiqueta}")
                }
            },
            onDismiss = { showLoginDialog = false },
            onLoginSuccess = { email, password ->
                authViewModel.login(
                    email = email,
                    password = password,
                    onSuccess = { userName ->
                        showLoginDialog = false
                        // Recargar pedidos y favoritos del usuario recién logueado
                        orderViewModel.loadOrders()
                        homeViewModel.loadFavorites()
                        avisar("¡Bienvenido, $userName!")
                    },
                    onError = ::avisar
                )
            },
            onSwitchToRegister = {
                showLoginDialog = false
                showRegisterDialog = true
            }
        )
    }

    // Diálogo de Registro
    if (showRegisterDialog) {
        RegisterDialog(
            onDismiss = { showRegisterDialog = false },
            onRegisterSuccess = { name, email, phone, password ->
                authViewModel.register(
                    name = name,
                    email = email,
                    phone = phone,
                    password = password,
                    onSuccess = {
                        showRegisterDialog = false
                        orderViewModel.loadOrders()
                        homeViewModel.loadFavorites()
                        avisar("¡Cuenta creada exitosamente!")
                    },
                    onError = ::avisar
                )
            },
            onSwitchToLogin = {
                showRegisterDialog = false
                showLoginDialog = true
            }
        )
    }

    // Diálogo de Pago QR
    pagoQr?.let { pago ->
        QrPaymentDialog(
            restaurantName = pago.restauranteNombre,
            qrUrl = pago.qrUrl,
            onDismiss = { pagoQr = null },
            onPaymentConfirmed = {
                pagoQr = null
                registrarPedido(pago.entrega, pago.restauranteId, "wallet", pago.cuponId)
            }
        )
    }

    // Diálogo de Calificar Pedido
    reviewTargetOrder?.let { order ->
        ReviewDialog(
            order = order,
            onDismiss = { reviewTargetOrder = null },
            onSubmitReview = { orderId, stars, comment ->
                orderViewModel.submitReview(
                    orderId = orderId,
                    stars = stars,
                    comment = comment,
                    onDone = {
                        reviewTargetOrder = null
                        avisar("¡Gracias por calificar tu pedido!")
                    },
                    onError = ::avisar
                )
            }
        )
    }

    // Diálogo Mi Cuenta
    if (showMiCuentaDialog) {
        MiCuentaDialog(
            userName = activeUser.name,
            userEmail = activeUser.email,
            isStudent = activeUser.isStudent,
            isAdmin = activeUser.isAdmin,
            servidor = servidor,
            onCambiarServidor = { nuevo ->
                authViewModel.cambiarServidor(nuevo) {
                    showMiCuentaDialog = false
                    limpiarSesion()
                    avisar("Ahora usas ${nuevo.etiqueta}. Inicia sesión de nuevo.")
                }
            },
            onAbrirWeb = {
                if (!IntentsFoodJet.abrirWeb(context, servidor.webOrigen)) avisar("No hay navegador instalado")
            },
            onVerifyStudent = {
                authViewModel.verifyStudent(
                    onSuccess = {
                        showMiCuentaDialog = false
                        homeViewModel.retry()
                        avisar("¡Carnet verificado! Descuentos de estudiante desbloqueados.")
                    },
                    onError = ::avisar
                )
            },
            onOpenAdminDashboard = {
                showMiCuentaDialog = false
                navController.navigate(Rutas.DASHBOARD) { launchSingleTop = true }
            },
            onLogout = {
                authViewModel.logout {
                    showMiCuentaDialog = false
                    limpiarSesion()
                    avisar("Sesión cerrada")
                }
            },
            onDismiss = { showMiCuentaDialog = false }
        )
    }
}

/**
 * Sondea el pedido solo mientras su pantalla está en primer plano
 * (REQ-SEM04-LOG-01 y REQ-SEM04-INF-02): al pasar la app a segundo plano
 * (ON_STOP) o salir de la ruta se cancela la corrutina, y al volver
 * (ON_START) se reanuda con una consulta inmediata.
 */
@Composable
private fun SeguimientoMientrasVisible(orderViewModel: OrderViewModel, pedidoId: Int) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, pedidoId) {
        orderViewModel.seguirPedido(pedidoId)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> orderViewModel.reanudarSeguimiento()
                Lifecycle.Event.ON_STOP -> orderViewModel.pausarSeguimiento()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            orderViewModel.pausarSeguimiento()
        }
    }
}
