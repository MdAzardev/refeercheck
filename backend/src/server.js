require('dotenv').config();
const express = require('express');
const cors = require('cors');

const app = express();

// Middleware
app.use(cors());
app.use(express.json());

// Health check
app.get('/health', (req, res) => res.json({ status: 'ok', time: new Date().toISOString() }));

// Routes
app.use('/api/auth', require('./routes/auth.routes'));
app.use('/api/user', require('./routes/user.routes'));
app.use('/api/admin', require('./routes/admin.routes'));
app.use('/api/payment', require('./routes/payment.routes'));

// 404 handler
app.use((req, res) => {
  res.status(404).json({ success: false, message: `Route ${req.method} ${req.path} not found`, data: null });
});

// Global error handler
app.use((err, req, res, next) => {
  console.error('[SERVER ERROR]', err);
  res.status(500).json({ success: false, message: 'Internal server error', data: null });
});

const PORT = process.env.PORT || 8000;
app.listen(PORT, () => {
  console.log(`\n🚀 Reefer Check API running on port ${PORT}`);
  console.log(`   Health: http://localhost:${PORT}/health\n`);
});
