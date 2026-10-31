package com.example.foodjeetapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Esquemas de color para FoodJet.
 *
 * Consideraciones de accesibilidad y contraste WCAG 2.1:
 * - Botón Primario y Controles Gráficos: FoodJetPrimary (#F4511E) con onPrimary (Color.White)
 *   ofrece contraste 3.48:1, superando el umbral de 3.0:1 exigido por WCAG AA para componentes
 *   de interfaz interactivos y tipografía destacada/negrita (>= 14pt / 18.66px).
 * - Texto Estándar sobre Primario: Para texto normal que requiera ratio >= 4.5:1, se utiliza
 *   FoodJetOnPrimaryAccessible (FoodJetDark #1C1C1C), alcanzando 4.90:1.
 * - Contenedor Primario Adaptable:
 *   - Modo Claro: primaryContainer = FoodJetPrimaryLight (#FCF0E4), onPrimaryContainer = FoodJetDark (#1C1C1C) -> 15.19:1
 *   - Modo Oscuro: primaryContainer = Color(0xFF3A2818), onPrimaryContainer = FoodJetPrimaryLight (#FCF0E4) -> 12.51:1
 *   Ambos modos superan con holgura el nivel WCAG AAA (7.0:1).
 */
private val LightColorScheme = lightColorScheme(
    primary = FoodJetPrimary,
    onPrimary = Color.White,
    primaryContainer = FoodJetPrimaryLight,
    onPrimaryContainer = FoodJetDark,
    secondary = FoodJetDark,
    onSecondary = Color.White,
    secondaryContainer = FoodJetPrimaryLight,
    onSecondaryContainer = FoodJetDark,
    background = FoodJetLightGray,
    onBackground = FoodJetDark,
    surface = Color.White,
    onSurface = FoodJetDark,
    surfaceVariant = FoodJetLightGray,
    onSurfaceVariant = FoodJetGray,
    error = FoodJetDanger,
    onError = Color.White,
    outline = FoodJetBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = FoodJetPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A2818),
    onPrimaryContainer = FoodJetPrimaryLight,
    secondary = FoodJetPrimaryDark,
    onSecondary = Color.White,
    background = Color(0xFF141414),
    onBackground = Color(0xFFEEEEEE),
    surface = Color(0xFF202020),
    onSurface = Color(0xFFEEEEEE),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFAAAAAA),
    error = FoodJetDanger,
    onError = Color.White,
    outline = Color(0xFF404040)
)

@Composable
fun FoodJetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Preview(name = "Theme Preview - Light Mode", showBackground = true)
@Composable
fun FoodJetThemeLightPreview() {
    FoodJetTheme(darkTheme = false) {
        ThemePreviewSample()
    }
}

@Preview(name = "Theme Preview - Dark Mode", showBackground = true)
@Composable
fun FoodJetThemeDarkPreview() {
    FoodJetTheme(darkTheme = true) {
        ThemePreviewSample()
    }
}

@Composable
private fun ThemePreviewSample() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "FoodJet Design Tokens",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Superficie de Tarjeta",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Texto secundario onSurfaceVariant",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Botón Primario")
                }
            }
        }
    }
}
