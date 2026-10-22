const express = require('express');
const router = express.Router();
const reviewController = require('../controllers/reviewController');
const { authMiddleware } = require('../middlewares/authMiddleware');

router.post('/', authMiddleware, reviewController.createReview);
router.get('/my-reviews', authMiddleware, reviewController.getMyReviews);

module.exports = router;
