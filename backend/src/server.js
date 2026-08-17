require('dotenv').config();
const express = require('express');
const cors = require('cors');
const path = require('path');
const inquiryRoutes = require('./routes/inquiry.routes');

const app = express();

// Allowed Origins for CORS
const allowedOrigins = (process.env.ALLOWED_ORIGINS ? process.env.ALLOWED_ORIGINS.split(',') : [
  'http://localhost:3000',
  'http://localhost:3001',
  'http://localhost:5173',
  'http://localhost:8000',
  'http://localhost:5200',
  'https://mrrawthereltech.com',
  'https://www.mrrawthereltech.com'
]).map(o => o.trim());

// Middleware - CORS
app.use(cors({
  origin: function (origin, callback) {
    if (!origin || allowedOrigins.includes(origin) || process.env.NODE_ENV !== 'production') {
      callback(null, true);
    } else {
      console.error('❌ CORS blocked origin:', origin);
      callback(new Error('Not allowed by CORS'));
    }
  },
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
  credentials: true
}));

app.options('*', cors());

// Middleware - Body Parser with 10MB payload limit
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Static file serving (if build directory exists)
const buildPath = path.join(__dirname, '../build');
app.use(express.static(buildPath));

const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

// Health check endpoint
app.get('/health', async (req, res) => {
  let dbConnected = false;
  try {
    await prisma.$queryRaw`SELECT 1`;
    dbConnected = true;
  } catch (err) {}

  res.status(200).json({
    status: 'OK',
    service: 'MR RAWTHER ELTECH & Reefer Check Unified API',
    database: dbConnected ? 'Connected (PostgreSQL)' : 'Disconnected',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
    environment: process.env.NODE_ENV || 'development',
    version: '1.0.0'
  });
});

// Root level EmailSender Endpoints (Backwards Compatibility)
const inquiryController = require('./controllers/inquiry.controller');
app.post('/send-code', inquiryController.sendCode);
app.post('/verify-code', inquiryController.verifyCode);
app.post('/product-info', inquiryController.productInfo);
app.post('/motor-inquiry', inquiryController.motorInquiry);
app.post('/api/contact', inquiryController.contactForm);

// Modular API Routes
app.use('/api/inquiry', inquiryRoutes);
app.use('/api/auth', require('./routes/auth.routes'));
app.use('/api/user', require('./routes/user.routes'));
app.use('/api/admin', require('./routes/admin.routes'));
app.use('/api/payment', require('./routes/payment.routes'));

// Catch-all route for SPA frontend fallback (if index.html exists in build)
app.get('*', (req, res, next) => {
  if (req.path.startsWith('/api') || req.path.startsWith('/send-code') || req.path.startsWith('/verify-code') || req.path.startsWith('/product-info') || req.path.startsWith('/motor-inquiry')) {
    return next();
  }
  const indexHtml = path.join(buildPath, 'index.html');
  if (require('fs').existsSync(indexHtml)) {
    return res.sendFile(indexHtml);
  }
  next();
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({ success: false, message: `Route ${req.method} ${req.path} not found`, data: null });
});

// Global error handler
app.use((err, req, res, next) => {
  console.error('[SERVER ERROR]', err);
  res.status(500).json({ success: false, message: err.message || 'Internal server error', data: null });
});

const PORT = process.env.PORT || 5200;
app.listen(PORT, async () => {
  console.log(`\n🚀 Consolidated Backend API running on port ${PORT}`);
  console.log(`   Health Check: http://localhost:${PORT}/health`);
  console.log(`   Services: ReeferCheck Auth/User/Admin/Payment + EmailSender Inquiries & Verification`);

  try {
    await prisma.$connect();
    console.log(`   🗄️  Database: Connected to PostgreSQL (reefercheck @ 44.213.10.56:5432)\n`);
  } catch (err) {
    console.error(`   ❌ Database Connection Failed: ${err.message}\n`);
  }
});
