package com.example.foodjeetapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodjeetapp.data.common.UiState
import com.example.foodjeetapp.data.model.ProductItem
import com.example.foodjeetapp.data.model.PromotionSlide
import com.example.foodjeetapp.ui.components.FilterSection
import com.example.foodjeetapp.ui.components.FoodJetFooter
import com.example.foodjeetapp.ui.components.HeroCarousel
import com.example.foodjeetapp.ui.components.ProductCard
import com.example.foodjeetapp.ui.theme.FoodJetBorder
import com.example.foodjeetapp.ui.theme.FoodJetPrimary
import com.example.foodjeetapp.ui.theme.FoodJetPrimaryDark
import com.example.foodjeetapp.ui.theme.FoodJetTheme

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
    var searchQuery by remember { mutableStateOf("") }

    val allProducts = when (productsState) {
        is UiState.Success -> productsState.data
        else -> emptyList()
    }

    val categories = remember(allProducts) { allProducts.map { it.tipoComida }.distinct() }

    // Lógica de filtrado reactivo
    val filteredProducts = remember(allProducts, selectedCategories, minPrice, maxPrice, selectedDeliveryTime, searchQuery) {
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

            val q = searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                product.nombre.contains(q, ignoreCase = true) ||
                product.descripcion.contains(q, ignoreCase = true)

            matchesPrice && matchesCategory && matchesTime && matchesSearch
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Titular y buscador
        item {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
                Text(
                    text = "Tu antojo. Sin escalas.",
                    fontSize = 30.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lo mejor del barrio, directo a tu puerta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it.take(60) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("¿Qué se te antoja hoy?") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = FoodJetPrimary,
                        unfocusedBorderColor = FoodJetBorder
                    )
                )
            }
        }

        // 2. Banner de promociones
        if (promotions.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HeroCarousel(
                    slides = promotions,
                    onActionClick = { onNavigateToMenu() }
                )
            }
        }

        // 3. Categorías y filtros
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
                onClearCategories = { selectedCategories = emptySet() },
                onClearFilters = {
                    selectedCategories = emptySet()
                    minPrice = ""
                    maxPrice = ""
                    selectedDeliveryTime = null
                }
            )
        }

        item {
            Text(
                text = "Los favoritos del barrio",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 5. Renderizado Condicional según UiState
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
                            colors = ButtonDefaults.buttonColors(containerColor = FoodJetPrimary, contentColor = Color.White)
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
                    items(filteredProducts.chunked(2)) { fila ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            fila.forEach { product ->
                                Box(modifier = Modifier.weight(1f)) {
                                    ProductCard(
                                        product = product,
                                        isFavorite = favorites.contains(product.id),
                                        isStudent = isStudent,
                                        onFavoriteToggle = { onFavoriteToggle(product.id) },
                                        onAddToCart = { onAddToCart(product) }
                                    )
                                }
                            }
                            if (fila.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Pie de página
        item { FoodJetFooter() }
    }
}

@Preview(name = "HomeScreen - Light Mode", showBackground = true)
@Composable
private fun HomeScreenLightPreview() {
    FoodJetTheme(darkTheme = false) {
        HomeScreen(
            productsState = UiState.Success(
                listOf(
                    ProductItem(
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
                    ProductItem(
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
                    )
                )
            ),
            promotions = listOf(
                PromotionSlide(
                    id = 1,
                    title = "¡Mitad de precio!",
                    subtitle = "En todas las hamburguesas los martes y jueves.",
                    imageUrl = "",
                    buttonText = "Pedir ahora"
                )
            ),
            isStudent = true,
            favorites = setOf(1),
            onFavoriteToggle = {},
            onAddToCart = {},
            onNavigateToMenu = {},
            onRetry = {}
        )
    }
}

@Preview(name = "HomeScreen - Dark Mode", showBackground = true)
@Composable
private fun HomeScreenDarkPreview() {
    FoodJetTheme(darkTheme = true) {
        HomeScreen(
            productsState = UiState.Success(emptyList()),
            promotions = emptyList(),
            isStudent = false,
            favorites = emptySet(),
            onFavoriteToggle = {},
            onAddToCart = {},
            onNavigateToMenu = {},
            onRetry = {}
        )
    }
}
