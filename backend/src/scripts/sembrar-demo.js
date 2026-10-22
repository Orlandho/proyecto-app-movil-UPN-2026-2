/**
 * Datos de demostración para ver el seguimiento y el panel de operaciones.
 *
 * BORRA TODOS LOS PEDIDOS (con sus ítems, pagos, historial y reseñas) y crea
 * otros nuevos: clientes distintos, entregas repartidas por Lima y pedidos en
 * todos los estados. Los usuarios, locales y productos que ya existen no se
 * tocan.
 *
 * Los pedidos entregados y cancelados nacen con su historial completo, en horas
 * pasadas. Los que están en curso nacen a mitad de su etapa, con el reloj
 * ajustado, y el simulador los lleva hasta el final mientras se mira: así hay
 * repartidores en movimiento desde el primer momento.
 *
 * Uso:  docker compose exec backend node src/scripts/sembrar-demo.js
 */
const bcrypt = require('bcryptjs');
const prisma = require('../config/db');
const { simplificarRuta, progresoDelReparto } = require('../domain/geo');
const { leerConfigSimulacion, crearPlan, duracionRepartoSegundos, conDuracionReparto } = require('../domain/simulacion');
const { planificarReparto } = require('../services/routing');

const CLAVE = 'Cliente123!';
const IGV = 0.18;
const ENVIO = 5.0;
// Para que el movimiento se aprecie, un reparto en curso dura al menos esto.
const MIN_REPARTO_VIVO_S = 240;

const CLIENTES = [
    { nombre: 'Lucía Paredes', email: 'lucia.paredes@mail.com', telefono: '987100201', es_estudiante: true },
    { nombre: 'Marco Quispe', email: 'marco.quispe@mail.com', telefono: '987100202', es_estudiante: false },
    { nombre: 'Valeria Rojas', email: 'valeria.rojas@mail.com', telefono: '987100203', es_estudiante: true },
    { nombre: 'Diego Salazar', email: 'diego.salazar@mail.com', telefono: '987100204', es_estudiante: false },
    { nombre: 'Camila Huamán', email: 'camila.huaman@mail.com', telefono: '987100205', es_estudiante: true },
    { nombre: 'Renzo Valdivia', email: 'renzo.valdivia@mail.com', telefono: '987100206', es_estudiante: false },
    { nombre: 'Andrea Cáceres', email: 'andrea.caceres@mail.com', telefono: '987100207', es_estudiante: false },
    { nombre: 'Jorge Mendoza', email: 'jorge.mendoza@mail.com', telefono: '987100208', es_estudiante: true }
];

// Puntos aproximados dentro de cada distrito.
const ZONAS = [
    { cliente: 0, direccion: 'Av. Primavera 1250, Santiago de Surco', referencia: 'Frente al centro comercial', lat: -12.1048, lon: -76.9731 },
    { cliente: 1, direccion: 'Av. San Luis 2045, San Borja', referencia: 'Edificio azul, piso 5', lat: -12.1007, lon: -76.9998 },
    { cliente: 2, direccion: 'Av. La Molina 1840, La Molina', referencia: 'Casa de reja negra', lat: -12.0847, lon: -76.9437 },
    { cliente: 3, direccion: 'Av. Grau 515, Barranco', referencia: 'Al costado de la bodega', lat: -12.1495, lon: -77.0215 },
    { cliente: 4, direccion: 'Av. Huaylas 330, Chorrillos', referencia: 'Cerca al mercado', lat: -12.1685, lon: -77.0089 },
    { cliente: 5, direccion: 'Av. Brasil 1560, Jesús María', referencia: 'Departamento 302', lat: -12.0721, lon: -77.0517 },
    { cliente: 6, direccion: 'Av. La Marina 2355, San Miguel', referencia: 'Cerca a Plaza San Miguel', lat: -12.0770, lon: -77.0855 },
    { cliente: 7, direccion: 'Jr. de la Unión 600, Cercado de Lima', referencia: 'Altura de la plaza', lat: -12.0464, lon: -77.0306 },
    { cliente: 0, direccion: 'Av. Angamos Este 1200, Surquillo', referencia: 'Oficina 4B', lat: -12.1117, lon: -77.0152 },
    { cliente: 1, direccion: 'Av. Brasil 3150, Magdalena del Mar', referencia: 'Portón verde', lat: -12.0897, lon: -77.0721 },
    { cliente: 2, direccion: 'Av. Bolivar 780, Pueblo Libre', referencia: 'Cerca al parque', lat: -12.0716, lon: -77.0626 },
    { cliente: 3, direccion: 'Av. Arenales 1900, Lince', referencia: 'Edificio Los Álamos', lat: -12.0846, lon: -77.0342 }
];

// Precios del catálogo sembrado (backend/prisma/seed.js).
const PRECIO = { 1: 8.5, 2: 12.0, 3: 14.5, 4: 9.0, 5: 7.0, 6: 2.0 };
const PRODUCTOS_DEL_LOCAL = { 1: [1, 5, 6], 2: [2, 3], 3: [4] };

/**
 * Cada pedido: zona (índice en ZONAS), local, estado y, según el estado:
 *   hace      minutos desde que se hizo, para los ya terminados
 *   avance    0..1 del recorrido, para los que van en camino
 */
const PEDIDOS = [
    // En curso: se ven moverse y cambiar de estado.
    { zona: 0, local: 2, estado: 'en_camino', avance: 0.2, metodo: 'card' },
    { zona: 3, local: 1, estado: 'en_camino', avance: 0.5, metodo: 'wallet' },
    { zona: 5, local: 3, estado: 'en_camino', avance: 0.8, metodo: 'card' },
    { zona: 1, local: 1, estado: 'en_preparacion', metodo: 'card' },
    { zona: 6, local: 2, estado: 'confirmado', metodo: 'wallet' },
    { zona: 2, local: 3, estado: 'pendiente', metodo: 'cash' },
    // Terminados, repartidos en las últimas horas y días.
    { zona: 7, local: 1, estado: 'entregado', hace: 25, metodo: 'card' },
    { zona: 8, local: 2, estado: 'entregado', hace: 55, metodo: 'cash' },
    { zona: 4, local: 3, estado: 'entregado', hace: 95, metodo: 'card' },
    { zona: 9, local: 1, estado: 'entregado', hace: 170, metodo: 'wallet' },
    { zona: 10, local: 2, estado: 'entregado', hace: 300, metodo: 'card' },
    { zona: 11, local: 3, estado: 'entregado', hace: 1500, metodo: 'card' },
    { zona: 3, local: 1, estado: 'entregado', hace: 1700, metodo: 'cash' },
    { zona: 5, local: 2, estado: 'entregado', hace: 2900, metodo: 'wallet' },
    { zona: 0, local: 3, estado: 'entregado', hace: 4300, metodo: 'card' },
    { zona: 6, local: 1, estado: 'cancelado', hace: 80, metodo: 'cash' },
    { zona: 8, local: 3, estado: 'cancelado', hace: 2000, metodo: 'cash' }
];

const dormir = (ms) => new Promise((r) => setTimeout(r, ms));
const redondear = (n) => Math.round(n * 100) / 100;

function itemsPara(local, i) {
    const catalogo = PRODUCTOS_DEL_LOCAL[local];
    const a = catalogo[i % catalogo.length];
    const items = [{ product_id: a, cantidad: 1 + (i % 2), precio_unitario: PRECIO[a] }];
    if (catalogo.length > 1 && i % 3 !== 0) {
        const b = catalogo[(i + 1) % catalogo.length];
        items.push({ product_id: b, cantidad: 1, precio_unitario: PRECIO[b] });
    }
    return items;
}

/** Variación de ±25 % para que los tiempos de los pedidos pasados no sean todos iguales. */
const variar = (s, i) => Math.max(2, Math.round(s * (0.75 + ((i * 37) % 50) / 100)));

async function main() {
    const config = leerConfigSimulacion();
    const ahora = Date.now();

    const borrados = await prisma.order.deleteMany();
    console.log(`🗑️  ${borrados.count} pedidos borrados (con ítems, pagos, historial y reseñas).`);

    const hash = await bcrypt.hash(CLAVE, 10);
    const usuarios = [];
    for (const c of CLIENTES) {
        usuarios.push(await prisma.user.upsert({
            where: { email: c.email },
            update: { nombre: c.nombre, telefono: c.telefono, es_estudiante: c.es_estudiante },
            create: { ...c, password: hash, rol: 'cliente' }
        }));
    }
    await prisma.deliveryAddress.deleteMany({ where: { usuario_id: { in: usuarios.map((u) => u.id) } } });

    const direcciones = [];
    for (const z of ZONAS) {
        direcciones.push(await prisma.deliveryAddress.create({
            data: {
                usuario_id: usuarios[z.cliente].id,
                direccion_detallada: z.direccion,
                referencia: z.referencia,
                latitud: z.lat,
                longitud: z.lon,
                es_predeterminada: true
            }
        }));
    }
    console.log(`👥 ${usuarios.length} clientes y ${direcciones.length} direcciones en ${ZONAS.length} zonas de Lima.`);

    const locales = Object.fromEntries((await prisma.restaurant.findMany()).map((l) => [l.id, l]));

    for (const [i, p] of PEDIDOS.entries()) {
        const zona = ZONAS[p.zona];
        const local = locales[p.local];
        const items = itemsPara(p.local, i);

        const subtotal = items.reduce((t, it) => t + it.cantidad * it.precio_unitario, 0);
        const impuestos = redondear(subtotal * IGV);
        const total = redondear(subtotal + impuestos + ENVIO);

        const recorrido = await planificarReparto([local.latitud, local.longitud], [zona.lat, zona.lon], config.velocidadKmh);
        const ruta = simplificarRuta(recorrido.ruta, 300);
        const enVivo = ['pendiente', 'confirmado', 'en_preparacion', 'en_camino'].includes(p.estado);

        const base = crearPlan({ ...config, activa: true });
        let repartoS = duracionRepartoSegundos(recorrido.distancia_metros, config);
        if (enVivo) repartoS = Math.max(repartoS, MIN_REPARTO_VIVO_S);
        const plan = conDuracionReparto(base, repartoS);

        // Línea de tiempo: [estado, segundos desde que se hizo el pedido, origen].
        let inicio;
        const historial = [];
        if (enVivo) {
            const hasta = { pendiente: 0, confirmado: 1, en_preparacion: 2, en_camino: 3 }[p.estado];
            const etapas = ['pendiente', 'confirmado', 'en_preparacion', 'en_camino'];
            let t = 0;
            for (let k = 0; k <= hasta; k++) {
                historial.push([etapas[k], t, k === 0 ? 'cliente' : 'simulador']);
                t += plan[etapas[k]];
            }
            // Cuánto lleva en el estado actual: algo menos de lo que dura, para
            // que se vea el cambio enseguida pero no al instante.
            const llevaS = p.estado === 'en_camino' ? repartoS * p.avance : Math.min(plan[p.estado] * 0.4, 25);
            const entroEnActual = historial[historial.length - 1][1];
            inicio = ahora - (entroEnActual + llevaS) * 1000;
        } else {
            inicio = ahora - p.hace * 60000;
            if (p.estado === 'cancelado') {
                historial.push(['pendiente', 0, 'cliente'], ['cancelado', variar(40, i), 'cliente']);
            } else {
                let t = 0;
                const pasos = [['pendiente', 'cliente', plan.pendiente], ['confirmado', 'simulador', plan.confirmado],
                    ['en_preparacion', i % 4 === 0 ? 'personal' : 'simulador', plan.en_preparacion],
                    ['en_camino', 'simulador', repartoS], ['entregado', 'simulador', 0]];
                for (const [estado, origen, dur] of pasos) {
                    historial.push([estado, t, origen]);
                    t += variar(dur, i);
                }
            }
        }

        const salida = historial.find(([e]) => e === 'en_camino');
        const envioInicio = salida ? new Date(inicio + salida[1] * 1000) : null;

        let posicion = null;
        if (p.estado === 'en_camino') {
            posicion = progresoDelReparto(ruta, repartoS * p.avance * 1000, repartoS * 1000).posicion;
        } else if (p.estado === 'entregado') {
            posicion = ruta[ruta.length - 1];
        }

        await prisma.order.create({
            data: {
                user_id: usuarios[zona.cliente].id,
                restaurante_id: p.local,
                direccion_entrega_id: direcciones[p.zona].id,
                total, impuestos, costo_envio: ENVIO,
                estado: p.estado,
                fecha: new Date(inicio),
                envio_iniciado_en: envioInicio,
                ruta,
                distancia_metros: recorrido.distancia_metros,
                eta_minutos: recorrido.eta_minutos,
                repartidor_lat: posicion ? posicion[0] : null,
                repartidor_lon: posicion ? posicion[1] : null,
                plan_tiempos: plan,
                OrderItem: { create: items },
                Transaction: {
                    create: {
                        metodo_pago: p.metodo,
                        monto: total,
                        estado_pago: p.estado === 'cancelado' || (p.metodo === 'cash' && p.estado !== 'entregado') ? 'pendiente' : 'completado',
                        fecha_transaccion: new Date(inicio)
                    }
                },
                OrderStatusHistory: {
                    create: historial.map(([estado, seg, origen]) => ({ estado, origen, fecha: new Date(inicio + seg * 1000) }))
                }
            }
        });
        console.log(`📦 ${p.estado.padEnd(14)} ${local.nombre.padEnd(12)} → ${zona.direccion.split(',').pop().trim()} (${(recorrido.distancia_metros / 1000).toFixed(1)} km, ${recorrido.origen || 'ruta'})`);
        await dormir(1100); // cortesía con el servicio público de rutas
    }

    console.log(`\n✅ Listo. Contraseña de los clientes nuevos: ${CLAVE}`);
}

main()
    .catch((error) => {
        console.error(error);
        process.exitCode = 1;
    })
    .finally(() => prisma.$disconnect());
