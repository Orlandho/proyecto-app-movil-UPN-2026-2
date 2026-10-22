/**
 * Único punto por el que cambia el estado de un pedido.
 *
 * Antes cada sitio (el personal, la cancelación del cliente, la
 * auto-cancelación) hacía su propio UPDATE y no quedaba constancia de cuándo
 * ni quién. Ahora todo cambio deja una fila en OrderStatusHistory, que es de
 * donde salen las horas de la línea de tiempo del seguimiento.
 */
const prisma = require('../config/db');
const { normalizeEstado } = require('../domain/orderStatus');
const { simplificarRuta } = require('../domain/geo');
const { leerConfigSimulacion, duracionRepartoSegundos, conDuracionReparto } = require('../domain/simulacion');
const { planificarReparto } = require('./routing');

const MAX_PUNTOS_RUTA = 300;

/** Las dos puntas del reparto como [lat, lon], o null si falta alguna. */
function puntasDelReparto(pedido) {
    const local = pedido?.Restaurant;
    const destino = pedido?.DeliveryAddress;
    const ubicado = (p) => p && p.latitud !== null && p.longitud !== null;

    if (!ubicado(local) || !ubicado(destino)) return null;
    return { origen: [local.latitud, local.longitud], destino: [destino.latitud, destino.longitud] };
}

/**
 * Calcula la ruta del local a la dirección de entrega y la guarda en el
 * pedido, junto con la duración del trayecto en la simulación.
 *
 * Se lanza al crear el pedido, para que el mapa la muestre desde el primer
 * momento, y otra vez al salir a reparto por si entonces aún no estaba. Si el
 * pedido ya tiene ruta, o le falta alguna de las dos ubicaciones, no hace nada.
 */
async function prepararRuta(pedidoId) {
    const pedido = await prisma.order.findUnique({
        where: { id: pedidoId },
        include: { Restaurant: true, DeliveryAddress: true }
    });
    if (!pedido || pedido.ruta) return pedido;

    const puntas = puntasDelReparto(pedido);
    if (!puntas) return pedido;

    const config = leerConfigSimulacion();
    const plan = await planificarReparto(puntas.origen, puntas.destino, config.velocidadKmh);

    return prisma.order.update({
        where: { id: pedidoId },
        data: {
            ruta: simplificarRuta(plan.ruta, MAX_PUNTOS_RUTA),
            distancia_metros: plan.distancia_metros,
            eta_minutos: plan.eta_minutos,
            plan_tiempos: conDuracionReparto(pedido.plan_tiempos, duracionRepartoSegundos(plan.distancia_metros, config))
        }
    });
}

/**
 * Cambia el estado de un pedido y lo anota en el historial, en una sola
 * transacción.
 *
 * @param {number} pedidoId
 * @param {string} nuevoEstado
 * @param {string} origen Quién lo provoca: cliente, personal, simulador o sistema.
 * @param {Object} [opciones]
 * @param {string} [opciones.desde] Estado (tal como está guardado) en el que
 *        se espera encontrar el pedido. Si otro proceso lo movió entretanto,
 *        no se toca y se devuelve null: así el simulador y el personal no
 *        pueden avanzar dos veces el mismo paso.
 * @returns {Promise<Object|null>} El pedido actualizado, o null.
 */
async function cambiarEstado(pedidoId, nuevoEstado, origen, { desde } = {}) {
    const estado = normalizeEstado(nuevoEstado);
    const ahora = new Date();
    const data = { estado };

    if (estado === 'en_camino') {
        // La ruta se pide fuera de la transacción: es una llamada de red.
        const conRuta = await prepararRuta(pedidoId);
        data.envio_iniciado_en = ahora;
        if (Array.isArray(conRuta?.ruta) && conRuta.ruta.length) {
            [data.repartidor_lat, data.repartidor_lon] = conRuta.ruta[0];
        }
    }

    return prisma.$transaction(async (tx) => {
        const where = desde === undefined ? { id: pedidoId } : { id: pedidoId, estado: desde };
        const { count } = await tx.order.updateMany({ where, data });
        if (count === 0) return null;

        await tx.orderStatusHistory.create({
            data: { pedido_id: pedidoId, estado, origen, fecha: ahora }
        });

        const pedido = await tx.order.findUnique({ where: { id: pedidoId } });

        // Al entregar, el repartidor queda en la puerta del cliente.
        if (estado === 'entregado' && Array.isArray(pedido.ruta) && pedido.ruta.length) {
            const [lat, lon] = pedido.ruta[pedido.ruta.length - 1];
            return tx.order.update({ where: { id: pedidoId }, data: { repartidor_lat: lat, repartidor_lon: lon } });
        }
        return pedido;
    });
}

module.exports = { cambiarEstado, prepararRuta, puntasDelReparto };
