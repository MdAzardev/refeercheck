const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/auth');
const paymentController = require('../controllers/payment.controller');

router.use(authMiddleware);
router.post('/paypal/create-order', paymentController.createPaypalOrder);
router.post('/paypal/capture/:orderId', paymentController.capturePaypalOrder);

module.exports = router;
