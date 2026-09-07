package com.example.foodjeetapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.foodjeetapp.ui.FoodJetApp
import com.example.foodjeetapp.ui.theme.FoodJetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FoodJetTheme {
                FoodJetApp()
            }
        }
    }
}