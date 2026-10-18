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
        atender(intent)
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
        atender(intent)
    }

    /**
     * Lee el pedido del Intent y quita el extra: tras un giro de pantalla la
     * Activity se recrea con el mismo Intent y no debe volver a navegar.
     */
    private fun atender(intent: Intent?) {
        val pedido = IntentsFoodJet.pedidoDe(intent) ?: return
        intent?.removeExtra(IntentsFoodJet.EXTRA_PEDIDO_ID)
        pedidoSolicitado.value = pedido
    }
}
