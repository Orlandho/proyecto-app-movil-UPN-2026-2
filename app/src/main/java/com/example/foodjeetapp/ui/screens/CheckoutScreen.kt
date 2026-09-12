package com.example.foodjeetapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodjeetapp.data.model.CartItem
import com.example.foodjeetapp.ui.theme.FoodJetPrimary
import com.example.foodjeetapp.ui.theme.FoodJetSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartItems: List<CartItem>,
    isStudent: Boolean,
    initialName: String,
    initialPhone: String,
    onBackToMenu: () -> Unit,
    onConfirmOrder: (paymentMethod: String, total: Double, cuponId: Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var address by remember { mutableStateOf("Av. Principal 123, Miraflores, Lima") }
    var reference by remember { mutableStateOf("Frente al parque central, departamento 402") }

    // Validación reactiva (REQ-SEM04-VIS-01)
    val isNameValid = name.trim().length >= 3
    val isPhoneValid = phone.trim().matches(Regex("^\\d{9}$"))
    val isAddressValid = address.trim().length >= 5
    val isFormValid = isNameValid && isPhoneValid && isAddressValid && cartItems.isNotEmpty()

    // Selección de Método de Pago: canónicos 'cash', 'card', 'wallet'
    var paymentMethod by remember { mutableStateOf("cash") } // cash, card, wallet
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvc by remember { mutableStateOf("") }

    // Cupones homologados con la base de datos FoodJet (id 1: PruebaCupon, id 2: FOODJET20)
    var couponCode by remember { mutableStateOf("PruebaCupon") }
    var couponAppliedId by remember { mutableStateOf<Int?>(null) }
    var couponDiscountPercent by remember { mutableStateOf(0.0) }
    var couponMessage by remember { mutableStateOf<String?>(null) }

    // Cálculos económicos
    val subtotal = cartItems.sumOf { it.product.getEffectivePrice(isStudent) * it.quantity }
    val discount = if (couponAppliedId != null) subtotal * couponDiscountPercent else 0.0
    val subtotalAfterDiscount = (subtotal - discount).coerceAtLeast(0.0)
    val taxes = subtotalAfterDiscount * 0.18
    val deliveryFee = 5.00
    val total = subtotalAfterDiscount + taxes + deliveryFee

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finalizar Pedido", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackToMenu) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
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
            // 1. Información de Entrega
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = FoodJetPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Información de Entrega", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        // Nombre completo
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nombre completo") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = !isNameValid && name.isNotEmpty(),
                            supportingText = {
                                if (!isNameValid && name.isNotEmpty()) {
                                    Text("Debe contener al menos 3 caracteres")
                                }
                            },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Teléfono
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it.take(9) },
                            label = { Text("Teléfono de contacto (9 dígitos)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = !isPhoneValid && phone.isNotEmpty(),
                            supportingText = {
                                if (!isPhoneValid && phone.isNotEmpty()) {
                                    Text("Debe tener exactamente 9 dígitos numéricos")
                                }
                            },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Dirección
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Dirección de entrega") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    address = "Av. Salaverry 2020, Jesús María, Lima"
                                }) {
                                    Icon(Icons.Default.MyLocation, contentDescription = "Usar mi ubicación GPS")
                                }
                            },
                            isError = !isAddressValid && address.isNotEmpty(),
                            supportingText = {
                                if (!isAddressValid && address.isNotEmpty()) {
                                    Text("Por favor ingresa una dirección válida")
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Referencia
                        OutlinedTextField(
                            value = reference,
                            onValueChange = { reference = it },
                            label = { Text("Referencia (Opcional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 2. Método de Pago
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = FoodJetPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Método de Pago", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Opción 1: Efectivo
                        PaymentOptionRow(
                            title = "Pago en Efectivo",
                            subtitle = "Paga contraentrega cuando recibas tu pedido",
                            icon = Icons.Default.AttachMoney,
                            selected = paymentMethod == "cash",
                            onClick = { paymentMethod = "cash" }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Opción 2: Tarjeta
                        PaymentOptionRow(
                            title = "Tarjeta de Crédito / Débito",
                            subtitle = "Visa, Mastercard o AMEX",
                            icon = Icons.Default.CreditCard,
                            selected = paymentMethod == "card",
                            onClick = { paymentMethod = "card" }
                        )

                        AnimatedVisibility(visible = paymentMethod == "card") {
                            Column(modifier = Modifier.padding(top = 10.dp, start = 8.dp, end = 8.dp)) {
                                OutlinedTextField(
                                    value = cardNumber,
                                    onValueChange = { cardNumber = it.take(19) },
                                    label = { Text("Número de tarjeta") },
                                    placeholder = { Text("1234 5678 9012 3456") },
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
                                        onValueChange = { cardCvc = it.take(3) },
                                        label = { Text("CVC") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Opción 3: Billetera Digital (Yape/Plin)
                        PaymentOptionRow(
                            title = "Billetera Digital (Yape / Plin)",
                            subtitle = "Paga escaneando el código QR oficial",
                            icon = Icons.Default.QrCode,
                            selected = paymentMethod == "wallet",
                            onClick = { paymentMethod = "wallet" }
                        )
                    }
                }
            }

            // 3. Resumen y Cupón
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Resumen del Pedido", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Artículos
                        cartItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.quantity}x ${item.product.nombre}",
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "S/ ${String.format("%.2f", item.product.getEffectivePrice(isStudent) * item.quantity)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Código de Cupón
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = couponCode,
                                onValueChange = { couponCode = it },
                                label = { Text("Código de cupón") },
                                placeholder = { Text("Ejem: PruebaCupon") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val code = couponCode.trim()
                                    if (code.equals("PruebaCupon", ignoreCase = true)) {
                                        couponAppliedId = 1
                                        couponDiscountPercent = 0.10
                                        couponMessage = "¡Cupón 'PruebaCupon' (10%) aplicado exitosamente!"
                                    } else if (code.equals("FOODJET20", ignoreCase = true)) {
                                        couponAppliedId = 2
                                        couponDiscountPercent = 0.20
                                        couponMessage = "¡Cupón 'FOODJET20' (20%) aplicado exitosamente!"
                                    } else {
                                        couponAppliedId = null
                                        couponDiscountPercent = 0.0
                                        couponMessage = "Código de cupón no reconocido. Disponibles: PruebaCupon, FOODJET20"
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FoodJetPrimary, contentColor = Color.Black)
                            ) {
                                Text("Aplicar")
                            }
                        }

                        if (couponMessage != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = couponMessage!!,
                                fontSize = 12.sp,
                                color = if (couponAppliedId != null) FoodJetSuccess else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Desglose
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Botón Confirmar Pedido
                        Button(
                            onClick = { onConfirmOrder(paymentMethod, total, couponAppliedId) },
                            enabled = isFormValid,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FoodJetPrimary,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Confirmar Pedido",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentOptionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, FoodJetPrimary) else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = icon, contentDescription = null, tint = FoodJetPrimary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
            color = if (isDiscount) FoodJetSuccess else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDiscount) FoodJetSuccess else MaterialTheme.colorScheme.onSurface
        )
    }
}
