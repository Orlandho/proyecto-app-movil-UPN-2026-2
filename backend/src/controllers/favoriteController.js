const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

// Obtener los productos favoritos del usuario
exports.getFavorites = async (req, res) => {
    try {
        const userId = req.user.userId;
        const user = await prisma.user.findUnique({
            where: { id: userId },
            include: {
                Product: {
                    include: {
                        Restaurant: {
                            select: { nombre: true, id: true }
                        }
                    }
                }
            }
        });

        if (!user) {
            return res.status(404).json({ error: 'Usuario no encontrado' });
        }

        res.json(user.Product);
    } catch (error) {
        console.error('Error al obtener favoritos:', error);
        res.status(500).json({ error: 'Error al obtener favoritos' });
    }
};

// Alternar (agregar/quitar) un producto favorito
exports.toggleFavorite = async (req, res) => {
    try {
        const userId = req.user.userId;
        const productId = parseInt(req.params.productId);

        if (isNaN(productId)) {
            return res.status(400).json({ error: 'ID de producto inválido' });
        }

        // Verificar que el producto exista
        const product = await prisma.product.findUnique({ where: { id: productId } });
        if (!product) {
            return res.status(404).json({ error: 'Producto no encontrado' });
        }

        // Verificar si ya es favorito
        const userWithFavorites = await prisma.user.findUnique({
            where: { id: userId },
            include: { Product: { where: { id: productId } } }
        });

        const isFavorite = userWithFavorites.Product && userWithFavorites.Product.length > 0;

        if (isFavorite) {
            // Remover
            await prisma.user.update({
                where: { id: userId },
                data: {
                    Product: {
                        disconnect: { id: productId }
                    }
                }
            });
            res.json({ message: 'Producto eliminado de favoritos', isFavorite: false });
        } else {
            // Agregar
            await prisma.user.update({
                where: { id: userId },
                data: {
                    Product: {
                        connect: { id: productId }
                    }
                }
            });
            res.json({ message: 'Producto agregado a favoritos', isFavorite: true });
        }

    } catch (error) {
        console.error('Error al alternar favorito:', error);
        res.status(500).json({ error: 'Error al actualizar favorito' });
    }
};
