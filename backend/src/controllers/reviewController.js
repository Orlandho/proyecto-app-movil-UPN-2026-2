const prisma = require('../config/db');

exports.createReview = async (req, res) => {
    try {
        const userId = req.user.userId;
        const { pedido_id, puntuacion, comentario } = req.body;

        if (!pedido_id || !puntuacion) {
            return res.status(400).json({ error: 'Faltan datos requeridos (pedido_id, puntuacion).' });
        }

        if (puntuacion < 1 || puntuacion > 5) {
            return res.status(400).json({ error: 'La puntuación debe estar entre 1 y 5.' });
        }

        // Verificar que el pedido pertenezca al usuario y esté en estado apto para reseñar (por ejemplo, entregado)
        const order = await prisma.order.findUnique({
            where: { id: parseInt(pedido_id) },
            include: { Review: true }
        });

        if (!order) {
            return res.status(404).json({ error: 'Pedido no encontrado.' });
        }

        if (order.user_id !== userId) {
            return res.status(403).json({ error: 'No tienes permiso para calificar este pedido.' });
        }

        if (order.estado !== 'Entregado' && order.estado !== 'entregado') {
            return res.status(400).json({ error: 'Solo se pueden calificar pedidos que han sido entregados.' });
        }

        // Verificar si ya existe una reseña para este pedido
        if (order.Review) {
             return res.status(400).json({ error: 'Ya existe una calificación para este pedido.' });
        }

        const result = await prisma.$transaction(async (tx) => {
            // Crear la reseña
            const newReview = await tx.review.create({
                data: {
                    usuario_id: userId,
                    restaurante_id: order.restaurante_id,
                    pedido_id: parseInt(pedido_id),
                    puntuacion: parseInt(puntuacion),
                    comentario: comentario || null
                }
            });

            // Recalcular la calificación promedio del restaurante
            const aggregate = await tx.review.aggregate({
                where: { restaurante_id: order.restaurante_id },
                _avg: { puntuacion: true }
            });

            const newAverage = aggregate._avg.puntuacion || 0;

            // Actualizar el restaurante con el nuevo promedio
            await tx.restaurant.update({
                where: { id: order.restaurante_id },
                data: { calificacion_promedio: newAverage }
            });

            return newReview;
        });

        res.status(201).json({ message: 'Calificación enviada exitosamente.', review: result });
    } catch (error) {
        console.error(error);
        if (error.code === 'P2002') {
             return res.status(400).json({ error: 'Ya has calificado este pedido.' });
        }
        res.status(500).json({ error: 'Error al crear la calificación.' });
    }
};

exports.getMyReviews = async (req, res) => {
    try {
        const userId = req.user.userId;

        const reviews = await prisma.review.findMany({
            where: { usuario_id: userId },
            include: {
                Restaurant: {
                    select: { nombre: true }
                },
                Order: {
                    select: { fecha: true, total: true }
                }
            },
            orderBy: {
                fecha_publicacion: 'desc'
            }
        });

        res.json(reviews);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Error al obtener tus calificaciones.' });
    }
};
