package com.example.foodjeetapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.foodjeetapp.ui.theme.FoodJetPrimaryDark

sealed class BottomBarScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomBarScreen("home", "Inicio", Icons.Default.Home)
    object Menu : BottomBarScreen("menu", "Menú", Icons.Default.Fastfood)
    object Favorites : BottomBarScreen("favorites", "Favoritos", Icons.Default.Favorite)
    object Orders : BottomBarScreen("orders", "Pedidos", Icons.Default.History)
    object Profile : BottomBarScreen("profile", "Cuenta", Icons.Default.Person)
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
        BottomBarScreen.Favorites,
        BottomBarScreen.Orders,
        BottomBarScreen.Profile
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
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
                label = { Text(screen.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = FoodJetPrimaryDark,
                    selectedTextColor = FoodJetPrimaryDark,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
