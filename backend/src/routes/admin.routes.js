const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/auth');
const adminOnly = require('../middleware/adminOnly');
const adminController = require('../controllers/admin.controller');

router.use(authMiddleware, adminOnly);
router.get('/users', adminController.getUsers);
router.get('/stats', adminController.getStats);
router.patch('/users/:id/subscription', adminController.updateSubscription);
router.delete('/users/:id', adminController.deleteUser);

module.exports = router;
