package com.example.foodjeetapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodjeetapp.data.common.UiState
import com.example.foodjeetapp.data.model.EstadisticasAdmin
import com.example.foodjeetapp.data.model.Seguimiento
import com.example.foodjeetapp.data.remote.dto.AdminOrderDto
import com.example.foodjeetapp.ui.theme.FoodJetPrimary
import com.example.foodjeetapp.ui.theme.FoodJetPrimaryDark
import com.example.foodjeetapp.ui.theme.FoodJetSuccess

private val ETIQUETA_ESTADO = mapOf(
    "pendiente" to "Pendiente",
    "confirmado" to "Confirmado",
    "en_preparacion" to "En preparación",
    "en_camino" to "En camino",
    "entregado" to "Entregado",
    "cancelado" to "Cancelado"
)

private val SIGUIENTE_ESTADO = mapOf(
    "pendiente" to "confirmado",
    "confirmado" to "en_preparacion",
    "en_preparacion" to "en_camino",
    "en_camino" to "entregado"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    adminName: String,
    adminEmail: String,
    ordersState: UiState<List<AdminOrderDto>> = UiState.Empty,
    onAdvanceOrderStatus: (orderId: Int, nextStatus: String) -> Unit = { _, _ -> },
    onRefresh: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onRefresh()
    }

    val pedidos = (ordersState as? UiState.Success<List<AdminOrderDto>>)?.data.orEmpty()
    val estadisticas = remember(pedidos) { EstadisticasAdmin.calcular(pedidos) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de Operaciones Admin", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recargar órdenes")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(FoodJetPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = FoodJetPrimaryDark, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(adminName, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(adminEmail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (ordersState is UiState.Success) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("S/ ${String.format("%.2f", estadisticas.totalVendido)}", fontWeight = FontWeight.ExtraBold, color = FoodJetPrimaryDark)
                                Text("${estadisticas.pedidosActivos} activos", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Gestión de pedidos en vivo (homólogo al panel web de operaciones)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, tint = FoodJetPrimaryDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pedidos del Sistema", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        when (ordersState) {
                            is UiState.Loading -> Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = FoodJetPrimary)
                            }
                            is UiState.Empty -> Text(
                                text = "No hay pedidos registrados en el sistema.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            is UiState.Error -> Column {
                                Text(text = ordersState.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                TextButton(onClick = onRefresh) { Text("Reintentar") }
                            }
                            is UiState.Success -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                pedidos.take(15).forEach { o ->
                                    val estado = Seguimiento.normalizarEstado(o.estado)
                                    val siguiente = SIGUIENTE_ESTADO[estado]
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Pedido #${o.id} · ${o.cliente ?: "Cliente"}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(
                                                    "${o.restaurante.orEmpty()} · S/ ${String.format("%.2f", o.total)} · ${ETIQUETA_ESTADO[estado] ?: o.estado}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (siguiente != null) {
                                                Button(
                                                    onClick = { onAdvanceOrderStatus(o.id, siguiente) },
                                                    shape = RoundedCornerShape(14.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = FoodJetPrimary, contentColor = Color.White),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier
                                                        .heightIn(min = 48.dp)
                                                        .semantics { contentDescription = "Avanzar pedido ${o.id} a ${ETIQUETA_ESTADO[siguiente]}" }
                                                ) {
                                                    Text("Avanzar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                Text(
                                                    text = ETIQUETA_ESTADO[estado] ?: "Cerrado",
                                                    fontSize = 11.sp,
                                                    color = if (estado == "entregado") FoodJetSuccess else MaterialTheme.colorScheme.error,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (ordersState is UiState.Success) {
                // Ventas de los últimos siete días
                item {
                    ChartCard(title = "Ventas de los últimos 7 días", icon = Icons.AutoMirrored.Filled.ShowChart) {
                        val maximo = estadisticas.ventasPorDia.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            estadisticas.ventasPorDia.forEach { (dia, monto) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text(if (monto > 0) "${monto.toInt()}" else "", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Box(
                                        modifier = Modifier
                                            .width(20.dp)
                                            .fillMaxHeight((monto / maximo).toFloat().coerceIn(0.02f, 0.85f))
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(if (monto > 0) FoodJetPrimaryDark else MaterialTheme.colorScheme.surfaceVariant)
                                            .semantics { contentDescription = "$dia: S/ ${String.format("%.2f", monto)}" }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(dia, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Ventas por local
                item {
                    ChartCard(title = "Ventas por Local", icon = Icons.Default.Storefront) {
                        val total = estadisticas.totalVendido.takeIf { it > 0 } ?: 1.0
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (estadisticas.ventasPorLocal.isEmpty()) {
                                Text("Todavía no hay ventas.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            estadisticas.ventasPorLocal.forEach { (localNombre, monto) ->
                                val fraccion = (monto / total).toFloat()
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(localNombre, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Text("S/ ${String.format("%.2f", monto)} · ${(fraccion * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraccion)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(FoodJetPrimary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Pedidos por estado
                item {
                    ChartCard(title = "Pedidos por Estado", icon = Icons.Default.PieChart) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            estadisticas.pedidosPorEstado.forEach { (estado, cantidad) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(ETIQUETA_ESTADO[estado] ?: estado, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = cantidad.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FoodJetPrimaryDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = FoodJetPrimaryDark, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            content()
        }
    }
}
