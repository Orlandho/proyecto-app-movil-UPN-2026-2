package com.example.foodjeetapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.example.foodjeetapp.data.di.ServiceLocator
import com.example.foodjeetapp.ui.FoodJetApp
import com.example.foodjeetapp.ui.IntentsFoodJet
import com.example.foodjeetapp.ui.theme.FoodJetTheme

class MainActivity : ComponentActivity() {

    /** Pedido que pidió abrir un Intent explícito (acceso directo "Seguir pedido"). */
    private val pedidoSolicitado = mutableStateOf<Int?>(null)
    private var intentInicialAtendido: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.initialize(this)
        intentInicialAtendido = savedInstanceState?.getBoolean(KEY_INTENT_INICIAL_ATENDIDO, false) ?: false
        if (!intentInicialAtendido) {
            atender(intent)
            intentInicialAtendido = true
        }
        enableEdgeToEdge()
        setContent {
            FoodJetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FoodJetApp(
                        pedidoSolicitado = pedidoSolicitado.value,
                        onPedidoAtendido = { pedidoSolicitado.value = null }
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_INTENT_INICIAL_ATENDIDO, intentInicialAtendido)
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

    companion object {
        private const val KEY_INTENT_INICIAL_ATENDIDO = "foodjet_intent_inicial_atendido"
    }
}
