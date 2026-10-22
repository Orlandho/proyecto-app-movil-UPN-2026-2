const express = require('express');
const router = express.Router();
const addressController = require('../controllers/addressController');
const { authMiddleware } = require('../middlewares/authMiddleware');

router.get('/', authMiddleware, addressController.getMyAddresses);
router.post('/', authMiddleware, addressController.createAddress);
router.put('/:id', authMiddleware, addressController.updateAddress);
router.delete('/:id', authMiddleware, addressController.deleteAddress);

module.exports = router;
