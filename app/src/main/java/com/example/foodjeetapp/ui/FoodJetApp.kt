package com.example.foodjeetapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodjeetapp.data.common.UiState
import com.example.foodjeetapp.data.model.*
import com.example.foodjeetapp.ui.components.FoodJetBottomBar
import com.example.foodjeetapp.ui.components.FoodJetTopBar
import com.example.foodjeetapp.ui.screens.*
import com.example.foodjeetapp.ui.viewmodel.AuthViewModel
import com.example.foodjeetapp.ui.viewmodel.CartViewModel
import com.example.foodjeetapp.ui.viewmodel.HomeViewModel
import com.example.foodjeetapp.ui.viewmodel.OrderViewModel
import kotlinx.coroutines.launch

/**
 * Contenedor principal de la aplicación FoodJet.
 * Implementa el patrón MVVM (REQ-SEM06-LOG-01) y retiene estado con ViewModels (REQ-SEM06-LOG-02).
 * Consume flujos de estado inmutables con collectAsStateWithLifecycle (REQ-SEM06-VIS-01).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodJetApp(
    homeViewModel: HomeViewModel = viewModel(),
    cartViewModel: CartViewModel = viewModel(),
    orderViewModel: OrderViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estados expuestos por los ViewModels (Arquitectura MVVM limpia)
    val productsState by homeViewModel.productsState.collectAsStateWithLifecycle()
    val favorites by homeViewModel.favorites.collectAsStateWithLifecycle()

    val cartItems by cartViewModel.cartItems.collectAsStateWithLifecycle()
    val ordersState by orderViewModel.ordersState.collectAsStateWithLifecycle()

    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()

    val activeUser = currentUser ?: UserProfile(name = "Invitado", email = "", isStudent = false)

    // Estados de navegación y vistas de interfaz
    var currentRoute by remember { mutableStateOf("home") } // home, menu, favorites, orders, checkout, tracking, dashboard

    // Estados de Hojas y Modales
    var showCartSheet by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var reviewTargetOrder by remember { mutableStateOf<OrderRecord?>(null) }
    var showMiCuentaDialog by remember { mutableStateOf(false) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cartCount = cartItems.sumOf { it.quantity }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (currentRoute !in listOf("checkout", "tracking", "dashboard")) {
                FoodJetTopBar(
                    cartCount = cartCount,
                    favoritesCount = favorites.size,
                    isLoggedIn = isLoggedIn,
                    userName = activeUser.name,
                    onLogoClick = { currentRoute = "home" },
                    onCartClick = { showCartSheet = true },
                    onFavoritesClick = { currentRoute = "favorites" },
                    onProfileClick = {
                        if (isLoggedIn) {
                            showMiCuentaDialog = true
                        } else {
                            showLoginDialog = true
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (currentRoute !in listOf("checkout", "tracking", "dashboard")) {
                FoodJetBottomBar(
                    currentRoute = currentRoute,
                    favoritesCount = favorites.size,
                    onNavigate = { route ->
                        if (route == "profile") {
                            if (isLoggedIn) {
                                showMiCuentaDialog = true
                            } else {
                                showLoginDialog = true
                            }
                        } else {
                            currentRoute = route
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
            when (currentRoute) {
                "home", "menu" -> {
                    HomeScreen(
                        productsState = productsState,
                        promotions = homeViewModel.getPromotions(),
                        isStudent = activeUser.isStudent,
                        favorites = favorites,
                        onFavoriteToggle = { id ->
                            val wasFav = favorites.contains(id)
                            homeViewModel.toggleFavorite(id)
                            coroutineScope.launch {
                                val msg = if (wasFav) "Eliminado de favoritos" else "¡Añadido a favoritos! ❤️"
                                snackbarHostState.showSnackbar(msg)
                            }
                        },
                        onAddToCart = { product ->
                            cartViewModel.addToCart(product)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("¡${product.nombre} añadido al carrito!")
                            }
                        },
                        onNavigateToMenu = {
                            currentRoute = "menu"
                        },
                        onRetry = {
                            homeViewModel.retry()
                        }
                    )
                }
                "favorites" -> {
                    val favoriteProducts = if (productsState is UiState.Success) {
                        (productsState as UiState.Success<List<ProductItem>>).data.filter { favorites.contains(it.id) }
                    } else {
                        emptyList()
                    }
                    FavoritesScreen(
                        favoriteProducts = favoriteProducts,
                        isStudent = activeUser.isStudent,
                        onRemoveFavorite = { id ->
                            homeViewModel.toggleFavorite(id)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Eliminado de favoritos")
                            }
                        },
                        onAddToCart = { product ->
                            cartViewModel.addToCart(product)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("¡${product.nombre} añadido al carrito!")
                            }
                        },
                        onExploreMenu = { currentRoute = "home" }
                    )
                }
                "orders" -> {
                    val ordersList = if (ordersState is UiState.Success) {
                        (ordersState as UiState.Success<List<OrderRecord>>).data
                    } else {
                        emptyList()
                    }
                    OrderHistoryScreen(
                        orders = ordersList,
                        onLeaveReview = { order ->
                            reviewTargetOrder = order
                        },
                        onBackToMenu = { currentRoute = "home" }
                    )
                }
                "checkout" -> {
                    CheckoutScreen(
                        cartItems = cartItems,
                        isStudent = activeUser.isStudent,
                        initialName = activeUser.name,
                        initialPhone = activeUser.phone,
                        onBackToMenu = { currentRoute = "home" },
                        onConfirmOrder = { method, total ->
                            if (method == "wallet") {
                                showQrDialog = true
                            } else {
                                val restId = cartItems.firstOrNull()?.product?.restauranteId ?: 1
                                val newOrder = OrderRecord(
                                    id = "FJ-TEMP",
                                    fecha = "Hoy",
                                    estado = OrderStatus.PENDIENTE,
                                    items = cartItems,
                                    subtotal = cartViewModel.getSubtotal(activeUser.isStudent),
                                    impuestos = cartViewModel.getTaxAmount(activeUser.isStudent),
                                    total = total,
                                    paymentMethod = if (method == "card") "tarjeta" else "efectivo"
                                )
                                orderViewModel.createOrder(
                                    order = newOrder,
                                    restauranteId = restId,
                                    metodoPago = if (method == "card") "tarjeta" else "efectivo",
                                    onSuccess = {
                                        cartViewModel.clearCart()
                                        currentRoute = "tracking"
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("¡Pedido registrado exitosamente en el backend!")
                                        }
                                    },
                                    onError = { err ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(err)
                                        }
                                    }
                                )
                            }
                        }
                    )
                }
                "tracking" -> {
                    TrackingScreen(
                        onBackToMenu = { currentRoute = "home" },
                        onViewHistory = { currentRoute = "orders" }
                    )
                }
                "dashboard" -> {
                    AdminDashboardScreen(
                        onBack = { currentRoute = "home" }
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
                onIncreaseQty = { productId ->
                    cartViewModel.increaseQuantity(productId)
                },
                onDecreaseQty = { productId ->
                    cartViewModel.decreaseQuantity(productId)
                },
                onProceedToCheckout = {
                    showCartSheet = false
                    currentRoute = "checkout"
                },
                onClose = { showCartSheet = false }
            )
        }
    }

    // Diálogo de Login
    if (showLoginDialog) {
        LoginDialog(
            onDismiss = { showLoginDialog = false },
            onLoginSuccess = { email, password ->
                authViewModel.login(
                    email = email,
                    password = password,
                    onSuccess = { userName ->
                        showLoginDialog = false
                        // Recargar pedidos del usuario recién logueado
                        orderViewModel.loadOrders()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("¡Bienvenido, $userName!")
                        }
                    },
                    onError = { err ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(err)
                        }
                    }
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
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("¡Cuenta creada exitosamente!")
                        }
                    },
                    onError = { err ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(err)
                        }
                    }
                )
            },
            onSwitchToLogin = {
                showRegisterDialog = false
                showLoginDialog = true
            }
        )
    }

    // Diálogo de Pago QR
    if (showQrDialog) {
        QrPaymentDialog(
            onDismiss = { showQrDialog = false },
            onPaymentConfirmed = {
                showQrDialog = false
                val restId = cartItems.firstOrNull()?.product?.restauranteId ?: 1
                val newOrder = OrderRecord(
                    id = "FJ-TEMP",
                    fecha = "Hoy",
                    estado = OrderStatus.EN_PREPARACION,
                    items = cartItems,
                    subtotal = cartViewModel.getSubtotal(activeUser.isStudent),
                    impuestos = cartViewModel.getTaxAmount(activeUser.isStudent),
                    total = cartViewModel.getTotal(activeUser.isStudent),
                    paymentMethod = "billetera"
                )
                orderViewModel.createOrder(
                    order = newOrder,
                    restauranteId = restId,
                    metodoPago = "billetera",
                    onSuccess = {
                        cartViewModel.clearCart()
                        currentRoute = "tracking"
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("¡Pago confirmado! Preparando tu orden.")
                        }
                    },
                    onError = { err ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(err)
                        }
                    }
                )
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
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("¡Gracias por calificar tu pedido!")
                        }
                    },
                    onError = { err ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(err)
                        }
                    }
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
            onVerifyStudent = {
                authViewModel.verifyStudent {
                    showMiCuentaDialog = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("¡Carnet verificado! Descuentos de estudiante desbloqueados.")
                    }
                }
            },
            onLogout = {
                authViewModel.logout {
                    showMiCuentaDialog = false
                    orderViewModel.loadOrders()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Sesión cerrada")
                    }
                }
            },
            onDismiss = { showMiCuentaDialog = false }
        )
    }
}
