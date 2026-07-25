const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();
const { success, error } = require('../utils/response');
const stripeService = require('../services/stripe.service');
const paypalService = require('../services/paypal.service');

// POST /api/payment/stripe/create-session
const createStripeSession = async (req, res) => {
  try {
    const user = await prisma.user.findUnique({ where: { id: req.user.id } });
    if (!user) return error(res, 'User not found', 404);
    const result = await stripeService.createCheckoutSession(user.email);
    await prisma.payment.create({
      data: { userId: req.user.id, method: 'stripe', amount: 99, status: 'pending', reference: result.sessionId },
    });
    return success(res, result, 'Stripe session created');
  } catch (err) {
    console.error('[createStripeSession]', err);
    return error(res, 'Failed to create Stripe session', 500);
  }
};

// POST /api/payment/paypal/create-order
const createPaypalOrder = async (req, res) => {
  try {
    const result = await paypalService.createOrder();
    await prisma.payment.create({
      data: { userId: req.user.id, method: 'paypal', amount: 99, status: 'pending', reference: result.orderId },
    });
    return success(res, result, 'PayPal order created');
  } catch (err) {
    console.error('[createPaypalOrder]', err);
    return error(res, 'Failed to create PayPal order', 500);
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
    // Activate 1-year subscription
    const subscriptionStart = new Date();
    const subscriptionEnd = new Date(Date.now() + 365 * 24 * 60 * 60 * 1000);
    await prisma.subscription.upsert({
      where: { userId: req.user.id },
      update: { status: 'active', subscriptionStart, subscriptionEnd, plan: 'annual' },
      create: { userId: req.user.id, status: 'active', subscriptionStart, subscriptionEnd, plan: 'annual' },
    });
    return success(res, null, 'Payment captured and subscription activated for 1 year');
  } catch (err) {
    console.error('[capturePaypalOrder]', err);
    return error(res, 'Failed to capture PayPal order', 500);
  }
};

module.exports = { createStripeSession, createPaypalOrder, capturePaypalOrder };
