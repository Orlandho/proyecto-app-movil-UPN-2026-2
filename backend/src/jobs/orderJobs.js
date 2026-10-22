/**
 * Tareas periódicas sobre los pedidos.
 *
 * Se activan con ENABLE_JOBS=true. En un sistema real el avance del pedido lo
 * dispararían la cocina y el repartidor desde sus propias aplicaciones; aquí
 * lo simula un temporizador, que es lo que permite grabar la demostración del
 * seguimiento sin necesitar a nadie al otro lado.
 */
const prisma = require('../config/db');
const { autoCancelUnconfirmedOrders } = require('../controllers/orderController');
const { normalizeEstado, nextEstado, TERMINALES } = require('../domain/orderStatus');
const { cambiarEstado } = require('../services/estadoPedido');
const { startSimuladorPedidos } = require('./simuladorPedidos');

const temporizadores = [];

/**
 * Avanza un paso todos los pedidos que aún no han terminado.
 * Con el intervalo por defecto (15 s) un pedido nuevo llega a "entregado" en
 * torno a un minuto.
 *
 * Es el simulador antiguo, a ciegas. Los pedidos con plan de tiempos los lleva
 * jobs/simuladorPedidos.js (SIMULAR_PEDIDOS=true) y aquí se saltan.
 */
async function avanzarPedidos() {
    try {
        const pedidos = await prisma.order.findMany({ select: { id: true, estado: true, plan_tiempos: true } });

        for (const pedido of pedidos) {
            if (pedido.plan_tiempos) continue;

            const actual = normalizeEstado(pedido.estado);
            if (TERMINALES.includes(actual)) continue;

            const siguiente = nextEstado(actual);
            if (!siguiente) continue;

            await cambiarEstado(pedido.id, siguiente, 'simulador', { desde: pedido.estado });
            console.log(`⏩ Pedido #${pedido.id}: ${actual} → ${siguiente}`);
        }
    } catch (error) {
        console.error('Error en el simulador de pedidos:', error);
    }
}

function startOrderSimulator() {
    const intervalo = Number(process.env.ORDER_SIMULATION_STEP_MS) || 15000;
    console.log(`🔄 Simulador de pedidos activo (un estado cada ${intervalo / 1000}s)`);
    temporizadores.push(setInterval(avanzarPedidos, intervalo));
}

function startAutoCancelJob() {
    const minutos = Number(process.env.AUTO_CANCEL_MINUTES) || 30;
    console.log(`🧹 Auto-cancelación activa (pedidos sin confirmar tras ${minutos} min)`);
    temporizadores.push(setInterval(() => autoCancelUnconfirmedOrders(minutos), 60000));
}

function startJobs() {
    if (process.env.SIMULAR_PEDIDOS === 'true') {
        temporizadores.push(startSimuladorPedidos());
    }

    if (process.env.ENABLE_JOBS !== 'true') return;
    startOrderSimulator();
    startAutoCancelJob();
}

function stopJobs() {
    while (temporizadores.length) clearInterval(temporizadores.pop());
}

module.exports = { startJobs, stopJobs, avanzarPedidos };
