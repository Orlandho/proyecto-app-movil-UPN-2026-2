package com.example.foodjeetapp.data.model

import java.time.YearMonth

/**
 * Validación del formulario de tarjeta del checkout (REQ-SEM04-LOG-03).
 *
 * El cobro con tarjeta es simulado y estos datos nunca salen del teléfono: el
 * backend solo recibe `metodo_pago = "card"`. Aun así el formulario no debe
 * aceptar una tarjeta vacía o imposible, porque el pedido entra como pagado.
 */
object ValidacionTarjeta {

    /** 13 a 19 dígitos y dígito verificador de Luhn correcto. */
    fun numeroValido(numero: String): Boolean {
        val digitos = numero.filter { !it.isWhitespace() }
        if (digitos.length !in 13..19 || !digitos.all(Char::isDigit)) return false
        val suma = digitos.reversed().mapIndexed { i, c ->
            val d = c - '0'
            if (i % 2 == 1) (d * 2).let { if (it > 9) it - 9 else it } else d
        }.sum()
        return suma % 10 == 0
    }

    /** "MM/AA" con mes 01-12 y no vencida respecto a [hoy]. */
    fun vencimientoValido(vencimiento: String, hoy: YearMonth = YearMonth.now()): Boolean {
        val partes = Regex("^(\\d{2})/(\\d{2})$").find(vencimiento.trim())?.groupValues ?: return false
        val mes = partes[1].toInt()
        if (mes !in 1..12) return false
        val fecha = YearMonth.of(2000 + partes[2].toInt(), mes)
        return !fecha.isBefore(hoy)
    }

    fun cvcValido(cvc: String): Boolean = cvc.length in 3..4 && cvc.all(Char::isDigit)

    fun tarjetaValida(numero: String, vencimiento: String, cvc: String, hoy: YearMonth = YearMonth.now()): Boolean =
        numeroValido(numero) && vencimientoValido(vencimiento, hoy) && cvcValido(cvc)

    /** Da formato "MM/AA" mientras se escribe: solo dígitos y la barra después del mes. */
    fun formatearVencimiento(entrada: String): String {
        val digitos = entrada.filter(Char::isDigit).take(4)
        return if (digitos.length > 2) digitos.substring(0, 2) + "/" + digitos.substring(2) else digitos
    }
}
