package com.example.foodjeetapp.audit

import com.example.foodjeetapp.data.model.CartItem
import com.example.foodjeetapp.data.model.OrderRecord
import com.example.foodjeetapp.data.model.OrderStatus
import com.example.foodjeetapp.data.model.ProductItem
import org.junit.Assert.*
import org.junit.Test

/**
 * Suite de Pruebas Erróneas Diseñadas a Propósito (Auditoría de CI y Jules).
 * Contiene fallas controladas en 3 niveles de complejidad:
 * 1. Fácil: Aserto directo de regla de costo de envío.
 * 2. Media: Violación de la máquina de estados de cancelación.
 * 3. Difícil: Discrepancia sutil de acumulación y precisión de coma flotante en base imponible e IGV.
 */
class IntentionalFailureAuditTest {

    /**
     * NIVEL 1: FÁCIL DE DETECTAR
     * Falla obvia en regla de negocio: Afirma erróneamente que FoodJet tiene envío gratis (S/ 0.00),
     * cuando la regla inmutable del sistema define un costo fijo de envío de S/ 5.00.
     */
    @Test
    fun testFallaFacil_EnvioFijoDebeSerGratis() {
        val prod = ProductItem(1, "Hamburguesa Simple", "Carne y pan", 15.0, "Hamburguesas", "https://img/1.png")
        val order = OrderRecord(
            id = "FJ-ERR-01",
            fecha = "14/09/2026",
            estado = OrderStatus.CONFIRMADO,
            items = listOf(CartItem(prod, 1)),
            subtotal = 15.0,
            impuestos = 2.7,
            envio = 5.0,
            total = 22.7
        )

        // ERROR INTENCIONAL FÁCIL: Se afirma que el envío es 0.0 cuando es 5.0
        assertEquals("Falla Fácil: Se esperaba envío gratuito S/ 0.00 pero la app cobra S/ 5.00", 0.0, order.envio, 0.001)
    }

    /**
     * NIVEL 2: MÁS O MENOS (MEDIA DIFICULTAD)
     * Regresión lógica en máquina de estados: Afirma erróneamente que un pedido ya entregado
     * al cliente puede ser cancelado (OrderStatus.ENTREGADO.isCancelable == true).
     */
    @Test
    fun testFallaMedia_CancelacionDePedidoEntregadoPermitida() {
        // En FoodJet un pedido ENTREGADO es terminal y NO cancelable.
        // ERROR INTENCIONAL MEDIO: Se afirma que es cancelable.
        assertTrue(
            "Falla Media: Un pedido en estado ENTREGADO no debe ser cancelable por el usuario",
            OrderStatus.ENTREGADO.isCancelable
        )
    }

    /**
     * NIVEL 3: MUY DIFÍCIL DE DETECTAR (ALTA DIFICULTAD)
     * Error sutil de precisión en coma flotante (IEEE 754) y base imponible acumulada:
     * Al calcular subtotales con descuentos porcentuales impares sobre precios decimales:
     * - Prod 1: S/ 19.99 con 15% desc = 19.99 * 0.85 = 16.9915
     * - Prod 2: S/ 12.49 con 10% desc = 12.49 * 0.90 = 11.2410
     * - Prod 3: S/ 8.75 sin desc     =  8.7500
     * Total real acumulado exacto: 16.9915 + 11.2410 + 8.7500 = 36.9825.
     * Si la prueba errónea asume un redondeo prematuro truncado por ítem (16.99 + 11.24 + 8.75 = 36.98)
     * y evalúa igualdad estricta con delta cero (0.0):
     * La aserción falla por una discrepancia sutil de 0.0025 (2 milésimas y media) que pasa
     * desapercibida si no se audita la precisión contable de punto flotante de la plataforma.
     */
    @Test
    fun testFallaDificil_PrecisionFlotanteAcumuladaEnImpuestosYDescuentos() {
        val p1 = ProductItem(10, "Combo 1", "Dsc 15%", 19.99, "Combos", "https://img/1.png", descuentoEstudiante = 15.0)
        val p2 = ProductItem(20, "Combo 2", "Dsc 10%", 12.49, "Combos", "https://img/2.png", descuentoEstudiante = 10.0)
        val p3 = ProductItem(30, "Combo 3", "Sin desc", 8.75, "Combos", "https://img/3.png", descuentoEstudiante = 0.0)

        val precioEfectivo1 = p1.getEffectivePrice(isStudent = true) // 16.9915
        val precioEfectivo2 = p2.getEffectivePrice(isStudent = true) // 11.241
        val precioEfectivo3 = p3.getEffectivePrice(isStudent = true) // 8.75

        val subtotalReal = precioEfectivo1 + precioEfectivo2 + precioEfectivo3 // 36.9825

        // ERROR INTENCIONAL SUTIL: Afirma que la suma exacta es 36.98 con delta 0.0 (esperando truncamiento binario)
        assertEquals(
            "Falla Difícil: Discrepancia de coma flotante IEEE 754 entre redondeo ítem a ítem vs acumulado contable",
            36.98,
            subtotalReal,
            0.0
        )
    }
}
