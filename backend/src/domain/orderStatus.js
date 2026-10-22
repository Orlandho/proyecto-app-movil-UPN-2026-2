/**
 * Estados de un pedido y las reglas que gobiernan su avance.
 *
 * Módulo puro: no toca la base de datos ni Express, así que se puede importar
 * directamente desde las pruebas. Tiene un espejo en el frontend
 * (frontend/js/domain/orderStatus.js) que debe mantenerse sincronizado.
 */

const ESTADOS = ['pendiente', 'confirmado', 'en_preparacion', 'en_camino', 'entregado', 'cancelado'];

/** Secuencia natural por la que avanza un pedido que no se cancela. */
const FLUJO = ['pendiente', 'confirmado', 'en_preparacion', 'en_camino', 'entregado'];

/** Estados desde los que ya no se puede avanzar. */
const TERMINALES = ['entregado', 'cancelado'];

/** Un pedido solo se puede cancelar antes de que entre a cocina. */
const CANCELABLES = ['pendiente', 'confirmado'];

/**
 * Lleva un estado a su forma canónica.
 *
 * La base de datos arrastra valores heredados con mayúsculas, tildes y
 * espacios ('En preparación', 'Entregado', el default 'Pendiente' del
 * esquema). Sin normalizar, esos pedidos no encajan en ninguna comparación y
 * la interfaz los muestra sin estado.
 */
function normalizeEstado(valor) {
    if (valor === null || valor === undefined) return '';

    return String(valor)
        .normalize('NFD')
        .replace(/[̀-ͯ]/g, '')
        .trim()
        .toLowerCase()
        .replace(/\s+/g, '_');
}

function isEstadoValido(estado) {
    return ESTADOS.includes(normalizeEstado(estado));
}

function isTerminal(estado) {
    return TERMINALES.includes(normalizeEstado(estado));
}

function isCancelable(estado) {
    return CANCELABLES.includes(normalizeEstado(estado));
}

/** Siguiente estado del flujo, o null si el pedido ya terminó. */
function nextEstado(estado) {
    const actual = normalizeEstado(estado);
    const indice = FLUJO.indexOf(actual);

    if (indice === -1 || indice === FLUJO.length - 1) return null;
    return FLUJO[indice + 1];
}

/**
 * Solo se admite avanzar un paso, o cancelar desde un estado cancelable.
 * Retroceder ('entregado' -> 'en_preparacion') o saltarse etapas se rechaza.
 */
function isValidTransition(actual, siguiente) {
    const desde = normalizeEstado(actual);
    const hacia = normalizeEstado(siguiente);

    if (!isEstadoValido(desde) || !isEstadoValido(hacia)) return false;
    if (isTerminal(desde)) return false;
    if (hacia === 'cancelado') return isCancelable(desde);

    return nextEstado(desde) === hacia;
}

module.exports = {
    ESTADOS,
    FLUJO,
    TERMINALES,
    CANCELABLES,
    normalizeEstado,
    isEstadoValido,
    isTerminal,
    isCancelable,
    nextEstado,
    isValidTransition
};
