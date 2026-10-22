/**
 * Simulación del ciclo de vida de un pedido.
 *
 * En un sistema real el pedido lo harían avanzar la cocina y el repartidor
 * desde sus aplicaciones. Aquí cada pedido nace con un plan: cuántos segundos
 * dura cada estado. Un temporizador (jobs/simuladorPedidos.js) lo hace avanzar
 * cuando vence, y el seguimiento usa el mismo plan para estimar a qué hora
 * llegará cada etapa.
 *
 * Los tiempos se configuran en minutos "reales" y se dividen por una escala:
 * con SIM_ESCALA_TIEMPO=10 una preparación de 12 minutos dura 72 segundos, y
 * con 1 dura los 12 minutos de verdad.
 *
 * Módulo puro: sin red ni base de datos, así que se importa tal cual desde las
 * pruebas.
 */
const { normalizeEstado } = require('./orderStatus');
const { progresoDelReparto } = require('./geo');

/** Lo que tarda el local en pasar un pedido aceptado a cocina. */
const MINUTOS_CONFIRMADO = 1;

function positivo(valor, porDefecto) {
    const n = Number(valor);
    return Number.isFinite(n) && n > 0 ? n : porDefecto;
}

/** Lee la configuración de la simulación de las variables de entorno. */
function leerConfigSimulacion(env = process.env) {
    return {
        activa: env.SIMULAR_PEDIDOS === 'true',
        escala: positivo(env.SIM_ESCALA_TIEMPO, 10),
        confirmacionMin: positivo(env.SIM_CONFIRMACION_MIN, 2),
        preparacionMin: positivo(env.SIM_PREPARACION_MIN, 12),
        // Un reparto más largo se comprime a este máximo: el repartidor va más
        // rápido, pero la demostración no se eterniza si el cliente marcó su
        // casa a 300 km del local.
        repartoMaxMin: positivo(env.SIM_REPARTO_MAX_MIN, 40),
        velocidadKmh: positivo(env.REPARTIDOR_VELOCIDAD_KMH, 25)
    };
}

/** Minutos reales a segundos de simulación. Nunca menos de un segundo. */
function segundosSimulados(minutosReales, escala) {
    return Math.max(1, Math.round((minutosReales * 60) / escala));
}

/**
 * Plan de un pedido nuevo. La duración del reparto queda en null hasta que se
 * calcula la ruta, porque depende de la distancia.
 * @returns {Object|null} null si la simulación está apagada.
 */
function crearPlan(config) {
    if (!config.activa) return null;

    return {
        pendiente: segundosSimulados(config.confirmacionMin, config.escala),
        confirmado: segundosSimulados(MINUTOS_CONFIRMADO, config.escala),
        // La preparación configurada incluye el minuto de "confirmado".
        en_preparacion: segundosSimulados(Math.max(config.preparacionMin - MINUTOS_CONFIRMADO, 1), config.escala),
        en_camino: null
    };
}

/** Segundos de simulación que dura el trayecto según su longitud. */
function duracionRepartoSegundos(distanciaMetros, config) {
    const metrosPorMinuto = (config.velocidadKmh * 1000) / 60;
    const minutos = Math.min(Math.max((Number(distanciaMetros) || 0) / metrosPorMinuto, 1), config.repartoMaxMin);
    return segundosSimulados(minutos, config.escala);
}

/** Añade la duración del reparto a un plan, exista o no. */
function conDuracionReparto(plan, segundos) {
    return { ...(plan && typeof plan === 'object' ? plan : {}), en_camino: segundos };
}

/**
 * ¿Ya pasó el tiempo previsto para el estado actual?
 * Un estado sin duración en el plan nunca vence: espera a que alguien lo mueva.
 */
function estadoVencido(estado, inicio, plan, ahoraMs = Date.now()) {
    const segundos = plan?.[normalizeEstado(estado)];
    if (!Number.isFinite(segundos) || segundos <= 0 || !inicio) return false;

    return ahoraMs - new Date(inicio).getTime() >= segundos * 1000;
}

/**
 * Dónde va el repartidor de un pedido y cuánto le falta.
 * @returns {Object|null} null si el pedido no ha salido o no tiene ruta.
 */
function progresoDelPedido(pedido, ahoraMs = Date.now()) {
    const ruta = pedido?.ruta;
    const duracionS = pedido?.plan_tiempos?.en_camino;
    if (!Array.isArray(ruta) || ruta.length < 2 || !pedido.envio_iniciado_en || !duracionS) return null;

    const inicio = new Date(pedido.envio_iniciado_en).getTime();
    const duracionMs = duracionS * 1000;
    // Entregado es entregado aunque el reloj diga otra cosa (lo movió el
    // personal antes de tiempo).
    const transcurrido = normalizeEstado(pedido.estado) === 'entregado' ? duracionMs : ahoraMs - inicio;
    const progreso = progresoDelReparto(ruta, transcurrido, duracionMs);

    return {
        posicion: progreso.posicion,
        avance: progreso.avance,
        restante_metros: Math.round(progreso.restanteMetros),
        llegada_estimada: new Date(inicio + duracionMs).toISOString()
    };
}

module.exports = {
    MINUTOS_CONFIRMADO,
    leerConfigSimulacion,
    segundosSimulados,
    crearPlan,
    duracionRepartoSegundos,
    conDuracionReparto,
    estadoVencido,
    progresoDelPedido
};
