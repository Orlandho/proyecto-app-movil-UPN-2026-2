const express = require('express');
const router = express.Router();
const restaurantController = require('../controllers/restaurantController');
const { authMiddleware, requireRole } = require('../middlewares/authMiddleware');

// Públicas: el catálogo y el mapa de locales las consumen sin sesión.
router.get('/', restaurantController.getAllRestaurants);
router.get('/:id', restaurantController.getRestaurantById);

// La gestión de locales es cosa del personal.
router.post('/', authMiddleware, requireRole('admin'), restaurantController.createRestaurant);
router.put('/:id', authMiddleware, requireRole('admin'), restaurantController.updateRestaurant);
router.delete('/:id', authMiddleware, requireRole('admin'), restaurantController.deleteRestaurant);

module.exports = router;
