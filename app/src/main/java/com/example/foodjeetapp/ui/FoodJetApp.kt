package com.example.foodjeetapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.foodjeetapp.data.model.*
import com.example.foodjeetapp.ui.components.FoodJetBottomBar
import com.example.foodjeetapp.ui.components.FoodJetTopBar
import com.example.foodjeetapp.ui.screens.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodJetApp() {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estados de datos
    var currentUser by remember { mutableStateOf(UserProfile(isStudent = true)) }
    var isLoggedIn by remember { mutableStateOf(true) }
    var favorites by remember { mutableStateOf(setOf(1, 3)) } // Hamburguesa y Pizza Pepperoni en favoritos por defecto
    var cartItems by remember {
        mutableStateOf(
            listOf(
                CartItem(FoodJetMockData.products[0], 1)
            )
        )
    }
    var orders by remember { mutableStateOf(FoodJetMockData.initialOrders) }

    // Estados de navegación y vistas
    var currentRoute by remember { mutableStateOf("home") } // home, menu, favorites, orders, profile, checkout, tracking, dashboard

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
                    userName = currentUser.name,
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
                        isStudent = currentUser.isStudent,
                        favorites = favorites,
                        onFavoriteToggle = { id ->
                            favorites = if (favorites.contains(id)) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Eliminado de favoritos")
                                }
                                favorites - id
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("¡Añadido a favoritos! ❤️")
                                }
                                favorites + id
                            }
                        },
                        onAddToCart = { product ->
                            val existingIndex = cartItems.indexOfFirst { it.product.id == product.id }
                            cartItems = if (existingIndex != -1) {
                                cartItems.mapIndexed { idx, item ->
                                    if (idx == existingIndex) item.copy(quantity = item.quantity + 1) else item
                                }
                            } else {
                                cartItems + CartItem(product, 1)
                            }
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("¡${product.nombre} añadido al carrito!")
                            }
                        },
                        onNavigateToMenu = {
                            currentRoute = "menu"
                        }
                    )
                }
                "favorites" -> {
                    val favoriteProducts = FoodJetMockData.products.filter { favorites.contains(it.id) }
                    FavoritesScreen(
                        favoriteProducts = favoriteProducts,
                        isStudent = currentUser.isStudent,
                        onRemoveFavorite = { id ->
                            favorites = favorites - id
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Eliminado de favoritos")
                            }
                        },
                        onAddToCart = { product ->
                            val existingIndex = cartItems.indexOfFirst { it.product.id == product.id }
                            cartItems = if (existingIndex != -1) {
                                cartItems.mapIndexed { idx, item ->
                                    if (idx == existingIndex) item.copy(quantity = item.quantity + 1) else item
                                }
                            } else {
                                cartItems + CartItem(product, 1)
                            }
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("¡${product.nombre} añadido al carrito!")
                            }
                        },
                        onExploreMenu = { currentRoute = "home" }
                    )
                }
                "orders" -> {
                    OrderHistoryScreen(
                        orders = orders,
                        onLeaveReview = { order ->
                            reviewTargetOrder = order
                        },
                        onBackToMenu = { currentRoute = "home" }
                    )
                }
                "checkout" -> {
                    CheckoutScreen(
                        cartItems = cartItems,
                        isStudent = currentUser.isStudent,
                        initialName = currentUser.name,
                        initialPhone = currentUser.phone,
                        onBackToMenu = { currentRoute = "home" },
                        onConfirmOrder = { method, total ->
                            if (method == "wallet") {
                                showQrDialog = true
                            } else {
                                // Crear nuevo registro de pedido
                                val newOrder = OrderRecord(
                                    id = "FJ-1004",
                                    fecha = "6 de Septiembre, 2026",
                                    estado = OrderStatus.PENDIENTE,
                                    items = cartItems,
                                    subtotal = cartItems.sumOf { it.product.getEffectivePrice(currentUser.isStudent) * it.quantity },
                                    impuestos = total * 0.18,
                                    total = total,
                                    paymentMethod = if (method == "card") "Tarjeta" else "Efectivo"
                                )
                                orders = listOf(newOrder) + orders
                                cartItems = emptyList()
                                currentRoute = "tracking"
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

    // Modal BottomSheet del Carrito (Enfoque Híbrido Móvil)
    if (showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = bottomSheetState
        ) {
            CartSheet(
                cartItems = cartItems,
                isStudent = currentUser.isStudent,
                onIncreaseQty = { productId ->
                    cartItems = cartItems.map {
                        if (it.product.id == productId) it.copy(quantity = it.quantity + 1) else it
                    }
                },
                onDecreaseQty = { productId ->
                    cartItems = cartItems.mapNotNull {
                        if (it.product.id == productId) {
                            if (it.quantity > 1) it.copy(quantity = it.quantity - 1) else null
                        } else it
                    }
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
            onLoginSuccess = { email ->
                isLoggedIn = true
                currentUser = currentUser.copy(email = email)
                showLoginDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("¡Bienvenido de vuelta, ${currentUser.name}!")
                }
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
            onRegisterSuccess = { name, email, phone ->
                isLoggedIn = true
                currentUser = UserProfile(name = name, email = email, phone = phone, isStudent = false)
                showRegisterDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("¡Cuenta creada exitosamente!")
                }
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
                val newOrder = OrderRecord(
                    id = "FJ-1004",
                    fecha = "6 de Septiembre, 2026",
                    estado = OrderStatus.EN_PREPARACION,
                    items = cartItems,
                    subtotal = cartItems.sumOf { it.product.getEffectivePrice(currentUser.isStudent) * it.quantity },
                    impuestos = 3.50,
                    total = 28.50,
                    paymentMethod = "Billetera Digital (Yape)"
                )
                orders = listOf(newOrder) + orders
                cartItems = emptyList()
                currentRoute = "tracking"
            }
        )
    }

    // Diálogo de Calificar Pedido
    reviewTargetOrder?.let { order ->
        ReviewDialog(
            order = order,
            onDismiss = { reviewTargetOrder = null },
            onSubmitReview = { orderId, stars, comment ->
                orders = orders.map {
                    if (it.id == orderId) it.copy(reviewStars = stars, reviewComment = comment) else it
                }
                reviewTargetOrder = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("¡Gracias por calificar tu pedido!")
                }
            }
        )
    }

    // Diálogo Mi Cuenta
    if (showMiCuentaDialog) {
        MiCuentaDialog(
            userName = currentUser.name,
            userEmail = currentUser.email,
            isStudent = currentUser.isStudent,
            onVerifyStudent = {
                currentUser = currentUser.copy(isStudent = true)
                showMiCuentaDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("¡Carnet verificado! Descuentos de estudiante desbloqueados.")
                }
            },
            onLogout = {
                isLoggedIn = false
                showMiCuentaDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Sesión cerrada")
                }
            },
            onDismiss = { showMiCuentaDialog = false }
        )
    }
}
