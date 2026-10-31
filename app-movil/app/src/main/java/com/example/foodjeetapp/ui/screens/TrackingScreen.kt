package com.example.foodjeetapp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.foodjeetapp.data.model.Geo
import com.example.foodjeetapp.data.model.GeoPoint
import com.example.foodjeetapp.data.model.OrderRecord
import com.example.foodjeetapp.data.model.OrderStatus
import com.example.foodjeetapp.data.model.Seguimiento
import com.example.foodjeetapp.ui.IntentsFoodJet
import com.example.foodjeetapp.ui.components.DatosMapa
import com.example.foodjeetapp.ui.components.FoodJetFooter
import com.example.foodjeetapp.ui.components.MapaFoodJet
import com.example.foodjeetapp.ui.components.PuntoMapa
import com.example.foodjeetapp.ui.theme.FoodJetPrimary
import com.example.foodjeetapp.ui.theme.FoodJetPrimaryDark
import com.example.foodjeetapp.ui.theme.FoodJetBorder
import com.example.foodjeetapp.ui.theme.FoodJetPrimaryLight
import com.example.foodjeetapp.ui.theme.FoodJetSuccess
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HORA = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
private val FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault())

/** Posición del repartidor y tramos de la ruta en un instante. */
internal data class RepartoEnVivo(
    val posicion: GeoPoint?,
    val recorrida: List<GeoPoint>,
    val pendiente: List<GeoPoint>,
    val avance: Double,
    val restanteMetros: Double?,
    val llegadaMs: Long?
)

/**
 * Por dónde va el repartidor ahora. La posición no se envía: se deriva del
 * momento de salida y la duración del reparto, como en el servidor
 * (`domain/simulacion.js`) y en la web (`domain/seguimiento.js`), y así se
 * puede animar entre consultas. Sin esos datos se usa lo último que dio el
 * servidor; si tampoco hay, no se inventa nada.
 */
internal fun repartoEnVivo(order: OrderRecord, ahoraServidorMs: Long): RepartoEnVivo? {
    val ruta = order.rutaPuntos
    val inicio = Seguimiento.parseIso(order.envioIniciadoEn)
    val duracionMs = order.planTiempos["en_camino"]?.let { (it * 1000).toLong() }
    val entregado = order.estado == OrderStatus.ENTREGADO

    if (ruta.size >= 2 && inicio != null && duracionMs != null) {
        val avance = Geo.avanceDelReparto(inicio, duracionMs, ahoraServidorMs, entregado)
        val partes = Geo.dividirRuta(ruta, avance)
        return RepartoEnVivo(partes.posicion, partes.recorrida, partes.pendiente, avance, partes.restanteMetros, inicio + duracionMs)
    }
    if (ruta.size >= 2 || order.avanceReparto != null) {
        val avance = order.avanceReparto ?: if (entregado) 1.0 else 0.0
        val partes = Geo.dividirRuta(ruta, avance)
        val posicion = if (order.repartidorLat != null && order.repartidorLon != null) {
            GeoPoint(order.repartidorLat, order.repartidorLon)
        } else partes.posicion
        return RepartoEnVivo(
            posicion, partes.recorrida, partes.pendiente, avance,
            order.restanteMetros?.toDouble() ?: partes.restanteMetros.takeIf { ruta.size >= 2 },
            Seguimiento.parseIso(order.llegadaEstimada)
        )
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    order: OrderRecord?,
    pedidoId: Int,
    error: String?,
    onBackToMenu: () -> Unit,
    onViewHistory: () -> Unit,
    onBack: () -> Unit = onBackToMenu,
    onCancelOrder: (orderId: Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Reloj del servidor, que avanza cada segundo solo con la pantalla en
    // primer plano y mientras el pedido va en camino (REQ-SEM04-INF-02).
    val desfase = order?.desfaseRelojMs ?: 0L
    var ahoraMs by remember { mutableLongStateOf(System.currentTimeMillis() + desfase) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val animar = order?.estado == OrderStatus.EN_CAMINO
    LaunchedEffect(order, animar) {
        ahoraMs = System.currentTimeMillis() + desfase
        if (!animar) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                ahoraMs = System.currentTimeMillis() + desfase
                delay(1000)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pedido #$pedidoId", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (order != null) {
                        IconButton(onClick = {
                            IntentsFoodJet.compartir(
                                context,
                                asunto = "Mi pedido FoodJet #${order.numericId}",
                                texto = "Mi pedido #${order.numericId} de ${order.restauranteNombre} está: " +
                                    "${order.estado.label}. Total S/ ${String.format("%.2f", order.total)}."
                            )
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir pedido")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        if (order == null) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (error == null) {
                    CircularProgressIndicator(color = FoodJetPrimary)
                    Spacer(Modifier.height(12.dp))
                    Text("Consultando el pedido #$pedidoId…")
                } else {
                    Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(error, textAlign = TextAlign.Center)
                    Text("Se reintentará automáticamente.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onViewHistory) { Text("Ver Historial de Pedidos") }
            }
            return@Scaffold
        }

        val linea = remember(order, ahoraMs) {
            Seguimiento.lineaDeTiempo(
                estado = order.estado.clave,
                historial = order.historialEstados,
                planSegundos = order.planTiempos,
                fechaPedidoMs = Seguimiento.parseIso(order.fechaIso),
                ahoraMs = ahoraMs
            )
        }
        val reparto = remember(order, ahoraMs) { repartoEnVivo(order, ahoraMs) }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Text(
                        "Sin conexión con el servidor: se muestra el último estado conocido. $error",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (order.estado == OrderStatus.EN_CAMINO) "¡Tu antojo va volando!" else "Sigue tu pedido",
                    fontSize = 28.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Pedido #${order.numericId} · Sigue cada parte del viaje.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            MapaDelPedido(order = order, reparto = reparto)

            Spacer(modifier = Modifier.height(16.dp))

            EstadoActual(order = order, linea = linea, ahoraMs = ahoraMs)

            Spacer(modifier = Modifier.height(16.dp))

            if (order.estado != OrderStatus.CANCELADO) {
                LineaDeTiempo(linea)
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (order.estado == OrderStatus.EN_CAMINO && reparto != null) {
                Spacer(modifier = Modifier.height(12.dp))
                TarjetaReparto(reparto = reparto, ahoraMs = ahoraMs)
            }

            Spacer(modifier = Modifier.height(16.dp))
            DetallesDelPedido(order)

            if (order.historialEstados.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HistorialDeEstados(order)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (order.estado.isCancelable) {
                OutlinedButton(
                    onClick = { onCancelOrder(order.numericId) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cancelar Pedido", fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Button(
                onClick = onBackToMenu,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FoodJetPrimary, contentColor = Color.White)
            ) {
                Text("Volver al Menú", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onViewHistory, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Ver Historial de Pedidos")
            }
            FoodJetFooter()
        }
    }
}

@Composable
private fun EstadoActual(order: OrderRecord, linea: Seguimiento.Linea, ahoraMs: Long) {
    val status = order.estado
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val (titulo, descripcion, icono, color) = when (status) {
        OrderStatus.PENDIENTE -> Quadruple(
            "Confirmando tu pedido",
            "Estamos confirmando tu pedido con el restaurante.",
            Icons.Default.HourglassEmpty, FoodJetPrimary
        )
        OrderStatus.CONFIRMADO -> Quadruple(
            "¡Pedido Confirmado!",
            "El restaurante recibió tu pedido y se prepara para cocinar.",
            Icons.AutoMirrored.Filled.ReceiptLong, FoodJetPrimary
        )
        OrderStatus.EN_PREPARACION -> Quadruple(
            "Preparando tu pedido",
            "Tu comida se está preparando en cocina.",
            Icons.Default.SoupKitchen, FoodJetPrimaryDark
        )
        OrderStatus.EN_CAMINO -> Quadruple(
            "¡Pedido en camino!",
            "El repartidor va hacia tu dirección de entrega.",
            Icons.AutoMirrored.Filled.DirectionsBike, FoodJetPrimary
        )
        OrderStatus.ENTREGADO -> Quadruple(
            "¡Pedido Entregado!",
            "Tu comida fue entregada. ¡Buen provecho!",
            Icons.Default.CheckCircle, FoodJetSuccess
        )
        OrderStatus.CANCELADO -> Quadruple(
            "Pedido Cancelado",
            "Este pedido fue cancelado.",
            Icons.Default.Cancel, MaterialTheme.colorScheme.error
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, FoodJetBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(if (!status.isTerminal) pulseScale else 1.0f)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = titulo.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            }

            // Llegada: real si ya ocurrió, estimada con el plan del backend si no.
            val llegada = linea.llegadaMs?.takeIf { !linea.cancelado }
            if (llegada != null && status != OrderStatus.ENTREGADO) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = Geo.formatearRestante(llegada - ahoraMs),
                    fontSize = 34.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(descripcion, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (llegada != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (status == OrderStatus.ENTREGADO) {
                        "Entregado a las ${HORA.format(Instant.ofEpochMilli(llegada))}"
                    } else {
                        "Llegada estimada: ${HORA.format(Instant.ofEpochMilli(llegada))}"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun LineaDeTiempo(linea: Seguimiento.Linea) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, FoodJetBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            linea.etapas.forEachIndexed { i, etapa ->
                val actual = etapa.situacion == Seguimiento.Situacion.EN_CURSO
                val hecha = etapa.situacion == Seguimiento.Situacion.HECHA
                val ultima = i == linea.etapas.lastIndex
                val hora = etapa.horaMs?.let { HORA.format(Instant.ofEpochMilli(it)) + if (etapa.estimada) " aprox." else "" }

                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PasoIndicador(actual = actual, hecha = hecha)
                        if (!ultima) {
                            val siguiente = linea.etapas[i + 1].situacion
                            val colorLinea by animateColorAsState(
                                targetValue = if (siguiente == Seguimiento.Situacion.HECHA || siguiente == Seguimiento.Situacion.EN_CURSO) {
                                    FoodJetSuccess
                                } else FoodJetBorder,
                                label = "lineColor"
                            )
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .weight(1f)
                                    .background(colorLinea)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = if (ultima) 0.dp else 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = etapa.etiqueta,
                            fontSize = 15.sp,
                            fontWeight = if (actual || hecha) FontWeight.Bold else FontWeight.Normal,
                            color = if (actual || hecha) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (actual) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(
                                    "Ahora",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        } else if (hora != null) {
                            Text(hora, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PasoIndicador(actual: Boolean, hecha: Boolean) {
    val fondo by animateColorAsState(
        targetValue = when {
            hecha -> FoodJetSuccess
            actual -> FoodJetPrimary
            else -> FoodJetBorder
        },
        label = "stepColor"
    )
    Box(
        modifier = Modifier.size(28.dp).clip(CircleShape).background(fondo),
        contentAlignment = Alignment.Center
    ) {
        when {
            hecha -> Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            actual -> Icon(Icons.AutoMirrored.Filled.DirectionsBike, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun MapaDelPedido(order: OrderRecord, reparto: RepartoEnVivo?) {
    val context = LocalContext.current
    val local = order.restauranteLatitud?.let { lat ->
        order.restauranteLongitud?.let { lon -> GeoPoint(lat, lon) }
    }
    val destino = order.latitudEntrega?.let { lat ->
        order.longitudEntrega?.let { lon -> GeoPoint(lat, lon) }
    }
    if (local == null && destino == null) return

    val datos = DatosMapa(
        restaurante = local?.let { PuntoMapa(it, order.restauranteNombre, order.restauranteDireccion.orEmpty()) },
        destino = destino?.let { PuntoMapa(it, "Tu dirección", order.direccionEntrega.orEmpty()) },
        repartidor = reparto?.posicion?.takeIf { order.estado == OrderStatus.EN_CAMINO }?.let {
            PuntoMapa(it, "Repartidor", reparto.restanteMetros?.let { m -> "Faltan ${Geo.formatearDistancia(m)}" }.orEmpty())
        },
        recorrida = reparto?.recorrida.orEmpty(),
        pendiente = reparto?.pendiente ?: order.rutaPuntos
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box {
                MapaFoodJet(datos = datos, modifier = Modifier.fillMaxWidth().height(260.dp))
                if (order.estado == OrderStatus.EN_CAMINO) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(FoodJetSuccess))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Actualizando en vivo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val distanciaRuta = order.distanciaMetros ?: order.rutaPuntos.takeIf { it.size >= 2 }?.let(Geo::longitudRuta)
                Text(
                    text = when {
                        distanciaRuta != null -> "Recorrido: ${Geo.formatearDistancia(distanciaRuta)}"
                        local != null && destino != null ->
                            "Distancia en línea recta: ${Geo.formatearDistancia(Geo.distanciaMetros(local, destino))}"
                        else -> order.direccionEntrega.orEmpty()
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                destino?.let {
                    TextButton(onClick = { IntentsFoodJet.abrirEnMapas(context, it, "Entrega FoodJet") }) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Abrir en Mapas", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaReparto(reparto: RepartoEnVivo, ahoraMs: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.DirectionsBike, contentDescription = null, tint = FoodJetPrimaryDark)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reparto en vivo", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                reparto.llegadaMs?.let {
                    Text(Geo.formatearRestante(it - ahoraMs), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = FoodJetPrimaryDark)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { reparto.avance.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = FoodJetPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${(reparto.avance * 100).toInt()}% recorrido", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                reparto.restanteMetros?.let {
                    Text("Faltan ${Geo.formatearDistancia(it)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = FoodJetPrimaryDark)
                }
            }
            Text(
                "Reparto simulado por el servidor: la posición sale de la ruta y la hora de salida, no del GPS de un repartidor.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun DetallesDelPedido(order: OrderRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tu pedido #${order.numericId}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(10.dp))
            DetailRow("Local:", order.restauranteNombre)
            DetailRow("Método de pago:", order.paymentMethod)
            DetailRow(
                "Fecha:",
                Seguimiento.parseIso(order.fechaIso)?.let { FECHA_HORA.format(Instant.ofEpochMilli(it)) } ?: order.fecha
            )
            DetailRow("Estado actual:", order.estado.label, isBold = true)
            order.direccionEntrega?.takeIf { it.isNotBlank() }?.let { DetailRow("Entrega en:", it) }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            order.items.forEach { item ->
                DetailRow("${item.quantity}x ${item.product.nombre}", "S/ ${String.format("%.2f", item.product.precio * item.quantity)}")
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DetailRow("Impuestos (18%):", "S/ ${String.format("%.2f", order.impuestos)}")
            DetailRow("Envío:", "S/ ${String.format("%.2f", order.envio)}")
            DetailRow("Total Final:", "S/ ${String.format("%.2f", order.total)}", isBold = true)
        }
    }
}

@Composable
private fun HistorialDeEstados(order: OrderRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null, tint = FoodJetPrimaryDark, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Historial de Actividad", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            order.historialEstados.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            entry.estado.replace('_', ' ').replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("Por: ${entry.origen}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        Seguimiento.parseIso(entry.fecha)?.let { FECHA_HORA.format(Instant.ofEpochMilli(it)) } ?: entry.fecha,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isBold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = if (isBold) 13.sp else 12.sp,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isBold) FoodJetPrimary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
