package com.example.foodjeetapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.example.foodjeetapp.data.di.ServiceLocator
import com.example.foodjeetapp.ui.FoodJetApp
import com.example.foodjeetapp.ui.IntentsFoodJet
import com.example.foodjeetapp.ui.theme.FoodJetTheme

class MainActivity : ComponentActivity() {

    /** Pedido que pidió abrir un Intent explícito (acceso directo "Seguir pedido"). */
    private val pedidoSolicitado = mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.initialize(this)
        // Tras un giro de pantalla el NavHost ya restauró la ruta: no se repite.
        if (savedInstanceState == null) pedidoSolicitado.value = IntentsFoodJet.pedidoDe(intent)
        enableEdgeToEdge()
        setContent {
            FoodJetTheme {
                FoodJetApp(
                    pedidoSolicitado = pedidoSolicitado.value,
                    onPedidoAtendido = { pedidoSolicitado.value = null }
                )
            }
        }
    }

    // launchMode="singleTop": con la app abierta el Intent llega aquí.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        IntentsFoodJet.pedidoDe(intent)?.let { pedidoSolicitado.value = it }
    }
}
