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

// Account Deletion Request Page (required by Google Play Store policy)
app.get('/delete-account', (req, res) => {
  res.send(`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Delete Account - Reefer Check</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: 'Segoe UI', sans-serif; background: #0D1B2A; color: #e0e0e0; min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 24px; }
    .card { background: #1a2a3a; border-radius: 16px; padding: 40px; max-width: 560px; width: 100%; box-shadow: 0 8px 32px rgba(0,0,0,0.4); }
    .logo { font-size: 28px; font-weight: 700; color: #00BFFF; margin-bottom: 8px; }
    .subtitle { color: #88aacc; font-size: 14px; margin-bottom: 32px; }
    h1 { font-size: 22px; font-weight: 600; margin-bottom: 16px; color: #fff; }
    p { font-size: 15px; line-height: 1.7; color: #b0c4d8; margin-bottom: 16px; }
    .steps { background: #0f2030; border-radius: 10px; padding: 20px 24px; margin: 20px 0; }
    .steps ol { padding-left: 20px; }
    .steps li { padding: 6px 0; font-size: 14px; color: #c0d8ee; }
    .highlight { color: #00BFFF; font-weight: 600; }
    .email-box { background: #0D1B2A; border: 1px solid #00BFFF44; border-radius: 8px; padding: 14px 18px; margin: 16px 0; font-size: 15px; }
    .note { font-size: 13px; color: #778899; background: #0f2030; border-left: 3px solid #00BFFF; padding: 12px 16px; border-radius: 4px; margin-top: 20px; }
  </style>
</head>
<body>
  <div class="card">
    <div class="logo">❄️ Reefer Check</div>
    <div class="subtitle">Account Management</div>
    <h1>Request Account Deletion</h1>
    <p>You can request deletion of your Reefer Check account and all associated data at any time.</p>
    <div class="steps">
      <ol>
        <li>Send an email to <span class="highlight">support@reefercheck.com</span></li>
        <li>Use subject: <span class="highlight">Delete My Account</span></li>
        <li>Include your registered <span class="highlight">email address</span> in the message</li>
        <li>We will process your request within <span class="highlight">7 business days</span></li>
      </ol>
    </div>
    <div class="email-box">
      📧 <strong>support@reefercheck.com</strong>
    </div>
    <p>Upon deletion, the following data will be permanently removed:</p>
    <p>✅ Account profile (name, email, phone)<br/>
       ✅ Subscription records<br/>
       ✅ All app activity and history</p>
    <div class="note">
      ℹ️ Some anonymised data may be retained for legal compliance and fraud prevention for up to 90 days.
    </div>
  </div>
</body>
</html>`);
});


// Privacy Policy Page (required by Google Play Store policy)
app.get('/privacy-policy', (req, res) => {
  res.send(`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>Privacy Policy - Reefer Check</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: 'Segoe UI', sans-serif; background: #0D1B2A; color: #e0e0e0; padding: 24px; }
    .container { max-width: 720px; margin: 0 auto; padding: 40px 0; }
    .logo { font-size: 26px; font-weight: 700; color: #00BFFF; margin-bottom: 4px; }
    .updated { color: #88aacc; font-size: 13px; margin-bottom: 36px; }
    h1 { font-size: 24px; font-weight: 700; color: #fff; margin-bottom: 24px; }
    h2 { font-size: 17px; font-weight: 600; color: #00BFFF; margin: 28px 0 10px; }
    p, li { font-size: 14px; line-height: 1.8; color: #b0c4d8; margin-bottom: 10px; }
    ul { padding-left: 20px; margin-bottom: 10px; }
    a { color: #00BFFF; }
    .divider { border: none; border-top: 1px solid #1e3a50; margin: 20px 0; }
  </style>
</head>
<body>
  <div class="container">
    <div class="logo">❄️ Reefer Check</div>
    <div class="updated">Last updated: September 2026</div>
    <h1>Privacy Policy</h1>
    <p>This Privacy Policy describes how Reefer Check ("we", "our", or "us") collects, uses, and protects your personal information when you use our mobile application.</p>

    <h2>1. Information We Collect</h2>
    <ul>
      <li><strong>Name</strong> – collected during account registration</li>
      <li><strong>Email address</strong> – used for login and account management</li>
      <li><strong>Phone number</strong> – used for OTP verification</li>
      <li><strong>App activity</strong> – alarm lookups and app usage for improving the service</li>
    </ul>

    <h2>2. How We Use Your Information</h2>
    <ul>
      <li>To create and manage your account</li>
      <li>To verify your identity via OTP</li>
      <li>To provide access to app features (alarm lookup, manuals, reefer rounds)</li>
      <li>To manage your subscription and trial period</li>
      <li>To send important service notifications</li>
    </ul>

    <h2>3. Data Storage & Security</h2>
    <p>All data is stored securely on our servers. All data transmitted between the app and our servers is encrypted using HTTPS (TLS). We do not sell or share your personal data with third parties.</p>

    <h2>4. Data Retention</h2>
    <p>We retain your data for as long as your account is active. Upon account deletion, all personal data is permanently removed within 7 business days. Some anonymised data may be retained for up to 90 days for legal compliance.</p>

    <h2>5. Account Deletion</h2>
    <p>You can delete your account at any time from within the app (Profile → Delete Account) or by submitting a request at:</p>
    <p><a href="/delete-account">https://mrrawthereltech.com/delete-account</a></p>

    <h2>6. Children's Privacy</h2>
    <p>Reefer Check is intended for professional use by adults. We do not knowingly collect data from children under 13.</p>

    <h2>7. Changes to This Policy</h2>
    <p>We may update this Privacy Policy from time to time. Any changes will be posted on this page with an updated date.</p>

    <h2>8. Contact Us</h2>
    <p>If you have any questions about this Privacy Policy, please contact us at:<br/>
    📧 <a href="mailto:support@reefercheck.com">support@reefercheck.com</a></p>
  </div>
</body>
</html>`);
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
