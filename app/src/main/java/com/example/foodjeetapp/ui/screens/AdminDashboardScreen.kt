package com.example.foodjeetapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodjeetapp.data.common.UiState
import com.example.foodjeetapp.data.remote.dto.AdminOrderDto
import com.example.foodjeetapp.ui.theme.FoodJetPrimary
import com.example.foodjeetapp.ui.theme.FoodJetPrimaryDark
import com.example.foodjeetapp.ui.theme.FoodJetSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    ordersState: UiState<List<AdminOrderDto>> = UiState.Empty,
    onAdvanceOrderStatus: (orderId: Int, nextStatus: String) -> Unit = { _, _ -> },
    onRefresh: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onRefresh()
    }

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
            // Tarjeta de Admin
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(FoodJetPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = FoodJetPrimaryDark,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Admin FoodJet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "admin@foodjet.com",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Sección de Gestión de Pedidos en Vivo (Homólogo al panel web de operaciones)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, tint = FoodJetPrimaryDark)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pedidos del Sistema", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        when (ordersState) {
                            is UiState.Loading -> {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp), color = FoodJetPrimary)
                                }
                            }
                            is UiState.Empty -> {
                                Text(
                                    text = "No hay pedidos registrados en el sistema.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            is UiState.Error -> {
                                Text(
                                    text = "Error: ${(ordersState as UiState.Error).message}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            is UiState.Success -> {
                                val orders = (ordersState as UiState.Success<List<AdminOrderDto>>).data
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    orders.take(10).forEach { o ->
                                        val normState = o.estado.lowercase().trim()
                                        val nextState = when (normState) {
                                            "pendiente" -> "confirmado"
                                            "confirmado" -> "en_preparacion"
                                            "en_preparacion" -> "en_camino"
                                            "en_camino" -> "entregado"
                                            else -> null
                                        }

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
                                                    Text("Pedido #${o.id} - ${o.cliente ?: "Cliente"}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("Total: S/ ${String.format("%.2f", o.total)} | Estado: ${o.estado}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                if (nextState != null) {
                                                    Button(
                                                        onClick = { onAdvanceOrderStatus(o.id, nextState) },
                                                        shape = RoundedCornerShape(14.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = FoodJetPrimary, contentColor = Color.Black),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Text("Avanzar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                } else {
                                                    Text(
                                                        text = if (normState == "entregado") "Entregado" else "Cerrado",
                                                        fontSize = 11.sp,
                                                        color = FoodJetSuccess,
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
            }

            // Gráfico 1: Ventas por Día
            item {
                ChartCard(
                    title = "Ventas por Día",
                    icon = Icons.AutoMirrored.Filled.ShowChart
                ) {
                    val days = listOf("Lun" to 0.45f, "Mar" to 0.60f, "Mié" to 0.75f, "Jue" to 0.50f, "Vie" to 0.90f, "Sáb" to 1.0f, "Dom" to 0.85f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        days.forEach { (day, fraction) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .fillMaxHeight(fraction)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(FoodJetPrimaryDark)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(day, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Gráfico 2: Productos Más Vendidos
            item {
                ChartCard(
                    title = "Productos Más Vendidos",
                    icon = Icons.Default.BarChart
                ) {
                    val topProducts = listOf(
                        "Hamburguesa Clásica" to 0.88f,
                        "Pizza Pepperoni" to 0.74f,
                        "Sushi Roll California" to 0.55f,
                        "Ensalada César" to 0.40f
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        topProducts.forEach { (name, fraction) ->
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text("${(fraction * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                            .fillMaxWidth(fraction)
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

            // Gráfico 3: Ventas por Categoría
            item {
                ChartCard(
                    title = "Ventas por Categoría",
                    icon = Icons.Default.PieChart
                ) {
                    val categories = listOf(
                        "Comida rápida" to "42%",
                        "Pizzas" to "30%",
                        "Sushi" to "15%",
                        "Bebidas y Ensaladas" to "13%"
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { (cat, pct) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cat, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = pct,
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
