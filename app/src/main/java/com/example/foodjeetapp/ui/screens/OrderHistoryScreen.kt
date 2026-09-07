package com.example.foodjeetapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodjeetapp.data.model.OrderRecord
import com.example.foodjeetapp.data.model.OrderStatus
import com.example.foodjeetapp.ui.components.StarRatingBar
import com.example.foodjeetapp.ui.theme.*

@Composable
fun OrderHistoryScreen(
    orders: List<OrderRecord>,
    onLeaveReview: (OrderRecord) -> Unit,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mi Historial de Pedidos",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }

        if (orders.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No tienes pedidos anteriores",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onBackToMenu) {
                        Text("Empezar a pedir")
                    }
                }
            }
        } else {
            items(orders) { order ->
                OrderCard(order = order, onLeaveReview = { onLeaveReview(order) })
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: OrderRecord,
    onLeaveReview: () -> Unit
) {
    val (statusColor, statusBg, statusIcon) = when (order.estado) {
        OrderStatus.PENDIENTE -> Triple(Color(0xFF856404), FoodJetWarningContainer, Icons.Default.HourglassEmpty)
        OrderStatus.EN_PREPARACION -> Triple(Color(0xFF0C5460), Color(0xFFD1ECF1), Icons.Default.SoupKitchen)
        OrderStatus.EN_CAMINO -> Triple(Color(0xFF004085), Color(0xFFCCE5FF), Icons.AutoMirrored.Filled.DirectionsBike)
        OrderStatus.ENTREGADO -> Triple(Color(0xFF155724), FoodJetSuccessContainer, Icons.Default.CheckCircle)
        OrderStatus.CANCELADO -> Triple(Color(0xFF721C24), FoodJetDangerContainer, Icons.Default.Cancel)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera: ID y Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pedido #${order.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = FoodJetPrimaryDark
                    )
                    Text(
                        text = order.fecha,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = statusBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = order.estado.label.uppercase(),
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Lista de platillos
            Text(
                text = "Artículos",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            order.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${item.quantity}x ${item.product.nombre}",
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "S/ ${String.format("%.2f", item.product.precio * item.quantity)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Resumen financiero
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("S/ ${String.format("%.2f", order.subtotal)}", fontSize = 12.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("IGV (18%)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("S/ ${String.format("%.2f", order.impuestos)}", fontSize = 12.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Envío", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("S/ ${String.format("%.2f", order.envio)}", fontSize = 12.sp)
            }
            if (order.descuento > 0.0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Descuento", fontSize = 12.sp, color = FoodJetSuccess)
                    Text("-S/ ${String.format("%.2f", order.descuento)}", fontSize = 12.sp, color = FoodJetSuccess)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("S/ ${String.format("%.2f", order.total)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = FoodJetPrimaryDark)
            }

            // Sección de Reseñas
            if (order.estado == OrderStatus.ENTREGADO) {
                Spacer(modifier = Modifier.height(10.dp))
                if (order.reviewStars == null) {
                    OutlinedButton(
                        onClick = onLeaveReview,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, FoodJetWarning)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = FoodJetWarning, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dejar una Reseña", color = MaterialTheme.colorScheme.onSurface)
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = FoodJetWarningContainer.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, FoodJetWarning.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(6.dp), color = FoodJetWarning) {
                                    Text("Tu reseña", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                StarRatingBar(rating = order.reviewStars, starSize = 16)
                            }
                            if (!order.reviewComment.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"${order.reviewComment}\"",
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
