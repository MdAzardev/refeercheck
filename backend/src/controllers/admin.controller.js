const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();
const { success, error } = require('../utils/response');

// GET /api/admin/users?page=1&limit=20&search=
const getUsers = async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 20;
    const search = req.query.search;
    const where = search
      ? { OR: [{ email: { contains: search } }, { phone: { contains: search } }] }
      : {};
    const [users, total] = await Promise.all([
      prisma.user.findMany({
        where, skip: (page - 1) * limit, take: limit,
        include: { subscription: true },
        orderBy: { createdAt: 'desc' },
      }),
      prisma.user.count({ where }),
    ]);
    const now = new Date();
    const mapped = users.map((u) => {
      const sub = u.subscription;
      let daysRemaining = 0;
      if (sub) {
        const endDate = sub.status === 'trial' ? sub.trialEnd : sub.subscriptionEnd;
        daysRemaining = endDate ? Math.max(0, Math.ceil((endDate - now) / (1000 * 60 * 60 * 24))) : 0;
      }
      return {
        id: u.id, email: u.email, phone: u.phone,
        isVerified: u.isVerified, isAdmin: u.isAdmin, createdAt: u.createdAt,
        subscription: sub
          ? {
              status: sub.status, plan: sub.plan, daysRemaining,
              trialStart: sub.trialStart, trialEnd: sub.trialEnd,
              subscriptionStart: sub.subscriptionStart, subscriptionEnd: sub.subscriptionEnd,
            }
          : null,
      };
    });
    return success(res, { users: mapped, total, page, totalPages: Math.ceil(total / limit) });
  } catch (err) {
    console.error('[getUsers]', err);
    return error(res, 'Failed to get users', 500);
  }
};

// GET /api/admin/stats
const getStats = async (req, res) => {
  try {
    const [totalUsers, activeSubscriptions, trialUsers, expiredUsers, unverifiedUsers] =
      await Promise.all([
        prisma.user.count(),
        prisma.subscription.count({ where: { status: 'active' } }),
        prisma.subscription.count({ where: { status: 'trial' } }),
        prisma.subscription.count({ where: { status: 'expired' } }),
        prisma.user.count({ where: { isVerified: false } }),
      ]);
    return success(res, { totalUsers, activeSubscriptions, trialUsers, expiredUsers, unverifiedUsers });
  } catch (err) {
    console.error('[getStats]', err);
    return error(res, 'Failed to get stats', 500);
  }
};

// PATCH /api/admin/users/:id/subscription
const updateSubscription = async (req, res) => {
  try {
    const userId = parseInt(req.params.id);
    const { status, plan } = req.body;
    if (!status) return error(res, 'Status is required');
    const data = { status };
    if (plan) data.plan = plan;
    if (status === 'active') {
      data.subscriptionStart = new Date();
      data.subscriptionEnd = new Date(Date.now() + 365 * 24 * 60 * 60 * 1000);
    }
    if (status === 'trial') {
      data.trialStart = new Date();
      data.trialEnd = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);
    }
    await prisma.subscription.upsert({
      where: { userId },
      update: data,
      create: { userId, ...data },
    });
    return success(res, null, 'Subscription updated successfully');
  } catch (err) {
    console.error('[updateSubscription]', err);
    return error(res, 'Failed to update subscription', 500);
  }
};

// DELETE /api/admin/users/:id
const deleteUser = async (req, res) => {
  try {
    const userId = parseInt(req.params.id);
    await prisma.$transaction([
      prisma.oTP.deleteMany({ where: { userId } }),
      prisma.subscription.deleteMany({ where: { userId } }),
      prisma.payment.deleteMany({ where: { userId } }),
      prisma.user.delete({ where: { id: userId } }),
    ]);
    return success(res, null, 'User deleted successfully');
  } catch (err) {
    console.error('[deleteUser]', err);
    return error(res, 'Failed to delete user', 500);
  }
};

module.exports = { getUsers, getStats, updateSubscription, deleteUser };
