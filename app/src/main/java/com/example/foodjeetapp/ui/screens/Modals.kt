package com.example.foodjeetapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.foodjeetapp.data.model.OrderRecord
import com.example.foodjeetapp.data.remote.ServidorFoodJet
import com.example.foodjeetapp.ui.components.StarRatingBar
import com.example.foodjeetapp.ui.theme.FoodJetDark
import com.example.foodjeetapp.ui.theme.FoodJetPrimary
import com.example.foodjeetapp.ui.theme.FoodJetPrimaryDark
import com.example.foodjeetapp.ui.theme.FoodJetSuccess
import android.webkit.WebView
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

@Composable
fun LoginDialog(
    servidor: ServidorFoodJet,
    onCambiarServidor: (ServidorFoodJet) -> Unit,
    onDismiss: () -> Unit,
    onLoginSuccess: (email: String, password: String) -> Unit,
    onSwitchToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isFormValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() && password.length >= 4

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(FoodJetPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = FoodJetPrimaryDark, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Iniciar Sesión", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                SelectorServidor(servidor = servidor, onCambiar = onCambiarServidor)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onLoginSuccess(email, password) },
                    enabled = isFormValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FoodJetPrimary, contentColor = Color.Black)
                ) {
                    Text("Ingresar", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onSwitchToRegister) {
                    Text("¿No tienes cuenta? Regístrate aquí", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun RegisterDialog(
    onDismiss: () -> Unit,
    onRegisterSuccess: (name: String, email: String, phone: String, password: String) -> Unit,
    onSwitchToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isNameValid = name.trim().length >= 3
    val isEmailValid = email.contains("@") && email.contains(".")
    val isPhoneValid = phone.matches(Regex("^\\d{9}$"))
    val isPassValid = password.length >= 6
    val isFormValid = isNameValid && isEmailValid && isPhoneValid && isPassValid

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(FoodJetPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = FoodJetPrimaryDark, modifier = Modifier.size(30.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Crear Cuenta", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre completo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    isError = !isEmailValid && email.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.take(9) },
                    label = { Text("Teléfono (9 dígitos)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = !isPhoneValid && phone.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña (mínimo 6 caracteres)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = { onRegisterSuccess(name, email, phone, password) },
                    enabled = isFormValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FoodJetPrimary, contentColor = Color.Black)
                ) {
                    Text("Registrarse", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onSwitchToLogin) {
                    Text("¿Ya tienes cuenta? Inicia sesión aquí", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ReviewDialog(
    order: OrderRecord,
    onDismiss: () -> Unit,
    onSubmitReview: (orderId: String, stars: Int, comment: String) -> Unit
) {
    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Calificar Pedido",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pedido #${order.id}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Tu Calificación", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                StarRatingBar(
                    rating = rating,
                    onRatingSelected = { rating = it },
                    starSize = 38
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comentario (Opcional)") },
                    placeholder = { Text("¿Qué te pareció tu pedido?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onSubmitReview(order.id, rating, comment) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FoodJetPrimary, contentColor = Color.Black)
                ) {
                    Text("Enviar Calificación", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


/**
 * Pago con billetera (Yape/Plin). Muestra el QR real del local, que el
 * backend da en `qr_pago`. Como en la web, el pago es simulado: a los cinco
 * segundos se da por confirmado y se registra el pedido.
 */
@Composable
fun QrPaymentDialog(
    restaurantName: String,
    qrUrl: String?,
    onDismiss: () -> Unit,
    onPaymentConfirmed: () -> Unit
) {
    var countdown by remember { mutableStateOf(5) }
    val confirmar by rememberUpdatedState(onPaymentConfirmed)

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        confirmar()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Pago con Billetera Digital",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Escanea el código QR de $restaurantName para pagar con Yape o Plin.",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(2.dp, FoodJetPrimary),
                    modifier = Modifier.size(200.dp)
                ) {
                    if (qrUrl != null) {
                        ImagenRemota(url = qrUrl, descripcion = "Código QR de pago de $restaurantName")
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Este local no tiene QR de pago", fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.5.dp,
                        color = FoodJetPrimaryDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Esperando confirmación de pago... (${countdown}s)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Cancelar Pago", color = Color.Red)
                }
            }
        }
    }
}

/**
 * Imagen servida por la web de FoodJet. Los QR son SVG, que Coil no decodifica
 * sin coil-svg (otra dependencia de Gradle); un WebView mínimo sí los dibuja.
 */
@Composable
private fun ImagenRemota(url: String, descripcion: String) {
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = descripcion },
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = false
                setBackgroundColor(android.graphics.Color.WHITE)
                val src = url.replace("\"", "%22")
                val html = "<html><body style=\"margin:0;display:flex;align-items:center;" +
                    "justify-content:center;height:100vh;background:#fff\">" +
                    "<img src=\"$src\" style=\"max-width:100%;max-height:100%\"></body></html>"
                loadDataWithBaseURL(url, html, "text/html", "utf-8", null)
            }
        },
        onRelease = { it.destroy() }
    )
}

/** Elige contra qué servidor trabaja la app. Se guarda con DataStore. */
@Composable
fun SelectorServidor(
    servidor: ServidorFoodJet,
    onCambiar: (ServidorFoodJet) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = { abierto = !abierto }, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Servidor: ${servidor.etiqueta}", fontSize = 12.sp, modifier = Modifier.weight(1f))
            Icon(if (abierto) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
        }
        if (abierto) {
            ServidorFoodJet.entries.forEach { opcion ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    RadioButton(selected = opcion == servidor, onClick = { onCambiar(opcion) })
                    Column(modifier = Modifier.weight(1f)) {
                        Text(opcion.etiqueta, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(opcion.descripcion, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(
                "Cambiar de servidor cierra la sesión.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
fun MiCuentaDialog(
    userName: String,
    userEmail: String,
    isStudent: Boolean,
    isAdmin: Boolean = false,
    servidor: ServidorFoodJet,
    onCambiarServidor: (ServidorFoodJet) -> Unit,
    onAbrirWeb: () -> Unit,
    onVerifyStudent: () -> Unit,
    onOpenAdminDashboard: () -> Unit = {},
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(FoodJetPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = FoodJetPrimaryDark, modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(userName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(userEmail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(16.dp))

                // Estado de Estudiante (lo decide el backend: es_estudiante)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isStudent) FoodJetSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isStudent) Icons.Default.School else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isStudent) FoodJetSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isStudent) "Estudiante Activo" else "Cuenta Regular",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isStudent) FoodJetSuccess else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isStudent) "Los productos con descuento de estudiante ya te salen rebajados"
                                else "Verifica tu condición de estudiante para ver los precios con descuento",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!isStudent) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onVerifyStudent,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FoodJetPrimary, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verificar Estudiante")
                    }
                }

                // Opción exclusiva para administradores
                if (isAdmin) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onOpenAdminDashboard,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FoodJetDark, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Panel de Operaciones Admin", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                // Intent implícito: lo abre el navegador que tenga el teléfono.
                TextButton(onClick = onAbrirWeb, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Abrir FoodJet en el navegador")
                }

                SelectorServidor(servidor = servidor, onCambiar = onCambiarServidor)

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar Sesión", color = Color.Red)
                }
            }
        }
    }
}
