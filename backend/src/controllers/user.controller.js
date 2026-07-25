const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();
const { success, error } = require('../utils/response');

// GET /api/user/me
const getMe = async (req, res) => {
  try {
    const user = await prisma.user.findUnique({ where: { id: req.user.id } });
    if (!user) return error(res, 'User not found', 404);
    return success(res, {
      id: user.id, email: user.email, phone: user.phone,
      isVerified: user.isVerified, isAdmin: user.isAdmin, createdAt: user.createdAt,
    });
  } catch (err) {
    console.error('[getMe]', err);
    return error(res, 'Failed to get profile', 500);
  }
};

// GET /api/user/subscription
const getSubscription = async (req, res) => {
  try {
    const sub = await prisma.subscription.findUnique({ where: { userId: req.user.id } });
    if (!sub) return error(res, 'No subscription found', 404);

    const now = new Date();
    let currentStatus = sub.status;

    // Auto-expire if date has passed
    if (currentStatus === 'trial' && sub.trialEnd && now > sub.trialEnd) {
      currentStatus = 'expired';
      await prisma.subscription.update({ where: { userId: req.user.id }, data: { status: 'expired' } });
    } else if (currentStatus === 'active' && sub.subscriptionEnd && now > sub.subscriptionEnd) {
      currentStatus = 'expired';
      await prisma.subscription.update({ where: { userId: req.user.id }, data: { status: 'expired' } });
    }

    const endDate = currentStatus === 'trial' ? sub.trialEnd : sub.subscriptionEnd;
    const daysRemaining = endDate ? Math.max(0, Math.ceil((endDate - now) / (1000 * 60 * 60 * 24))) : 0;

    return success(res, {
      status: currentStatus,
      trialStart: sub.trialStart, trialEnd: sub.trialEnd,
      subscriptionStart: sub.subscriptionStart, subscriptionEnd: sub.subscriptionEnd,
      daysRemaining, plan: sub.plan,
    }, 'Subscription retrieved successfully');
  } catch (err) {
    console.error('[getSubscription]', err);
    return error(res, 'Failed to get subscription', 500);
  }
};

// DELETE /api/user/me
const deleteAccount = async (req, res) => {
  try {
    const userId = req.user.id;
    await prisma.$transaction([
      prisma.oTP.deleteMany({ where: { userId } }),
      prisma.subscription.deleteMany({ where: { userId } }),
      prisma.payment.deleteMany({ where: { userId } }),
      prisma.user.delete({ where: { id: userId } }),
    ]);
    return success(res, null, 'Account deleted successfully');
  } catch (err) {
    console.error('[deleteAccount]', err);
    return error(res, 'Failed to delete account', 500);
  }
};

module.exports = { getMe, getSubscription, deleteAccount };
