const prisma = require('../config/db');

exports.getAllProducts = async (req, res) => {
    try {
        const products = await prisma.product.findMany({
            include: {
                Restaurant: {
                    select: {
                        nombre: true,
                        estado_afiliacion: true,
                        qr_pago: true,
                        tiempo_entrega: true,
                        calificacion_promedio: true
                    }
                }
            }
        });
        res.json(products);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Error al obtener los productos.' });
    }
};

exports.createProduct = async (req, res) => {
    try {
        const { restaurante_id, nombre, descripcion, precio, imagen_url, disponibilidad, tipo_comida, descuento_estudiante } = req.body;

        const newProduct = await prisma.product.create({
            data: {
                restaurante_id: parseInt(restaurante_id),
                nombre,
                descripcion,
                precio: parseFloat(precio),
                imagen_url,
                disponibilidad: disponibilidad !== undefined ? disponibilidad : true,
                tipo_comida: tipo_comida || 'General',
                descuento_estudiante: descuento_estudiante !== undefined ? parseFloat(descuento_estudiante) : 0.0
            }
        });

        res.status(201).json({ message: 'Producto creado exitosamente', product: newProduct });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Error al crear el producto.' });
    }
};

/**
 * Actualiza un producto. Solo se tocan los campos que llegan, para que editar
 * el precio no borre la descripción.
 */
exports.updateProduct = async (req, res) => {
    try {
        const id = parseInt(req.params.id, 10);
        if (Number.isNaN(id)) return res.status(400).json({ error: 'Identificador inválido.' });

        const actual = await prisma.product.findUnique({ where: { id } });
        if (!actual) return res.status(404).json({ error: 'Producto no encontrado.' });

        const { restaurante_id, nombre, descripcion, precio, imagen_url, disponibilidad, tipo_comida, descuento_estudiante } = req.body;

        if (precio !== undefined && (Number.isNaN(parseFloat(precio)) || parseFloat(precio) < 0)) {
            return res.status(400).json({ error: 'El precio debe ser un número positivo.' });
        }

        const producto = await prisma.product.update({
            where: { id },
            data: {
                restaurante_id: restaurante_id !== undefined ? parseInt(restaurante_id, 10) : actual.restaurante_id,
                nombre: nombre !== undefined ? String(nombre).trim() : actual.nombre,
                descripcion: descripcion !== undefined ? descripcion : actual.descripcion,
                precio: precio !== undefined ? parseFloat(precio) : actual.precio,
                imagen_url: imagen_url !== undefined ? imagen_url : actual.imagen_url,
                disponibilidad: disponibilidad !== undefined ? Boolean(disponibilidad) : actual.disponibilidad,
                tipo_comida: tipo_comida !== undefined ? tipo_comida : actual.tipo_comida,
                descuento_estudiante: descuento_estudiante !== undefined ? parseFloat(descuento_estudiante) : actual.descuento_estudiante
            }
        });

        res.json(producto);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Error al actualizar el producto.' });
    }
};

exports.deleteProduct = async (req, res) => {
    try {
        const id = parseInt(req.params.id, 10);
        if (Number.isNaN(id)) return res.status(400).json({ error: 'Identificador inválido.' });

        // OrderItem referencia al producto sin cascada: borrarlo rompería el
        // historial de pedidos que lo incluyen.
        const enPedidos = await prisma.orderItem.count({ where: { product_id: id } });
        if (enPedidos > 0) {
            return res.status(409).json({
                error: `No se puede eliminar: el producto aparece en ${enPedidos} pedido(s). Márcalo como no disponible.`
            });
        }

        await prisma.product.delete({ where: { id } });
        res.json({ message: 'Producto eliminado.' });
    } catch (error) {
        if (error.code === 'P2025') return res.status(404).json({ error: 'Producto no encontrado.' });
        console.error(error);
        res.status(500).json({ error: 'Error al eliminar el producto.' });
    }
};
