package com.example.foodjeetapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodjeetapp.ui.theme.FoodJetPrimary
import com.example.foodjeetapp.ui.theme.FoodJetTheme

@Composable
fun FoodJetTopBar(
    cartCount: Int,
    favoritesCount: Int,
    isLoggedIn: Boolean,
    userName: String?,
    onLogoClick: () -> Unit,
    onCartClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    address: String = "Ingresa tu dirección"
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLogoClick() }
                ) {
                    Text(
                        text = "FOODJET",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = FoodJetPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = FoodJetPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = onFavoritesClick) {
                    BadgedBox(
                        badge = {
                            if (favoritesCount > 0) {
                                Badge(containerColor = FoodJetPrimary, contentColor = Color.White) {
                                    Text(favoritesCount.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "Ver favoritos",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(FoodJetPrimary)
                        .clickable { onCartClick() },
                    contentAlignment = Alignment.Center
                ) {
                    BadgedBox(
                        badge = {
                            if (cartCount > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.onSurface, contentColor = Color.White) {
                                    Text(cartCount.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "Ver carrito de compras",
                            tint = Color.White
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clickable { onProfileClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = FoodJetPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = address,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(name = "FoodJetTopBar - Light Mode", showBackground = true)
@Composable
private fun FoodJetTopBarLightPreview() {
    FoodJetTheme(darkTheme = false) {
        FoodJetTopBar(
            cartCount = 2,
            favoritesCount = 1,
            isLoggedIn = true,
            userName = "Orlando",
            onLogoClick = {},
            onCartClick = {},
            onFavoritesClick = {},
            onProfileClick = {},
            address = "Ingresa tu dirección"
        )
    }
}

@Preview(name = "FoodJetTopBar - Dark Mode", showBackground = true)
@Composable
private fun FoodJetTopBarDarkPreview() {
    FoodJetTheme(darkTheme = true) {
        FoodJetTopBar(
            cartCount = 2,
            favoritesCount = 1,
            isLoggedIn = true,
            userName = "Orlando",
            onLogoClick = {},
            onCartClick = {},
            onFavoritesClick = {},
            onProfileClick = {},
            address = "Av. Javier Prado Este 450"
        )
    }
}
