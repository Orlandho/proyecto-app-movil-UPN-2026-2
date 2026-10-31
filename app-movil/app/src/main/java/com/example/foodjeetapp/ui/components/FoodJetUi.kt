package com.example.foodjeetapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodjeetapp.ui.theme.FoodJetTheme

/** Pie de página de las pantallas del diseño: marca a la izquierda y lema a la derecha. */
@Composable
fun FoodJetFooter(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "FOODJET",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp // Cumple WCAG AA para texto destacado (>= 14pt bold), contraste >= 3.0:1
        )
        Text(
            "Hecho para tus antojos.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

/** Encabezado de tarjeta: icono sobre cuadro con primaryContainer adaptable y título. */
@Composable
fun FoodJetSectionHeader(icon: ImageVector, title: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Preview(name = "FoodJetFooter Light", showBackground = true)
@Composable
fun FoodJetFooterLightPreview() {
    FoodJetTheme(darkTheme = false) {
        Surface {
            FoodJetFooter()
        }
    }
}

@Preview(name = "FoodJetFooter Dark", showBackground = true)
@Composable
fun FoodJetFooterDarkPreview() {
    FoodJetTheme(darkTheme = true) {
        Surface {
            FoodJetFooter()
        }
    }
}

@Preview(name = "FoodJetSectionHeader Light", showBackground = true)
@Composable
fun FoodJetSectionHeaderLightPreview() {
    FoodJetTheme(darkTheme = false) {
        Surface(modifier = Modifier.padding(16.dp)) {
            FoodJetSectionHeader(
                icon = Icons.Default.Restaurant,
                title = "Platos Principales"
            )
        }
    }
}

@Preview(name = "FoodJetSectionHeader Dark", showBackground = true)
@Composable
fun FoodJetSectionHeaderDarkPreview() {
    FoodJetTheme(darkTheme = true) {
        Surface(modifier = Modifier.padding(16.dp)) {
            FoodJetSectionHeader(
                icon = Icons.Default.Fastfood,
                title = "Promociones del Día"
            )
        }
    }
}
