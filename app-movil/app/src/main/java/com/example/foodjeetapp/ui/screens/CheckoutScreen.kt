package com.example.foodjeetapp.ui.screens

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.foodjeetapp.data.common.UiState
import com.example.foodjeetapp.data.di.ServiceLocator
import com.example.foodjeetapp.data.model.CartItem
import com.example.foodjeetapp.data.model.DeliverySelection
import com.example.foodjeetapp.data.model.Geo
import com.example.foodjeetapp.data.model.GeoPoint
import com.example.foodjeetapp.data.model.SavedAddress
import com.example.foodjeetapp.data.remote.dto.RestaurantDto
import com.example.foodjeetapp.ui.IntentsFoodJet
import com.example.foodjeetapp.ui.components.ControladorMapa
import com.example.foodjeetapp.ui.components.DatosMapa
import com.example.foodjeetapp.ui.components.MapaFoodJet
import com.example.foodjeetapp.ui.components.FoodJetFooter
import com.example.foodjeetapp.ui.components.FoodJetSectionHeader
import com.example.foodjeetapp.ui.components.ModoMapa
import com.example.foodjeetapp.ui.components.PuntoMapa
import com.example.foodjeetapp.ui.theme.FoodJetBorder
import com.example.foodjeetapp.ui.theme.FoodJetPrimary
import com.example.foodjeetapp.ui.theme.FoodJetPrimaryLight
import com.example.foodjeetapp.ui.theme.FoodJetSuccessDeep
import com.example.foodjeetapp.ui.theme.FoodJetSuccessSoft
import com.example.foodjeetapp.ui.theme.FoodJetTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Cupones que existen en la base del backend (`backend/prisma/seed.js`). El
 * backend no tiene un endpoint para buscar un cupón por su código, solo
 * acepta `cupon_id` al crear el pedido, así que el código se traduce aquí; el
 * descuento lo vuelve a calcular y validar el servidor.
 */
private val CUPONES_BACKEND = mapOf(
    "pruebacupon" to Triple(1, 0.10, "PruebaCupon"),
    "foodjet20" to Triple(2, 0.20, "FOODJET20")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartItems: List<CartItem>,
    isStudent: Boolean,
    isLoggedIn: Boolean,
    initialName: String,
    initialPhone: String,
    addressesState: UiState<List<SavedAddress>>,
    isSubmitting: Boolean,
    onRequireLogin: () -> Unit,
    onRetryAddresses: () -> Unit,
    onBackToMenu: () -> Unit,
    onConfirmar: (paymentMethod: String, cuponId: Int?, entrega: DeliverySelection) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationProvider = remember { ServiceLocator.locationProvider }
    val controladorMapa = remember { ControladorMapa() }

    var name by remember(initialName) { mutableStateOf(initialName) }
    var phone by remember(initialPhone) { mutableStateOf(initialPhone) }
    var address by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var punto by remember { mutableStateOf<GeoPoint?>(null) }
    var precisionGps by remember { mutableStateOf<Double?>(null) }
    var direccionGuardada by remember { mutableStateOf<SavedAddress?>(null) }
    var mapaDisponible by remember { mutableStateOf<Boolean?>(null) }
    var textoEditadoAMano by remember { mutableStateOf(false) }
    var avisoUbicacion by remember { mutableStateOf<String?>(null) }
    var buscandoUbicacion by remember { mutableStateOf(false) }
    var mostrarExplicacionPermiso by remember { mutableStateOf(false) }
    var local by remember { mutableStateOf<RestaurantDto?>(null) }
    var primerReposo by remember { mutableStateOf(true) }

    // Local del carrito, para mostrarlo en el mapa y medir la distancia.
    val restauranteId = cartItems.firstOrNull()?.product?.restauranteId
    LaunchedEffect(restauranteId) {
        local = restauranteId?.let { ServiceLocator.orderRepository.getRestaurant(it).getOrNull() }
    }
    val puntoLocal = local?.let { l ->
        if (Geo.coordenadasValidas(l.latitud, l.longitud)) GeoPoint(l.latitud!!, l.longitud!!) else null
    }

    // Al cargar las direcciones se abre con la predeterminada que esté ubicada.
    val guardadas = (addressesState as? UiState.Success<List<SavedAddress>>)?.data.orEmpty()
    LaunchedEffect(guardadas) {
        if (address.isBlank() && punto == null) {
            val inicial = guardadas.filter { it.punto != null }.let { u -> u.firstOrNull { it.predeterminada } ?: u.firstOrNull() }
            inicial?.let {
                direccionGuardada = it
                address = it.direccion
                reference = it.referencia.orEmpty()
                punto = it.punto
                it.punto?.let(controladorMapa::irA)
            }
        }
    }

    // Si el punto llegó antes de que cargara Google Maps, se lleva el mapa a él.
    LaunchedEffect(mapaDisponible) {
        if (mapaDisponible == true) punto?.let(controladorMapa::irA)
    }

    // Al soltar el mapa se rellena la dirección con el geocodificador, salvo
    // que el cliente la haya escrito a mano (REQ-SEM10-LOG-01).
    LaunchedEffect(punto) {
        val actual = punto ?: return@LaunchedEffect
        val guardada = direccionGuardada
        if (guardada?.punto != null && Geo.distanciaMetros(guardada.punto!!, actual) > Geo.MISMA_UBICACION_M) {
            direccionGuardada = null
        }
        if (textoEditadoAMano || direccionGuardada != null) return@LaunchedEffect
        delay(600)
        locationProvider.direccionDe(actual)?.let { address = it }
    }

    fun leerGps() {
        if (!locationProvider.ubicacionActivada()) {
            avisoUbicacion = "La ubicación del teléfono está apagada."
            return
        }
        scope.launch {
            buscandoUbicacion = true
            avisoUbicacion = null
            val lectura = locationProvider.ubicacionActual()
            buscandoUbicacion = false
            if (lectura == null) {
                avisoUbicacion = "No se pudo leer tu ubicación. Marca el punto en el mapa."
                return@launch
            }
            if (!Geo.dentroDeLima(lectura.punto)) {
                avisoUbicacion = "Tu ubicación está fuera de Lima, donde reparte FoodJet. Revisa el punto antes de confirmar."
            }
            direccionGuardada = null
            textoEditadoAMano = false
            precisionGps = lectura.precisionMetros
            punto = lectura.punto
            controladorMapa.irA(lectura.punto)
        }
    }

    // Permiso en tiempo de ejecución (REQ-SEM10-LOG-02): basta con el
    // aproximado; si el cliente da el preciso, mejor.
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
        if (resultado.values.any { it }) {
            leerGps()
        } else {
            val activity = context as? Activity
            val bloqueado = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
            avisoUbicacion = if (bloqueado) {
                "El permiso de ubicación está bloqueado. Puedes activarlo en Ajustes o marcar el punto en el mapa."
            } else {
                "Sin permiso de ubicación: marca el punto en el mapa o escribe tu dirección."
            }
        }
    }

    fun usarMiUbicacion() {
        when {
            locationProvider.tienePermiso() -> leerGps()
            (context as? Activity)?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_COARSE_LOCATION)
            } == true -> mostrarExplicacionPermiso = true
            else -> pedirPermiso.launch(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            )
        }
    }

    fun buscarEscrita() {
        val texto = address.trim()
        if (texto.length < 5) return
        scope.launch {
            val encontrado = locationProvider.buscarDireccion(texto)
            if (encontrado == null) {
                avisoUbicacion = "No encontramos esa dirección. Mueve el mapa hasta tu puerta."
            } else {
                avisoUbicacion = null
                textoEditadoAMano = true
                direccionGuardada = null
                precisionGps = null
                punto = encontrado
                controladorMapa.irA(encontrado)
            }
        }
    }

    // Validación reactiva (REQ-SEM04-VIS-01 y REQ-SEM04-LOG-03)
    val isNameValid = name.trim().length >= 3
    val isPhoneValid = phone.trim().matches(Regex("^\\d{9}$"))
    val isAddressValid = address.trim().length >= 5
    // Con mapa, el punto es obligatorio: es el destino del repartidor.
    val faltaPunto = mapaDisponible == true && punto == null
    val isFormValid = isLoggedIn && isNameValid && isPhoneValid && isAddressValid && !faltaPunto &&
        cartItems.isNotEmpty() && !isSubmitting

    // Selección de Método de Pago: canónicos 'cash', 'card', 'wallet'
    var paymentMethod by remember { mutableStateOf("cash") } // cash, card, wallet
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvc by remember { mutableStateOf("") }

    var couponCode by remember { mutableStateOf("") }
    var couponAppliedId by remember { mutableStateOf<Int?>(null) }
    var couponDiscountPercent by remember { mutableStateOf(0.0) }
    var couponMessage by remember { mutableStateOf<String?>(null) }

    // Vista previa; el importe definitivo lo calcula el backend al registrar el pedido.
    val subtotal = cartItems.sumOf { it.product.getEffectivePrice(isStudent) * it.quantity }
    val discount = if (couponAppliedId != null) subtotal * couponDiscountPercent else 0.0
    val subtotalAfterDiscount = (subtotal - discount).coerceAtLeast(0.0)
    val taxes = subtotalAfterDiscount * 0.18
    val deliveryFee = 5.00
    val total = subtotalAfterDiscount + taxes + deliveryFee

    if (mostrarExplicacionPermiso) {
        AlertDialog(
            onDismissRequest = { mostrarExplicacionPermiso = false },
            icon = { Icon(Icons.Default.MyLocation, contentDescription = null) },
            title = { Text("¿Usar tu ubicación?") },
            text = {
                Text(
                    "FoodJet usa tu ubicación una sola vez, para poner el pin del mapa en tu puerta. " +
                        "No la seguimos en segundo plano. También puedes mover el mapa a mano."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    mostrarExplicacionPermiso = false
                    pedirPermiso.launch(
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                    )
                }) { Text("Continuar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarExplicacionPermiso = false }) { Text("Ahora no") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Volver al carrito", fontSize = 15.sp, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBackToMenu) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text("A un paso del antojo.", fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Confirma tus datos y elige cómo pagar.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isLoggedIn) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Inicia sesión para registrar el pedido en tu cuenta.",
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            TextButton(onClick = onRequireLogin) { Text("Iniciar sesión") }
                        }
                    }
                }
            }

            // 1. Información de Entrega
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, FoodJetBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FoodJetSectionHeader(Icons.Default.LocationOn, "¿Dónde lo llevamos?")
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it.take(60) },
                            label = { Text("Nombre completo") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = !isNameValid && name.isNotEmpty(),
                            supportingText = {
                                if (!isNameValid && name.isNotEmpty()) Text("Debe contener al menos 3 caracteres")
                            },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { nuevo -> phone = nuevo.filter(Char::isDigit).take(9) },
                            label = { Text("Teléfono de contacto (9 dígitos)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = !isPhoneValid && phone.isNotEmpty(),
                            supportingText = {
                                if (!isPhoneValid && phone.isNotEmpty()) Text("Debe tener exactamente 9 dígitos numéricos")
                            },
                            singleLine = true
                        )

                        // Direcciones guardadas en el backend
                        when (addressesState) {
                            is UiState.Success -> if (guardadas.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Tus direcciones", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    guardadas.forEach { guardada ->
                                        FilterChip(
                                            selected = direccionGuardada?.id == guardada.id,
                                            onClick = {
                                                direccionGuardada = guardada
                                                address = guardada.direccion
                                                reference = guardada.referencia.orEmpty()
                                                precisionGps = null
                                                punto = guardada.punto
                                                guardada.punto?.let(controladorMapa::irA)
                                            },
                                            label = { Text(guardada.direccion, maxLines = 1, fontSize = 12.sp) },
                                            leadingIcon = if (guardada.predeterminada) {
                                                { Icon(Icons.Default.Home, contentDescription = "Predeterminada", modifier = Modifier.size(16.dp)) }
                                            } else null
                                        )
                                    }
                                }
                            }
                            is UiState.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    addressesState.message,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onRetryAddresses) { Text("Reintentar") }
                            }
                            else -> Unit
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = address,
                            onValueChange = {
                                address = it.take(200)
                                textoEditadoAMano = true
                                direccionGuardada = null
                            },
                            label = { Text("Dirección de entrega") },
                            placeholder = { Text("Calle, número y distrito") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { buscarEscrita() }),
                            trailingIcon = {
                                IconButton(onClick = ::buscarEscrita, enabled = address.trim().length >= 5) {
                                    Icon(Icons.Default.Search, contentDescription = "Buscar dirección en el mapa")
                                }
                            },
                            isError = !isAddressValid && address.isNotEmpty(),
                            supportingText = {
                                if (!isAddressValid && address.isNotEmpty()) Text("Por favor ingresa una dirección válida")
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = reference,
                            onValueChange = { reference = it.take(120) },
                            label = { Text("Referencia (Opcional)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Mapa con pin fijo en el centro (REQ-SEM10-VIS-01 / VIS-02)
                        MapaFoodJet(
                            modo = ModoMapa.SELECCION,
                            controlador = controladorMapa,
                            centroInicial = punto ?: puntoLocal,
                            datos = DatosMapa(
                                restaurante = puntoLocal?.let { PuntoMapa(it, local?.nombre ?: "Local", local?.direccion.orEmpty()) },
                                // Con Google Maps el pin fijo del centro ya marca la entrega; el esquema necesita el punto.
                                destino = punto?.takeIf { mapaDisponible == false }?.let { PuntoMapa(it, "Entrega", address) },
                                cobertura = punto?.let { p -> precisionGps?.takeIf { it > 0 }?.let { p to it } }
                            ),
                            onCentroCambiado = { centro ->
                                // El primer reposo es el encuadre inicial, no una elección del cliente.
                                if (primerReposo) {
                                    primerReposo = false
                                    return@MapaFoodJet
                                }
                                if (punto == null || Geo.distanciaMetros(centro, punto!!) > 2) {
                                    if (precisionGps != null && punto != null &&
                                        Geo.distanciaMetros(centro, punto!!) > Geo.MISMA_UBICACION_M) precisionGps = null
                                    punto = centro
                                }
                            },
                            onDisponibilidad = { mapaDisponible = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = ::usarMiUbicacion,
                                enabled = !buscandoUbicacion,
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                if (buscandoUbicacion) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Usar mi ubicación")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            punto?.let { p ->
                                val distancia = puntoLocal?.let { Geo.distanciaMetros(it, p) }
                                Text(
                                    text = buildString {
                                        append("%.5f, %.5f".format(p.latitud, p.longitud))
                                        if (distancia != null) append("\nA ${Geo.formatearDistancia(distancia)} del local (en línea recta)")
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        avisoUbicacion?.let { aviso ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(aviso, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            if (aviso.contains("Ajustes")) {
                                TextButton(onClick = { IntentsFoodJet.abrirAjustesDeLaApp(context) }) { Text("Abrir Ajustes") }
                            } else if (aviso.contains("apagada")) {
                                TextButton(onClick = { IntentsFoodJet.abrirAjustesDeUbicacion(context) }) { Text("Activar ubicación") }
                            }
                        }
                        if (faltaPunto) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Mueve el mapa hasta tu puerta o usa tu ubicación para marcar el punto de entrega.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // 2. Método de Pago
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, FoodJetBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FoodJetSectionHeader(Icons.Default.Payments, "¿Cómo quieres pagar?")
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PaymentTile("Yape / Plin", "Escanea el QR", Icons.Default.QrCode, paymentMethod == "wallet", { paymentMethod = "wallet" }, Modifier.weight(1f))
                            PaymentTile("Tarjeta", "Crédito o débito", Icons.Default.CreditCard, paymentMethod == "card", { paymentMethod = "card" }, Modifier.weight(1f))
                            PaymentTile("Efectivo", "Paga al recibir", Icons.Default.AttachMoney, paymentMethod == "cash", { paymentMethod = "cash" }, Modifier.weight(1f))
                        }

                        AnimatedVisibility(visible = paymentMethod == "card") {
                            Column(modifier = Modifier.padding(top = 10.dp, start = 8.dp, end = 8.dp)) {
                                OutlinedTextField(
                                    value = cardNumber,
                                    onValueChange = { cardNumber = it.filter(Char::isDigit).take(16) },
                                    label = { Text("Número de tarjeta") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = cardExpiry,
                                        onValueChange = { cardExpiry = it.take(5) },
                                        label = { Text("MM/AA") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    OutlinedTextField(
                                        value = cardCvc,
                                        onValueChange = { cardCvc = it.filter(Char::isDigit).take(4) },
                                        label = { Text("CVC") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = paymentMethod == "wallet") {
                            Column(
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(FoodJetPrimaryLight)
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Paga con Yape o Plin", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "S/ ${String.format("%.2f", total)}",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    "Paga escaneando el código QR del local",
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AnimatedVisibility(visible = paymentMethod == "cash") {
                            Text(
                                "Paga contraentrega cuando recibas tu pedido.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tus pagos y datos están protegidos.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // 3. Resumen y Cupón
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, FoodJetBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FoodJetSectionHeader(Icons.Default.ShoppingBag, "Tu pedido")
                        Spacer(modifier = Modifier.height(12.dp))

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${cartItems.firstOrNull()?.product?.restauranteNombre ?: "FoodJet"} · ${cartItems.sumOf { it.quantity }} productos",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        cartItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(item.product.imagenUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = item.product.nombre,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.product.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                                    Text(
                                        "Cantidad: ${item.quantity}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "S/ ${String.format("%.2f", item.product.getEffectivePrice(isStudent) * item.quantity)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = couponCode,
                                onValueChange = { couponCode = it.take(30) },
                                label = { Text("Código de cupón") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val cupon = CUPONES_BACKEND[couponCode.trim().lowercase()]
                                    if (cupon != null) {
                                        couponAppliedId = cupon.first
                                        couponDiscountPercent = cupon.second
                                        couponMessage = "Cupón '${cupon.third}' (${(cupon.second * 100).toInt()}%) aplicado"
                                    } else {
                                        couponAppliedId = null
                                        couponDiscountPercent = 0.0
                                        couponMessage = "Código de cupón no válido"
                                    }
                                },
                                enabled = couponCode.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FoodJetPrimary, contentColor = Color.White)
                            ) {
                                Text("Aplicar")
                            }
                        }

                        couponMessage?.let { mensaje ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = mensaje,
                                fontSize = 12.sp,
                                color = if (couponAppliedId != null) FoodJetSuccessDeep else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (couponAppliedId != null) FoodJetSuccessSoft else Color.Transparent)
                                    .padding(horizontal = 10.dp, vertical = if (couponAppliedId != null) 8.dp else 0.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        FinanceRow("Subtotal", "S/ ${String.format("%.2f", subtotal)}")
                        if (couponAppliedId != null) {
                            FinanceRow("Descuento (${(couponDiscountPercent * 100).toInt()}%)", "-S/ ${String.format("%.2f", discount)}", isDiscount = true)
                        }
                        FinanceRow("Impuestos (18% IGV)", "S/ ${String.format("%.2f", taxes)}")
                        FinanceRow("Costo de Delivery", "S/ ${String.format("%.2f", deliveryFee)}")

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total a Pagar", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                text = "S/ ${String.format("%.2f", total)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            "El servidor confirma el importe final al registrar el pedido.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                onConfirmar(
                                    paymentMethod,
                                    couponAppliedId,
                                    DeliverySelection(
                                        direccion = address.trim(),
                                        referencia = reference.trim().ifEmpty { null },
                                        punto = punto,
                                        direccionGuardadaId = direccionGuardada?.id
                                    )
                                )
                            },
                            enabled = isFormValid,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FoodJetPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = when {
                                    isSubmitting -> "Registrando..."
                                    paymentMethod == "wallet" -> "Ya pagué · S/ ${String.format("%.2f", total)}"
                                    else -> "Confirmar pedido · S/ ${String.format("%.2f", total)}"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (!isSubmitting) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            item { FoodJetFooter() }
        }
    }
}

@Composable
private fun PaymentTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) FoodJetPrimary else FoodJetBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, lineHeight = 12.sp)
        }
    }
}

@Composable
private fun FinanceRow(label: String, value: String, isDiscount: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (isDiscount) FoodJetSuccessDeep else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDiscount) FoodJetSuccessDeep else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(name = "PaymentTile - Light Mode", showBackground = true)
@Composable
private fun PaymentTileLightPreview() {
    FoodJetTheme(darkTheme = false) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentTile(
                title = "Efectivo",
                subtitle = "Pagas al recibir",
                icon = Icons.Default.AttachMoney,
                selected = true,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            PaymentTile(
                title = "Tarjeta",
                subtitle = "Crédito o débito",
                icon = Icons.Default.CreditCard,
                selected = false,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(name = "PaymentTile - Dark Mode", showBackground = true)
@Composable
private fun PaymentTileDarkPreview() {
    FoodJetTheme(darkTheme = true) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentTile(
                title = "Billetera QR",
                subtitle = "Yape o Plin",
                icon = Icons.Default.QrCode,
                selected = true,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            PaymentTile(
                title = "Efectivo",
                subtitle = "Pagas al recibir",
                icon = Icons.Default.AttachMoney,
                selected = false,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }
    }
}
