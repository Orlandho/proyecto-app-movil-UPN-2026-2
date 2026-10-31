package com.example.foodjeetapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.foodjeetapp.data.model.PromotionSlide
import com.example.foodjeetapp.ui.theme.FoodJetDark
import com.example.foodjeetapp.ui.theme.FoodJetPrimary
import com.example.foodjeetapp.ui.theme.FoodJetTheme
import com.example.foodjeetapp.ui.theme.FoodJetYellow

@Composable
fun HeroCarousel(
    slides: List<PromotionSlide>,
    onActionClick: (PromotionSlide) -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String = "POCO PRESUPUESTO. MUCHO SABOR."
) {
    val pagerState = rememberPagerState(pageCount = { slides.size })

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(230.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(FoodJetPrimary)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val slide = slides[page]
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(slide.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = slide.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(width = 150.dp, height = 170.dp)
                        .clip(RoundedCornerShape(topStart = 32.dp))
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(0.62f)
                        .padding(start = 20.dp, top = 16.dp, bottom = 28.dp)
                ) {
                    Surface(shape = CircleShape, color = FoodJetYellow) {
                        Text(
                            text = badgeText,
                            color = FoodJetDark,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = slide.title,
                        color = Color.White,
                        fontSize = 28.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = slide.subtitle,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onActionClick(slide) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = FoodJetDark
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(slide.buttonText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (slides.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(slides.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .height(6.dp)
                            .width(if (isSelected) 18.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

@Preview(name = "HeroCarousel - Light Mode", showBackground = true)
@Composable
private fun HeroCarouselLightPreview() {
    FoodJetTheme(darkTheme = false) {
        HeroCarousel(
            slides = listOf(
                PromotionSlide(
                    id = 1,
                    title = "¡Mitad de precio!",
                    subtitle = "En todas las hamburguesas clásicas los martes y jueves.",
                    imageUrl = "",
                    buttonText = "Pedir ahora"
                )
            ),
            onActionClick = {}
        )
    }
}

@Preview(name = "HeroCarousel - Dark Mode", showBackground = true)
@Composable
private fun HeroCarouselDarkPreview() {
    FoodJetTheme(darkTheme = true) {
        HeroCarousel(
            slides = listOf(
                PromotionSlide(
                    id = 1,
                    title = "Combo Universitario",
                    subtitle = "Tu almuerzo completo con descuento especial UPN.",
                    imageUrl = "",
                    buttonText = "Aprovechar"
                )
            ),
            onActionClick = {}
        )
    }
}
