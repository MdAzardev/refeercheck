const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();
const { success, error } = require('../utils/response');
const paypalService = require('../services/paypal.service');

// POST /api/payment/paypal/create-order
const createPaypalOrder = async (req, res) => {
  try {
    const result = await paypalService.createOrder();
    await prisma.payment.create({
      data: { userId: req.user.id, method: 'paypal', amount: 200, currency: 'INR', status: 'pending', reference: result.orderId },
    });
    return success(res, result, 'PayPal order created');
  } catch (err) {
    console.error('[createPaypalOrder]', err);
    return error(res, err.message || 'Failed to create PayPal order', 500);
  }
};

// POST /api/payment/paypal/capture/:orderId
const capturePaypalOrder = async (req, res) => {
  try {
    const { orderId } = req.params;
    const captureResult = await paypalService.captureOrder(orderId);
    if (captureResult.status !== 'COMPLETED') {
      return error(res, 'PayPal capture did not complete');
    }
    await prisma.payment.updateMany({ where: { reference: orderId }, data: { status: 'completed' } });
    // Activate 30-day (1 Month) subscription
    const subscriptionStart = new Date();
    const subscriptionEnd = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);
    await prisma.subscription.upsert({
      where: { userId: req.user.id },
      update: { status: 'active', subscriptionStart, subscriptionEnd, plan: 'monthly' },
      create: { userId: req.user.id, status: 'active', subscriptionStart, subscriptionEnd, plan: 'monthly' },
    });
    return success(res, null, 'Payment captured! Your 1-Month Pro subscription is active.');
  } catch (err) {
    console.error('[capturePaypalOrder]', err);
    return error(res, 'Failed to capture PayPal order', 500);
  }
};

module.exports = { createPaypalOrder, capturePaypalOrder };
