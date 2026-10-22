package com.example.foodjeetapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.foodjeetapp.ui.theme.FoodJetWarning

@Composable
fun StarRatingBar(
    rating: Int,
    onRatingSelected: ((Int) -> Unit)? = null,
    maxStars: Int = 5,
    starSize: Int = 32,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        for (i in 1..maxStars) {
            val isFilled = i <= rating
            Icon(
                imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "$i estrellas",
                tint = FoodJetWarning,
                modifier = Modifier
                    .size(starSize.dp)
                    .padding(horizontal = 2.dp)
                    .then(
                        if (onRatingSelected != null) {
                            Modifier.clickable { onRatingSelected(i) }
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}
