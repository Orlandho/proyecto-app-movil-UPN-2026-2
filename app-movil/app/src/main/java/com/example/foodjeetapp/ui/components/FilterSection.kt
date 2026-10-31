package com.example.foodjeetapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodjeetapp.ui.theme.FoodJetBorder
import com.example.foodjeetapp.ui.theme.FoodJetTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSection(
    categories: List<String>,
    selectedCategories: Set<String>,
    minPrice: String,
    maxPrice: String,
    selectedDeliveryTime: String?,
    onCategoryToggle: (String) -> Unit,
    onMinPriceChange: (String) -> Unit,
    onMaxPriceChange: (String) -> Unit,
    onDeliveryTimeChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
    onClearCategories: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        // Fila de categorías (Todos + categorías del catálogo)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PillChip(label = "Todos", selected = selectedCategories.isEmpty(), onClick = onClearCategories)
            categories.forEach { category ->
                PillChip(
                    label = category,
                    selected = selectedCategories.contains(category),
                    onClick = { onCategoryToggle(category) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filtros rápidos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PillChip(label = "Filtros", selected = isExpanded, icon = Icons.Default.Tune, onClick = { isExpanded = !isExpanded })
            PillChip(
                label = "Menos de S/ 20",
                selected = maxPrice == "20",
                onClick = { onMaxPriceChange(if (maxPrice == "20") "" else "20") }
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = FoodJetBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Rango de Precios
                    Text(
                        text = "Rango de precio (S/)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = minPrice,
                            onValueChange = onMinPriceChange,
                            label = { Text("Mínimo") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = maxPrice,
                            onValueChange = onMaxPriceChange,
                            label = { Text("Máximo") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tiempo de Entrega
                    Text(
                        text = "Tiempo de entrega",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val deliveryOptions = listOf("30 minutos", "1 hora", "Más de 1 hora")
                    Column {
                        deliveryOptions.forEach { option ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                RadioButton(
                                    selected = selectedDeliveryTime == option,
                                    onClick = {
                                        if (selectedDeliveryTime == option) {
                                            onDeliveryTimeChange(null)
                                        } else {
                                            onDeliveryTimeChange(option)
                                        }
                                    }
                                )
                                Text(
                                    text = option,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(start = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botón para limpiar filtros
                    OutlinedButton(
                        onClick = onClearFilters,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Limpiar filtros")
                    }
                }
            }
        }
    }
}

/** Píldora del diseño adaptada a tema claro y oscuro con colores semánticos. */
@Composable
private fun PillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    val shape = RoundedCornerShape(50)
    val bgColor = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
    val borderColor = if (selected) Color.Transparent else FoodJetBorder

    Row(
        modifier = Modifier
            .clip(shape)
            .background(bgColor)
            .then(if (selected) Modifier else Modifier.border(BorderStroke(1.dp, borderColor), shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}

@Preview(name = "FilterSection - Light Mode", showBackground = true)
@Composable
private fun FilterSectionLightPreview() {
    FoodJetTheme(darkTheme = false) {
        FilterSection(
            categories = listOf("Hamburguesas", "Pizzas", "Bebidas", "Postres"),
            selectedCategories = setOf("Hamburguesas"),
            minPrice = "",
            maxPrice = "",
            selectedDeliveryTime = null,
            onCategoryToggle = {},
            onMinPriceChange = {},
            onMaxPriceChange = {},
            onDeliveryTimeChange = {},
            onClearFilters = {}
        )
    }
}

@Preview(name = "FilterSection - Dark Mode", showBackground = true)
@Composable
private fun FilterSectionDarkPreview() {
    FoodJetTheme(darkTheme = true) {
        FilterSection(
            categories = listOf("Hamburguesas", "Pizzas", "Bebidas", "Postres"),
            selectedCategories = emptySet(),
            minPrice = "10",
            maxPrice = "30",
            selectedDeliveryTime = "30 minutos",
            onCategoryToggle = {},
            onMinPriceChange = {},
            onMaxPriceChange = {},
            onDeliveryTimeChange = {},
            onClearFilters = {}
        )
    }
}
