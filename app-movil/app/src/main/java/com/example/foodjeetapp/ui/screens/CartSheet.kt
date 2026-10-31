package com.example.foodjeetapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.foodjeetapp.data.model.CartItem
import com.example.foodjeetapp.data.model.ProductItem
import com.example.foodjeetapp.ui.theme.FoodJetBorder
import com.example.foodjeetapp.ui.theme.FoodJetPrimary
import com.example.foodjeetapp.ui.theme.FoodJetSuccessDeep
import com.example.foodjeetapp.ui.theme.FoodJetSuccessSoft
import com.example.foodjeetapp.ui.theme.FoodJetTheme
import com.example.foodjeetapp.ui.theme.FoodJetWarning

@Composable
fun CartSheet(
    cartItems: List<CartItem>,
    isStudent: Boolean,
    onIncreaseQty: (Int) -> Unit,
    onDecreaseQty: (Int) -> Unit,
    onProceedToCheckout: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val total = cartItems.sumOf { it.product.getEffectivePrice(isStudent) * it.quantity }
    val totalCount = cartItems.sumOf { it.quantity }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Cabecera
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "Tu carrito",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$totalCount ${if (totalCount == 1) "producto" else "productos"} · " +
                        "${cartItems.size} ${if (cartItems.size == 1) "antojo" else "antojos"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar carrito", modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (cartItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "🛒", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tu carrito está vacío",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "¡Agrega antojos deliciosos desde el menú!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onClose,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FoodJetPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Explorar Menú", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Local y tiempo de entrega
            val primero = cartItems.first().product
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, FoodJetBorder), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        primero.restauranteNombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text("Preparado al momento", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FoodJetSuccessSoft)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.DirectionsBike,
                    contentDescription = null,
                    tint = FoodJetSuccessDeep,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "En tu puerta en ${primero.tiempoEntrega.replace(" minutos", " min")}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FoodJetSuccessDeep
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(cartItems, key = { it.product.id }) { item ->
                    val effectivePrice = item.product.getEffectivePrice(isStudent)
                    val hasDiscount = isStudent && item.product.descuentoEstudiante > 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.product.nombre,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (hasDiscount) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = FoodJetWarning.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Descuento Estudiante",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB78103),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            // Control de cantidad
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .border(BorderStroke(1.dp, FoodJetBorder), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 2.dp)
                            ) {
                                IconButton(
                                    onClick = { onDecreaseQty(item.product.id) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Disminuir", modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = item.quantity.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.widthIn(min = 18.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = { onIncreaseQty(item.product.id) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Aumentar", modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Text(
                            text = "S/ ${String.format("%.2f", effectivePrice * item.quantity)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            TextButton(onClick = onClose, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, tint = FoodJetPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Agregar más antojos", color = FoodJetPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            HorizontalDivider(color = FoodJetBorder)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subtotal estimado",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "S/ ${String.format("%.2f", total)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "El envío, el IGV y los cupones se calculan al pagar.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onProceedToCheckout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FoodJetPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Ir a pagar · S/ ${String.format("%.2f", total)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Pago seguro. Antojos garantizados.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(name = "CartSheet - Light Mode", showBackground = true)
@Composable
private fun CartSheetLightPreview() {
    FoodJetTheme(darkTheme = false) {
        CartSheet(
            cartItems = listOf(
                CartItem(
                    product = ProductItem(
                        id = 1,
                        nombre = "Hamburguesa FoodJet Clásica",
                        descripcion = "Carne de res premium con queso cheddar.",
                        precio = 18.50,
                        descuentoEstudiante = 3.50,
                        imagenUrl = "",
                        tipoComida = "Hamburguesas",
                        restauranteId = 1,
                        restauranteNombre = "Burger Jet",
                        tiempoEntrega = "20 min",
                        disponibilidad = true
                    ),
                    quantity = 2
                )
            ),
            isStudent = true,
            onIncreaseQty = {},
            onDecreaseQty = {},
            onProceedToCheckout = {},
            onClose = {}
        )
    }
}

@Preview(name = "CartSheet - Dark Mode", showBackground = true)
@Composable
private fun CartSheetDarkPreview() {
    FoodJetTheme(darkTheme = true) {
        CartSheet(
            cartItems = emptyList(),
            isStudent = false,
            onIncreaseQty = {},
            onDecreaseQty = {},
            onProceedToCheckout = {},
            onClose = {}
        )
    }
}
