package com.example.foodjeetapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodjeetapp.ui.theme.FoodJetBorder
import com.example.foodjeetapp.ui.theme.FoodJetGray
import com.example.foodjeetapp.ui.theme.FoodJetPrimary
import com.example.foodjeetapp.ui.theme.FoodJetTheme

sealed class BottomBarScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomBarScreen("home", "Inicio", Icons.Default.Home)
    object Menu : BottomBarScreen("menu", "Explorar", Icons.Default.Explore)
    object Favorites : BottomBarScreen("favorites", "Favoritos", Icons.Default.Favorite)
    object Orders : BottomBarScreen("orders", "Pedidos", Icons.Default.Receipt)
    object Profile : BottomBarScreen("profile", "Mi perfil", Icons.Default.Person)
}

@Composable
fun FoodJetBottomBar(
    currentRoute: String,
    favoritesCount: Int,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomBarScreen.Home,
        BottomBarScreen.Menu,
        BottomBarScreen.Orders,
        BottomBarScreen.Profile
    )

    Column {
        HorizontalDivider(color = FoodJetBorder)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp
        ) {
            items.forEach { screen ->
                val selected = currentRoute == screen.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(screen.route) },
                    icon = {
                        if (screen == BottomBarScreen.Favorites && favoritesCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = Color.Red,
                                        contentColor = Color.White
                                    ) {
                                        Text(favoritesCount.toString())
                                    }
                                }
                            ) {
                                Icon(imageVector = screen.icon, contentDescription = screen.title)
                            }
                        } else {
                            Icon(imageVector = screen.icon, contentDescription = screen.title)
                        }
                    },
                    label = { Text(screen.title, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FoodJetPrimary,
                        selectedTextColor = FoodJetPrimary,
                        unselectedIconColor = FoodJetGray,
                        unselectedTextColor = FoodJetGray,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}

@Preview(name = "FoodJetBottomBar - Light Mode", showBackground = true)
@Composable
private fun FoodJetBottomBarLightPreview() {
    FoodJetTheme(darkTheme = false) {
        FoodJetBottomBar(
            currentRoute = "home",
            favoritesCount = 0,
            onNavigate = {}
        )
    }
}

@Preview(name = "FoodJetBottomBar - Dark Mode", showBackground = true)
@Composable
private fun FoodJetBottomBarDarkPreview() {
    FoodJetTheme(darkTheme = true) {
        FoodJetBottomBar(
            currentRoute = "orders",
            favoritesCount = 2,
            onNavigate = {}
        )
    }
}
