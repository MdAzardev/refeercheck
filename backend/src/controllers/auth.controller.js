const bcrypt = require('bcryptjs');
const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();
const { generateAccessToken, generateRefreshToken, verifyRefreshToken } = require('../utils/jwt');
const { success, error } = require('../utils/response');
const { generateOtp, saveOtp, verifyOtp } = require('../services/otp.service');
const { sendOtpEmail } = require('../services/email.service');

// POST /api/auth/register
const register = async (req, res) => {
  try {
    const { email, phone, password } = req.body;
    if (!email || !phone || !password) return error(res, 'Email, phone and password are required');
    const exists = await prisma.user.findUnique({ where: { email } });
    if (exists) return error(res, 'Email already registered');
    const hashed = await bcrypt.hash(password, 10);
    const user = await prisma.user.create({ data: { email, phone, password: hashed } });
    const otp = generateOtp();
    await saveOtp(user.id, otp);
    await sendOtpEmail(email, otp);
    return success(res, null, 'OTP sent to your email. Check your inbox.', 201);
  } catch (err) {
    console.error('[register]', err);
    return error(res, 'Registration failed', 500);
  }
};

// POST /api/auth/verify-otp
const verifyOtpHandler = async (req, res) => {
  try {
    const { email, otp } = req.body;
    if (!email || !otp) return error(res, 'Email and OTP are required');
    const user = await prisma.user.findUnique({ where: { email } });
    if (!user) return error(res, 'User not found', 404);
    const valid = await verifyOtp(user.id, otp);
    if (!valid) return error(res, 'Invalid or expired OTP');
    await prisma.user.update({ where: { id: user.id }, data: { isVerified: true } });
    // Create 30-day trial subscription
    const trialStart = new Date();
    const trialEnd = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);
    await prisma.subscription.upsert({
      where: { userId: user.id },
      update: { status: 'trial', trialStart, trialEnd },
      create: { userId: user.id, status: 'trial', trialStart, trialEnd },
    });
    const accessToken = generateAccessToken({ id: user.id, email: user.email, isAdmin: user.isAdmin });
    const refreshToken = generateRefreshToken({ id: user.id });
    return success(res, {
      accessToken,
      refreshToken,
      user: {
        id: user.id, email: user.email, phone: user.phone,
        isVerified: true, isAdmin: user.isAdmin, createdAt: user.createdAt,
      },
    }, 'Email verified successfully! Your 30-day trial has started.');
  } catch (err) {
    console.error('[verifyOtp]', err);
    return error(res, 'Verification failed', 500);
  }
};

// POST /api/auth/resend-otp
const resendOtp = async (req, res) => {
  try {
    const { email } = req.body;
    if (!email) return error(res, 'Email is required');
    const user = await prisma.user.findUnique({ where: { email } });
    if (!user) return error(res, 'User not found', 404);
    const otp = generateOtp();
    await saveOtp(user.id, otp);
    await sendOtpEmail(email, otp);
    return success(res, null, 'OTP resent successfully');
  } catch (err) {
    console.error('[resendOtp]', err);
    return error(res, 'Failed to resend OTP', 500);
  }
};

// POST /api/auth/login
const login = async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) return error(res, 'Email and password are required');
    const user = await prisma.user.findUnique({ where: { email } });
    if (!user) return error(res, 'Invalid email or password', 401);
    if (!user.isVerified) return error(res, 'Please verify your email before logging in');
    const match = await bcrypt.compare(password, user.password);
    if (!match) return error(res, 'Invalid email or password', 401);
    const accessToken = generateAccessToken({ id: user.id, email: user.email, isAdmin: user.isAdmin });
    const refreshToken = generateRefreshToken({ id: user.id });
    return success(res, {
      accessToken,
      refreshToken,
      user: {
        id: user.id, email: user.email, phone: user.phone,
        isVerified: user.isVerified, isAdmin: user.isAdmin, createdAt: user.createdAt,
      },
    }, 'Login successful');
  } catch (err) {
    console.error('[login]', err);
    return error(res, 'Login failed', 500);
  }
};

// POST /api/auth/refresh
const refresh = async (req, res) => {
  try {
    const { refreshToken } = req.body;
    if (!refreshToken) return error(res, 'Refresh token required', 401);
    const payload = verifyRefreshToken(refreshToken);
    const user = await prisma.user.findUnique({ where: { id: payload.id } });
    if (!user) return error(res, 'User not found', 404);
    const newAccessToken = generateAccessToken({ id: user.id, email: user.email, isAdmin: user.isAdmin });
    return success(res, { accessToken: newAccessToken }, 'Token refreshed');
  } catch {
    return error(res, 'Invalid or expired refresh token', 401);
  }
};

// POST /api/auth/logout
const logout = async (req, res) => success(res, null, 'Logged out successfully');

module.exports = { register, verifyOtp: verifyOtpHandler, resendOtp, login, refresh, logout };
