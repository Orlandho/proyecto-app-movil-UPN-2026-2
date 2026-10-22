/**
 * Reglas del cobro.
 *
 * Módulo puro, sin base de datos ni Express.
 */

const METODOS_VALIDOS = ['cash', 'card', 'wallet'];

function isMetodoValido(metodo) {
    return METODOS_VALIDOS.includes(String(metodo || '').toLowerCase());
}

/**
 * El efectivo se cobra en la puerta, así que el pago nace pendiente. La
 * tarjeta y la billetera se autorizan antes de crear el pedido.
 *
 * Antes esto era un literal 'completado' para cualquier método, incluido el
 * pago en efectivo.
 */
function resolvePaymentStatus(metodo) {
    return String(metodo || '').toLowerCase() === 'cash' ? 'pendiente' : 'completado';
}

/**
 * Un pedido ya pagado entra directamente como confirmado; uno por pagar se
 * queda pendiente, que es además el estado que vigila la auto-cancelación.
 */
function resolveInitialOrderStatus(estadoPago) {
    return estadoPago === 'completado' ? 'confirmado' : 'pendiente';
}

module.exports = {
    METODOS_VALIDOS,
    isMetodoValido,
    resolvePaymentStatus,
    resolveInitialOrderStatus
};
