/**
 * Simulador del avance de los pedidos.
 *
 * Se activa con SIMULAR_PEDIDOS=true. Cada pocos segundos mira los pedidos en
 * curso que tienen plan de tiempos y avanza los que ya cumplieron el tiempo de
 * su estado. Mientras un pedido va en camino, además, guarda la posición del
 * repartidor.
 *
 * Los pedidos sin plan (los anteriores a la simulación) no se tocan: solo los
 * mueve el personal desde el panel.
 */
const prisma = require('../config/db');
const { nextEstado } = require('../domain/orderStatus');
const { estadoVencido, progresoDelPedido } = require('../domain/simulacion');
const { cambiarEstado } = require('../services/estadoPedido');

const EN_CURSO = ['pendiente', 'confirmado', 'en_preparacion', 'en_camino'];

let enMarcha = false;

/** Un paso del simulador. Devuelve cuántos pedidos avanzó. */
async function simularPaso(ahora = Date.now()) {
    // Un paso que tarda (la ruta se pide a un servicio externo) no debe
    // solaparse con el siguiente, o un pedido podría avanzar dos veces.
    if (enMarcha) return 0;
    enMarcha = true;

    let avanzados = 0;
    try {
        const pedidos = await prisma.order.findMany({
            where: { estado: { in: EN_CURSO } },
            select: {
                id: true,
                estado: true,
                plan_tiempos: true,
                ruta: true,
                envio_iniciado_en: true,
                // Cuándo entró en su estado actual.
                OrderStatusHistory: { orderBy: { fecha: 'desc' }, take: 1, select: { estado: true, fecha: true } }
            }
        });

        for (const pedido of pedidos) {
            if (!pedido.plan_tiempos) continue;

            const ultimo = pedido.OrderStatusHistory[0];
            const inicio = ultimo?.estado === pedido.estado ? ultimo.fecha : null;

            if (estadoVencido(pedido.estado, inicio, pedido.plan_tiempos, ahora)) {
                const siguiente = nextEstado(pedido.estado);
                if (siguiente && await cambiarEstado(pedido.id, siguiente, 'simulador', { desde: pedido.estado })) {
                    avanzados += 1;
                    console.log(`⏩ Pedido #${pedido.id}: ${pedido.estado} → ${siguiente}`);
                }
                continue;
            }

            if (pedido.estado === 'en_camino') {
                const progreso = progresoDelPedido(pedido, ahora);
                if (progreso) {
                    const [lat, lon] = progreso.posicion;
                    await prisma.order.update({
                        where: { id: pedido.id },
                        data: { repartidor_lat: lat, repartidor_lon: lon }
                    });
                }
            }
        }
    } catch (error) {
        console.error('Error en el simulador de pedidos:', error);
    } finally {
        enMarcha = false;
    }
    return avanzados;
}

function startSimuladorPedidos() {
    const intervalo = Number(process.env.SIM_TICK_MS) || 2000;
    console.log(`🛵 Simulación de pedidos activa (revisa cada ${intervalo / 1000}s, escala ${process.env.SIM_ESCALA_TIEMPO || 10}x)`);
    return setInterval(simularPaso, intervalo);
}

module.exports = { simularPaso, startSimuladorPedidos };
