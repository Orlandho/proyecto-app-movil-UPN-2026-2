const prisma = require('../config/db');
const { validarCoordenadas, numeroONulo } = require('../domain/coordenadas');

/**
 * Direcciones de entrega del usuario autenticado.
 *
 * Todas las operaciones comprueban la propiedad: una dirección solo la puede
 * leer, modificar o borrar quien la creó.
 */

exports.getMyAddresses = async (req, res) => {
    try {
        const direcciones = await prisma.deliveryAddress.findMany({
            where: { usuario_id: req.user.userId },
            orderBy: [{ es_predeterminada: 'desc' }, { id: 'asc' }]
        });

        res.json(direcciones);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Error al obtener las direcciones.' });
    }
};

exports.createAddress = async (req, res) => {
    try {
        const { direccion_detallada, referencia, latitud, longitud, es_predeterminada } = req.body;

        if (!direccion_detallada || !String(direccion_detallada).trim()) {
            return res.status(400).json({ error: 'La dirección es obligatoria.' });
        }

        // Son el destino de la ruta del reparto: una coordenada inválida
        // dibujaría al repartidor yendo a cualquier parte.
        const coordenadas = validarCoordenadas(latitud, longitud);
        if (!coordenadas.valid) {
            return res.status(400).json({ error: coordenadas.error });
        }

        const esPredeterminada = Boolean(es_predeterminada);

        // Solo puede haber una dirección predeterminada por usuario.
        if (esPredeterminada) {
            await prisma.deliveryAddress.updateMany({
                where: { usuario_id: req.user.userId },
                data: { es_predeterminada: false }
            });
        }

        const direccion = await prisma.deliveryAddress.create({
            data: {
                usuario_id: req.user.userId,
                direccion_detallada: String(direccion_detallada).trim(),
                referencia: referencia ? String(referencia).trim() : null,
                latitud: numeroONulo(latitud),
                longitud: numeroONulo(longitud),
                es_predeterminada: esPredeterminada
            }
        });

        res.status(201).json(direccion);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Error al guardar la dirección.' });
    }
};

/** Devuelve la dirección si existe y es del usuario; si no, responde y devuelve null. */
async function resolveOwnAddress(req, res) {
    const id = parseInt(req.params.id, 10);

    if (Number.isNaN(id)) {
        res.status(400).json({ error: 'Identificador de dirección inválido.' });
        return null;
    }

    const direccion = await prisma.deliveryAddress.findUnique({ where: { id } });

    if (!direccion) {
        res.status(404).json({ error: 'Dirección no encontrada.' });
        return null;
    }

    if (direccion.usuario_id !== req.user.userId) {
        res.status(403).json({ error: 'No tienes permiso sobre esta dirección.' });
        return null;
    }

    return direccion;
}

exports.updateAddress = async (req, res) => {
    try {
        const direccion = await resolveOwnAddress(req, res);
        if (!direccion) return;

        const { direccion_detallada, referencia, latitud, longitud, es_predeterminada } = req.body;

        const coordenadas = validarCoordenadas(latitud, longitud);
        if (!coordenadas.valid) {
            return res.status(400).json({ error: coordenadas.error });
        }

        if (es_predeterminada) {
            await prisma.deliveryAddress.updateMany({
                where: { usuario_id: req.user.userId },
                data: { es_predeterminada: false }
            });
        }

        const actualizada = await prisma.deliveryAddress.update({
            where: { id: direccion.id },
            data: {
                direccion_detallada: direccion_detallada ?? direccion.direccion_detallada,
                referencia: referencia ?? direccion.referencia,
                // Si no llegan se conservan; Number(null) daría 0 y llevaría
                // la dirección al golfo de Guinea.
                latitud: latitud !== undefined ? numeroONulo(latitud) : direccion.latitud,
                longitud: longitud !== undefined ? numeroONulo(longitud) : direccion.longitud,
                es_predeterminada: es_predeterminada ?? direccion.es_predeterminada
            }
        });

        res.json(actualizada);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Error al actualizar la dirección.' });
    }
};

exports.deleteAddress = async (req, res) => {
    try {
        const direccion = await resolveOwnAddress(req, res);
        if (!direccion) return;

        await prisma.deliveryAddress.delete({ where: { id: direccion.id } });
        res.json({ message: 'Dirección eliminada.' });
    } catch (error) {
        // Prisma devuelve P2003 cuando la dirección sigue referenciada por un pedido.
        if (error.code === 'P2003') {
            return res.status(409).json({ error: 'No se puede eliminar una dirección usada en un pedido.' });
        }
        console.error(error);
        res.status(500).json({ error: 'Error al eliminar la dirección.' });
    }
};
