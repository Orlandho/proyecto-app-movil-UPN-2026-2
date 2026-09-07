package com.example.foodjeetapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodjeetapp.ui.theme.FoodJetPrimary
import com.example.foodjeetapp.ui.theme.FoodJetPrimaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodJetTopBar(
    cartCount: Int,
    favoritesCount: Int,
    isLoggedIn: Boolean,
    userName: String?,
    onLogoClick: () -> Unit,
    onCartClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onLogoClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(FoodJetPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🚀",
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FoodJet",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            // Botón de Favoritos con Badge
            BadgedBox(
                badge = {
                    if (favoritesCount > 0) {
                        Badge(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        ) {
                            Text(favoritesCount.toString())
                        }
                    }
                }
            ) {
                IconButton(onClick = onFavoritesClick) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Ver favoritos",
                        tint = if (favoritesCount > 0) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Botón de Carrito con Badge
            BadgedBox(
                badge = {
                    if (cartCount > 0) {
                        Badge(
                            containerColor = FoodJetPrimaryDark,
                            contentColor = Color.White
                        ) {
                            Text(cartCount.toString())
                        }
                    }
                }
            ) {
                IconButton(onClick = onCartClick) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Ver carrito de compras",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Botón de Perfil / Login
            IconButton(onClick = onProfileClick) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = if (isLoggedIn) "Perfil de $userName" else "Iniciar sesión",
                    tint = if (isLoggedIn) FoodJetPrimaryDark else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
