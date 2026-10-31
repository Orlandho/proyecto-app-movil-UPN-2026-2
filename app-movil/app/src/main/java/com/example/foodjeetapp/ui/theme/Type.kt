package com.example.foodjeetapp.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
)

@Preview(name = "Typography Preview - Light Mode", showBackground = true)
@Composable
fun TypographyPreview() {
    FoodJetTheme(darkTheme = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("displayLarge 30sp Bold", style = Typography.displayLarge)
            Text("headlineMedium 22sp Bold", style = Typography.headlineMedium)
            Text("titleLarge 18sp Bold", style = Typography.titleLarge)
            Text("titleMedium 16sp SemiBold", style = Typography.titleMedium)
            Text("bodyLarge 15sp Normal", style = Typography.bodyLarge)
            Text("bodyMedium 13sp Normal", style = Typography.bodyMedium)
            Text("labelLarge 14sp Bold", style = Typography.labelLarge)
            Text("labelSmall 11sp Medium", style = Typography.labelSmall)
        }
    }
}

@Preview(name = "Typography Preview - Dark Mode", showBackground = true)
@Composable
fun TypographyDarkPreview() {
    FoodJetTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("displayLarge 30sp Bold", style = Typography.displayLarge)
            Text("headlineMedium 22sp Bold", style = Typography.headlineMedium)
            Text("titleLarge 18sp Bold", style = Typography.titleLarge)
            Text("titleMedium 16sp SemiBold", style = Typography.titleMedium)
            Text("bodyLarge 15sp Normal", style = Typography.bodyLarge)
            Text("bodyMedium 13sp Normal", style = Typography.bodyMedium)
            Text("labelLarge 14sp Bold", style = Typography.labelLarge)
            Text("labelSmall 11sp Medium", style = Typography.labelSmall)
        }
    }
}
