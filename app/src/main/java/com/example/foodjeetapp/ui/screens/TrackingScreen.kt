package com.example.foodjeetapp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodjeetapp.data.model.OrderRecord
import com.example.foodjeetapp.data.model.OrderStatus
import com.example.foodjeetapp.ui.theme.FoodJetPrimary
import com.example.foodjeetapp.ui.theme.FoodJetPrimaryDark
import com.example.foodjeetapp.ui.theme.FoodJetSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    order: OrderRecord?,
    onBackToMenu: () -> Unit,
    onViewHistory: () -> Unit,
    onCancelOrder: (orderId: Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentStatus = order?.estado ?: OrderStatus.CONFIRMADO
    val currentStep = currentStatus.stepIndex

    // Animación de pulso para el icono de estado mientras no sea terminal
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

    val (statusTitle, statusDesc, statusIcon, iconColor) = when (currentStatus) {
        OrderStatus.PENDIENTE -> Quadruple(
            "Pedido Pendiente de Pago",
            "Tu orden ha sido registrada. Si elegiste pago en efectivo, pagarás al recibir.",
            Icons.Default.HourglassEmpty,
            FoodJetPrimary
        )
        OrderStatus.CONFIRMADO -> Quadruple(
            "¡Pedido Confirmado!",
            "El restaurante ha recibido tu pedido y se prepara para cocinar.",
            Icons.AutoMirrored.Filled.ReceiptLong,
            FoodJetPrimary
        )
        OrderStatus.EN_PREPARACION -> Quadruple(
            "Preparando tu pedido",
            "Tu comida está siendo preparada con mucho cuidado en cocina.",
            Icons.Default.SoupKitchen,
            FoodJetPrimaryDark
        )
        OrderStatus.EN_CAMINO -> Quadruple(
            "¡Pedido en camino!",
            "El repartidor va en camino hacia tu dirección de entrega. Mantente atento.",
            Icons.AutoMirrored.Filled.DirectionsBike,
            FoodJetPrimary
        )
        OrderStatus.ENTREGADO -> Quadruple(
            "¡Pedido Entregado!",
            "Tu comida ha sido entregada. ¡Buen provecho y que disfrutes!",
            Icons.Default.CheckCircle,
            FoodJetSuccess
        )
        OrderStatus.CANCELADO -> Quadruple(
            "Pedido Cancelado",
            "Este pedido ha sido cancelado.",
            Icons.Default.Cancel,
            MaterialTheme.colorScheme.error
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seguimiento de Pedido", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackToMenu) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icono pulsante
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .scale(if (!currentStatus.isTerminal) pulseScale else 1.0f)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = statusTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = statusDesc,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!currentStatus.isTerminal) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (currentStatus) {
                                        OrderStatus.PENDIENTE, OrderStatus.CONFIRMADO -> "Tiempo estimado: 35 minutos"
                                        OrderStatus.EN_PREPARACION -> "En cocina: aprox. 20 minutos"
                                        OrderStatus.EN_CAMINO -> "Llega en aprox. 10 minutos"
                                        else -> "En proceso"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Stepper de 4 pasos (Confirmado -> Preparando -> En camino -> Entregado)
                    if (currentStatus != OrderStatus.CANCELADO) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StepNode(stepNumber = 1, label = "Confirmado", isCurrent = currentStep == 1, isCompleted = currentStep > 1, modifier = Modifier.weight(1f))
                            StepLine(isCompleted = currentStep >= 2, modifier = Modifier.weight(0.7f))
                            StepNode(stepNumber = 2, label = "En cocina", isCurrent = currentStep == 2, isCompleted = currentStep > 2, modifier = Modifier.weight(1f))
                            StepLine(isCompleted = currentStep >= 3, modifier = Modifier.weight(0.7f))
                            StepNode(stepNumber = 3, label = "En camino", isCurrent = currentStep == 3, isCompleted = currentStep > 3, modifier = Modifier.weight(1f))
                            StepLine(isCompleted = currentStep >= 4, modifier = Modifier.weight(0.7f))
                            StepNode(stepNumber = 4, label = "Entregado", isCurrent = currentStep == 4, isCompleted = currentStep >= 4, modifier = Modifier.weight(1f))
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "El pedido fue cancelado",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Detalles del pedido
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Detalles del Pedido", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            DetailRow("Número de orden:", order?.id ?: "#FJ-1001")
                            DetailRow("Método de pago:", order?.paymentMethod ?: "Pago Registrado")
                            DetailRow("Fecha:", order?.fecha ?: "Hoy")
                            DetailRow("Estado actual:", currentStatus.label, isBold = true)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            DetailRow("Subtotal:", "S/ ${String.format("%.2f", order?.subtotal ?: 0.0)}")
                            DetailRow("Impuestos (18%):", "S/ ${String.format("%.2f", order?.impuestos ?: 0.0)}")
                            DetailRow("Envío:", "S/ ${String.format("%.2f", order?.envio ?: 5.00)}")
                            DetailRow("Total Final:", "S/ ${String.format("%.2f", order?.total ?: 0.0)}", isBold = true)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón Cancelar Pedido (solo si está en estado cancelable)
                    if (order != null && currentStatus.isCancelable) {
                        OutlinedButton(
                            onClick = { onCancelOrder(order.numericId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cancelar Pedido", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Caja de soporte telefónico
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = FoodJetPrimaryDark)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "¿Preguntas sobre tu pedido?\nLlámanos gratis al 01 234 5678",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Botones de navegación
                    Button(
                        onClick = onBackToMenu,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FoodJetPrimary, contentColor = Color.Black)
                    ) {
                        Text("Volver al Menú", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onViewHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Ver Historial de Pedidos")
                    }
                }
            }
        }
    }
}

@Composable
private fun StepNode(
    stepNumber: Int,
    label: String,
    isCurrent: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = when {
            isCompleted -> FoodJetSuccess
            isCurrent -> FoodJetPrimary
            else -> Color.LightGray.copy(alpha = 0.5f)
        },
        label = "stepColor"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            } else {
                Text(
                    text = stepNumber.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isCurrent) Color.Black else Color.DarkGray
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StepLine(isCompleted: Boolean, modifier: Modifier = Modifier) {
    val color by animateColorAsState(
        targetValue = if (isCompleted) FoodJetSuccess else Color.LightGray.copy(alpha = 0.5f),
        label = "lineColor"
    )
    Box(
        modifier = modifier
            .padding(bottom = 16.dp)
            .height(3.dp)
            .background(color)
    )
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
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = if (isBold) 13.sp else 12.sp,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isBold) FoodJetPrimaryDark else MaterialTheme.colorScheme.onSurface
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
