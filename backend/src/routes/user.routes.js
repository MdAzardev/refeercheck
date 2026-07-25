const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/auth');
const userController = require('../controllers/user.controller');

router.use(authMiddleware);
router.get('/me', userController.getMe);
router.get('/subscription', userController.getSubscription);
router.delete('/me', userController.deleteAccount);

module.exports = router;
