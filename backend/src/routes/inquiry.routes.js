const express = require('express');
const router = express.Router();
const inquiryController = require('../controllers/inquiry.controller');

router.post('/send-code', inquiryController.sendCode);
router.post('/verify-code', inquiryController.verifyCode);
router.post('/product-info', inquiryController.productInfo);
router.post('/motor-inquiry', inquiryController.motorInquiry);
router.post('/contact', inquiryController.contactForm);

module.exports = router;
