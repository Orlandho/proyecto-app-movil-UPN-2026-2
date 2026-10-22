const express = require('express');
const router = express.Router();
const orderController = require('../controllers/orderController');
const { authMiddleware, requireRole } = require('../middlewares/authMiddleware');

// Rutas protegidas (requieren autenticación)
router.post('/', authMiddleware, orderController.createOrder);
router.get('/my-orders', authMiddleware, orderController.getMyOrders);

// Listado completo para el panel de operaciones
router.get('/', authMiddleware, requireRole('admin', 'repartidor'), orderController.getAllOrders);

// OJO: '/:id' debe declararse DESPUÉS de '/my-orders'. Al revés, Express
// trataría "my-orders" como un identificador y esa ruta dejaría de existir.
router.get('/:id', authMiddleware, orderController.getOrderById);

// Gestión de estados: solo el personal puede mover un pedido. Antes cualquier
// usuario autenticado podía cambiar el estado de cualquier pedido.
router.put('/:id/status', authMiddleware, requireRole('admin', 'repartidor'), orderController.updateOrderStatus);

// Cancelación de orden (la hace el propio cliente dueño del pedido)
router.put('/:id/cancel', authMiddleware, orderController.cancelOrder);

module.exports = router;
