package com.example.foodjeetapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.foodjeetapp.data.model.ProductItem
import com.example.foodjeetapp.ui.theme.FoodJetBorder
import com.example.foodjeetapp.ui.theme.FoodJetDark
import com.example.foodjeetapp.ui.theme.FoodJetPrimary
import com.example.foodjeetapp.ui.theme.FoodJetSuccessDeep
import com.example.foodjeetapp.ui.theme.FoodJetTheme
import com.example.foodjeetapp.ui.theme.FoodJetYellow

@Composable
fun ProductCard(
    product: ProductItem,
    isFavorite: Boolean,
    isStudent: Boolean,
    onFavoriteToggle: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val effectivePrice = product.getEffectivePrice(isStudent)
    val hasDiscount = isStudent && product.descuentoEstudiante > 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, FoodJetBorder)
    ) {
        Column {
            // Imagen con etiqueta y favorito flotantes
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imagenUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = product.nombre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                val etiqueta = when {
                    !product.disponibilidad -> "AGOTADO"
                    product.descuentoEstudiante > 0 -> "PRECIO ESTUDIANTE"
                    else -> null
                }
                if (etiqueta != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        shape = CircleShape,
                        color = if (product.disponibilidad) FoodJetYellow else FoodJetDark
                    ) {
                        Text(
                            text = etiqueta,
                            color = if (product.disponibilidad) FoodJetDark else Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                        tint = if (isFavorite) FoodJetPrimary else FoodJetDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = product.nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = product.descripcion,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = product.tiempoEntrega.replace(" minutos", " min"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (hasDiscount) {
                            Text(
                                text = "S/ ${String.format("%.2f", product.precio)}",
                                fontSize = 10.sp,
                                textDecoration = TextDecoration.LineThrough,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "S/ ${String.format("%.2f", effectivePrice)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (hasDiscount) FoodJetSuccessDeep else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (product.disponibilidad) FoodJetPrimary else Color.LightGray)
                            .then(if (product.disponibilidad) Modifier.clickable { onAddToCart() } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (product.disponibilidad) "Agregar ${product.nombre} al carrito" else "Agotado",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "ProductCard - Light Mode", showBackground = true)
@Composable
private fun ProductCardLightPreview() {
    FoodJetTheme(darkTheme = false) {
        ProductCard(
            product = ProductItem(
                id = 1,
                nombre = "Hamburguesa FoodJet Clásica",
                descripcion = "Carne de res premium con queso cheddar y papas fritas crocantes.",
                precio = 18.50,
                descuentoEstudiante = 3.50,
                imagenUrl = "",
                tipoComida = "Hamburguesas",
                restauranteId = 1,
                restauranteNombre = "Burger Jet",
                tiempoEntrega = "20 min",
                disponibilidad = true
            ),
            isFavorite = true,
            isStudent = true,
            onFavoriteToggle = {},
            onAddToCart = {}
        )
    }
}

@Preview(name = "ProductCard - Dark Mode", showBackground = true)
@Composable
private fun ProductCardDarkPreview() {
    FoodJetTheme(darkTheme = true) {
        ProductCard(
            product = ProductItem(
                id = 2,
                nombre = "Pizza Pepperoni Suprema",
                descripcion = "Masa artesanal con salsa de tomates frescos y pepperoni crocante.",
                precio = 24.00,
                descuentoEstudiante = 0.0,
                imagenUrl = "",
                tipoComida = "Pizzas",
                restauranteId = 2,
                restauranteNombre = "Pizza Fast",
                tiempoEntrega = "25 min",
                disponibilidad = true
            ),
            isFavorite = false,
            isStudent = false,
            onFavoriteToggle = {},
            onAddToCart = {}
        )
    }
}
