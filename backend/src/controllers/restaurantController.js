const prisma = require('../config/db');
const { validarCoordenadas } = require('../domain/coordenadas');

/**
 * Locales y su ubicación en el mapa.
 *
 * Las coordenadas son lo que permite calcular la ruta del reparto, así que un
 * local sin ubicar no puede recibir pedidos con seguimiento en el mapa.
 */

const ESTADOS_AFILIACION = ['activo', 'pendiente', 'suspendido'];

/** Valida el cuerpo de creación o edición. @returns {{valid, error?}} */
function validarLocal({ nombre, latitud, longitud, estado_afiliacion }, { exigirNombre = true } = {}) {
    if (exigirNombre && (!nombre || !String(nombre).trim())) {
        return { valid: false, error: 'El nombre del local es obligatorio.' };
    }

    const coordenadas = validarCoordenadas(latitud, longitud);
    if (!coordenadas.valid) return coordenadas;

    if (estado_afiliacion && !ESTADOS_AFILIACION.includes(estado_afiliacion)) {
        return { valid: false, error: `Estado de afiliación no válido. Debe ser uno de: ${ESTADOS_AFILIACION.join(', ')}.` };
    }

    return { valid: true };
}

/** Público: alimenta el mapa de locales del catálogo. */
exports.getAllRestaurants = async (req, res) => {
    try {
        const locales = await prisma.restaurant.findMany({
            orderBy: { nombre: 'asc' },
            include: { _count: { select: { Product: true } } }
        });

        res.json(locales.map(({ _count, ...local }) => ({ ...local, productos: _count.Product })));
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Error al obtener los locales.' });
    }
};

function parseId(req, res) {
    const id = parseInt(req.params.id, 10);
    if (Number.isNaN(id)) {
        res.status(400).json({ error: 'Identificador inválido.' });
        return null;
    }
    return id;
}

exports.getRestaurantById = async (req, res) => {
    try {
        const id = parseId(req, res);
        if (id === null) return;

        const local = await prisma.restaurant.findUnique({
            where: { id },
            include: { Product: { orderBy: { nombre: 'asc' } } }
        });

        if (!local) return res.status(404).json({ error: 'Local no encontrado.' });
        res.json(local);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Error al obtener el local.' });
    }
};

exports.createRestaurant = async (req, res) => {
    try {
        const revision = validarLocal(req.body);
        if (!revision.valid) return res.status(400).json({ error: revision.error });

        const { nombre, descripcion, direccion, latitud, longitud, tiempo_entrega, estado_afiliacion, qr_pago } = req.body;

        const local = await prisma.restaurant.create({
            data: {
                nombre: String(nombre).trim(),
                descripcion: descripcion || null,
                direccion: direccion || null,
                latitud: latitud === undefined || latitud === null || latitud === '' ? null : Number(latitud),
                longitud: longitud === undefined || longitud === null || longitud === '' ? null : Number(longitud),
                tiempo_entrega: tiempo_entrega || '30 minutos',
                estado_afiliacion: estado_afiliacion || 'activo',
                qr_pago: qr_pago || null
            }
        });

        res.status(201).json(local);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Error al crear el local.' });
    }
};

exports.updateRestaurant = async (req, res) => {
    try {
        const id = parseId(req, res);
        if (id === null) return;

        const revision = validarLocal(req.body, { exigirNombre: false });
        if (!revision.valid) return res.status(400).json({ error: revision.error });

        const actual = await prisma.restaurant.findUnique({ where: { id } });
        if (!actual) return res.status(404).json({ error: 'Local no encontrado.' });

        const { nombre, descripcion, direccion, latitud, longitud, tiempo_entrega, estado_afiliacion, qr_pago } = req.body;
        const numeroOActual = (v, previo) => (v === undefined || v === null || v === '' ? previo : Number(v));

        const local = await prisma.restaurant.update({
            where: { id },
            data: {
                nombre: nombre !== undefined ? String(nombre).trim() : actual.nombre,
                descripcion: descripcion !== undefined ? descripcion : actual.descripcion,
                direccion: direccion !== undefined ? direccion : actual.direccion,
                latitud: numeroOActual(latitud, actual.latitud),
                longitud: numeroOActual(longitud, actual.longitud),
                tiempo_entrega: tiempo_entrega !== undefined ? tiempo_entrega : actual.tiempo_entrega,
                estado_afiliacion: estado_afiliacion !== undefined ? estado_afiliacion : actual.estado_afiliacion,
                qr_pago: qr_pago !== undefined ? qr_pago : actual.qr_pago
            }
        });

        res.json(local);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Error al actualizar el local.' });
    }
};

exports.deleteRestaurant = async (req, res) => {
    try {
        const id = parseId(req, res);
        if (id === null) return;

        const pedidos = await prisma.order.count({ where: { restaurante_id: id } });
        if (pedidos > 0) {
            return res.status(409).json({
                error: `No se puede eliminar: el local tiene ${pedidos} pedido(s). Suspéndelo en su lugar.`
            });
        }

        // Los productos se borran en cascada según el esquema.
        await prisma.restaurant.delete({ where: { id } });
        res.json({ message: 'Local eliminado.' });
    } catch (error) {
        if (error.code === 'P2025') return res.status(404).json({ error: 'Local no encontrado.' });
        console.error(error);
        res.status(500).json({ error: 'Error al eliminar el local.' });
    }
};

exports.validarLocal = validarLocal;
exports.ESTADOS_AFILIACION = ESTADOS_AFILIACION;
