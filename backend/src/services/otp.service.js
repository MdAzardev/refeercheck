const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

const generateOtp = () => Math.floor(100000 + Math.random() * 900000).toString();

const saveOtp = async (userId, code) => {
  // Delete any existing OTPs for this user first
  await prisma.oTP.deleteMany({ where: { userId } });
  const expiresAt = new Date(Date.now() + 10 * 60 * 1000); // 10 minutes
  return prisma.oTP.create({ data: { userId, code, expiresAt } });
};

const verifyOtp = async (userId, code) => {
  const otp = await prisma.oTP.findFirst({
    where: { userId, code, expiresAt: { gt: new Date() } },
  });
  if (!otp) return false;
  await prisma.oTP.delete({ where: { id: otp.id } });
  return true;
};

module.exports = { generateOtp, saveOtp, verifyOtp };
