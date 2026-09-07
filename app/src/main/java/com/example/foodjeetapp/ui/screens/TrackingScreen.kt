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
import com.example.foodjeetapp.ui.theme.FoodJetPrimary
import com.example.foodjeetapp.ui.theme.FoodJetPrimaryDark
import com.example.foodjeetapp.ui.theme.FoodJetSuccess
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    orderNumber: String = "#FJ1003",
    paymentMethod: String = "Efectivo",
    subtotal: Double = 25.50,
    taxes: Double = 4.59,
    deliveryFee: Double = 5.00,
    total: Double = 35.09,
    onBackToMenu: () -> Unit,
    onViewHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Simulación del progreso en 3 pasos (1 = Preparando, 2 = En camino, 3 = Entregado)
    var currentStep by remember { mutableStateOf(1) }

    LaunchedEffect(Unit) {
        delay(6000)
        currentStep = 2 // Pasa a "En camino"
        delay(8000)
        currentStep = 3 // Pasa a "Entregado"
    }

    // Animación de pulso para el icono de estado
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

    val (statusTitle, statusDesc, statusIcon, iconColor) = when (currentStep) {
        1 -> Quadruple(
            "Preparando tu pedido",
            "Tu pedido está siendo preparado con mucho cuidado en el restaurante.",
            Icons.Default.SoupKitchen,
            FoodJetPrimaryDark
        )
        2 -> Quadruple(
            "¡Pedido en camino!",
            "El repartidor va en camino hacia tu dirección. Mantente atento.",
            Icons.AutoMirrored.Filled.DirectionsBike,
            FoodJetPrimary
        )
        else -> Quadruple(
            "¡Pedido Entregado!",
            "Tu comida ha sido entregada. ¡Buen provecho y que disfrutes!",
            Icons.Default.CheckCircle,
            FoodJetSuccess
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
                            .scale(if (currentStep < 3) pulseScale else 1.0f)
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

                    if (currentStep < 3) {
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
                                    text = if (currentStep == 1) "Tiempo estimado: 30 minutos" else "Llega en aprox. 10 minutos",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Stepper de 3 pasos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StepNode(stepNumber = 1, label = "Preparando", isCurrent = currentStep == 1, isCompleted = currentStep > 1, modifier = Modifier.weight(1f))
                        StepLine(isCompleted = currentStep >= 2, modifier = Modifier.weight(0.8f))
                        StepNode(stepNumber = 2, label = "En camino", isCurrent = currentStep == 2, isCompleted = currentStep > 2, modifier = Modifier.weight(1f))
                        StepLine(isCompleted = currentStep >= 3, modifier = Modifier.weight(0.8f))
                        StepNode(stepNumber = 3, label = "Entregado", isCurrent = currentStep == 3, isCompleted = currentStep >= 3, modifier = Modifier.weight(1f))
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
                            DetailRow("Número de orden:", orderNumber)
                            DetailRow("Método de pago:", paymentMethod)
                            DetailRow("Fecha:", "6 de Septiembre, 2026")
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            DetailRow("Subtotal:", "S/ ${String.format("%.2f", subtotal)}")
                            DetailRow("Impuestos:", "S/ ${String.format("%.2f", taxes)}")
                            DetailRow("Envío:", "S/ ${String.format("%.2f", deliveryFee)}")
                            DetailRow("Total Final:", "S/ ${String.format("%.2f", total)}", isBold = true)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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

                    // Botones de acción
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
                .size(34.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            } else {
                Text(
                    text = stepNumber.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isCurrent) Color.Black else Color.DarkGray
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
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
            fontSize = if (isBold) 14.sp else 12.sp,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isBold) FoodJetPrimaryDark else MaterialTheme.colorScheme.onSurface
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
