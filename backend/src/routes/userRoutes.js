const express = require('express');
const router = express.Router();
const userController = require('../controllers/userController');
const { authMiddleware } = require('../middlewares/authMiddleware');

router.post('/verify-student', authMiddleware, userController.verifyStudent);

module.exports = router;
