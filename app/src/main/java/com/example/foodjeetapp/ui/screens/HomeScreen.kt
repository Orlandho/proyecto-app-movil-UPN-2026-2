package com.example.foodjeetapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodjeetapp.data.common.UiState
import com.example.foodjeetapp.data.model.ProductItem
import com.example.foodjeetapp.data.model.PromotionSlide
import com.example.foodjeetapp.ui.components.FeatureBadgesRow
import com.example.foodjeetapp.ui.components.FilterSection
import com.example.foodjeetapp.ui.components.HeroCarousel
import com.example.foodjeetapp.ui.components.ProductCard
import com.example.foodjeetapp.ui.theme.FoodJetPrimary
import com.example.foodjeetapp.ui.theme.FoodJetPrimaryDark

@Composable
fun HomeScreen(
    productsState: UiState<List<ProductItem>>,
    promotions: List<PromotionSlide>,
    isStudent: Boolean,
    favorites: Set<Int>,
    onFavoriteToggle: (Int) -> Unit,
    onAddToCart: (ProductItem) -> Unit,
    onNavigateToMenu: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategories by remember { mutableStateOf(emptySet<String>()) }
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var selectedDeliveryTime by remember { mutableStateOf<String?>(null) }

    val allProducts = when (productsState) {
        is UiState.Success -> productsState.data
        else -> emptyList()
    }

    val categories = remember(allProducts) { allProducts.map { it.tipoComida }.distinct() }

    // Lógica de filtrado reactivo
    val filteredProducts = remember(allProducts, selectedCategories, minPrice, maxPrice, selectedDeliveryTime) {
        allProducts.filter { product ->
            val minP = minPrice.toDoubleOrNull() ?: 0.0
            val maxP = maxPrice.toDoubleOrNull() ?: Double.MAX_VALUE
            val matchesPrice = product.precio in minP..maxP

            val matchesCategory = selectedCategories.isEmpty() || selectedCategories.contains(product.tipoComida)

            val matchesTime = when (selectedDeliveryTime) {
                "30 minutos" -> product.tiempoEntrega.contains("30")
                "1 hora" -> product.tiempoEntrega.contains("30") || (product.tiempoEntrega.contains("1 hora") && !product.tiempoEntrega.contains("Más"))
                "Más de 1 hora" -> true
                else -> true
            }

            matchesPrice && matchesCategory && matchesTime
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Carrusel Hero
        item {
            HeroCarousel(
                slides = promotions,
                onActionClick = { onNavigateToMenu() }
            )
        }

        // 2. 4 Características del servicio
        item {
            FeatureBadgesRow()
        }

        // 3. Encabezado del Menú
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Nuestro Menú",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Descubre una amplia variedad de platos deliciosos preparados con los mejores ingredientes.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 4. Barra de Filtros
        item {
            FilterSection(
                categories = categories,
                selectedCategories = selectedCategories,
                onCategoryToggle = { cat ->
                    selectedCategories = if (selectedCategories.contains(cat)) {
                        selectedCategories - cat
                    } else {
                        selectedCategories + cat
                    }
                },
                minPrice = minPrice,
                maxPrice = maxPrice,
                onMinPriceChange = { minPrice = it },
                onMaxPriceChange = { maxPrice = it },
                selectedDeliveryTime = selectedDeliveryTime,
                onDeliveryTimeChange = { selectedDeliveryTime = it },
                onClearFilters = {
                    selectedCategories = emptySet()
                    minPrice = ""
                    maxPrice = ""
                    selectedDeliveryTime = null
                }
            )
        }

        // 5. Renderizado Condicional según UiState (REQ-SEM06-VIS-02)
        when (productsState) {
            is UiState.Loading -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = FoodJetPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Cargando catálogo de FoodJet...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = productsState.message,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = FoodJetPrimary, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reintentar")
                        }
                    }
                }
            }
            is UiState.Empty -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestaurantMenu,
                                contentDescription = null,
                                tint = FoodJetPrimaryDark,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Catálogo listo para conexión",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "El catálogo de productos se alimentará desde el servicio REST (Retrofit) y la base de datos local (Room) según los requerimientos técnicos del proyecto.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = onRetry,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sincronizar ahora")
                            }
                        }
                    }
                }
            }
            is UiState.Success -> {
                if (filteredProducts.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No se encontraron productos con los filtros seleccionados.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(filteredProducts) { product ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            ProductCard(
                                product = product,
                                isFavorite = favorites.contains(product.id),
                                isStudent = isStudent,
                                onFavoriteToggle = { onFavoriteToggle(product.id) },
                                onAddToCart = { onAddToCart(product) }
                            )
                        }
                    }
                }
            }
        }

        // 6. Sección "¿Por qué elegir FoodJet?" (About)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "¿Por qué elegir FoodJet?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Somos más que un servicio de delivery. En FoodJet nos apasiona conectar a nuestros clientes con los mejores restaurantes de la ciudad. Garantizamos calidad, rapidez y el mejor servicio al cliente.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("500+", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = FoodJetPrimaryDark)
                            Text("Restaurantes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("50K+", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = FoodJetPrimaryDark)
                            Text("Clientes felices", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("24/7", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = FoodJetPrimaryDark)
                            Text("Servicio activo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // 7. Sección "Contacto"
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Contáctanos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "¿Tienes alguna pregunta o sugerencia? Estamos aquí para ayudarte.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = FoodJetPrimaryDark, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Email: hola@foodjet.com", fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = FoodJetPrimaryDark, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Teléfono: 01 234 5678", fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = FoodJetPrimaryDark, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dirección: Av. Principal 123, Lima, Perú", fontSize = 13.sp)
                    }
                }
            }
        }

        // 8. Footer FoodJet
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(FoodJetPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✈️", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("FoodJet", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "© 2026 FoodJet. Todos los derechos reservados.\nEntrega de comida rápida y confiable.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
