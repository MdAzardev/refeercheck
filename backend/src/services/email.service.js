const nodemailer = require('nodemailer');

const createTransporter = () => {
  if (process.env.EMAIL_HOST) {
    return nodemailer.createTransport({
      host: process.env.EMAIL_HOST,
      port: parseInt(process.env.EMAIL_PORT || '587'),
      secure: parseInt(process.env.EMAIL_PORT || '587') === 465,
      auth: {
        user: process.env.EMAIL_USER,
        pass: process.env.EMAIL_PASS,
      },
      tls: {
        rejectUnauthorized: false,
      },
    });
  }
  
  return nodemailer.createTransport({
    service: 'gmail',
    auth: {
      user: process.env.EMAIL_USER,
      pass: process.env.EMAIL_PASS,
    },
  });
};

const transporter = createTransporter();

// Verify connection configuration on startup
transporter.verify((error) => {
  if (error) {
    console.error('❌ Email server configuration error:', error.message);
  } else {
    console.log('✅ Email server is ready to send messages');
  }
});

const sendOtpEmail = async (to, otp) => {
  console.log(`\n========================================`);
  console.log(`[DEV OTP CODE] Email: ${to} | OTP: ${otp}`);
  console.log(`========================================\n`);

  try {
    if (process.env.EMAIL_USER && process.env.EMAIL_PASS && !process.env.EMAIL_USER.includes('your_email')) {
      await transporter.sendMail({
        from: `"Reefer Check" <${process.env.EMAIL_USER}>`,
        to,
        subject: 'Your OTP Code — Reefer Check',
        html: `
          <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;background:#0D1B2A;color:#fff;padding:32px;border-radius:12px">
            <div style="text-align:center;margin-bottom:24px">
              <h2 style="color:#00BFFF;margin:0;font-size:24px;letter-spacing:2px">REEFER CHECK</h2>
              <p style="color:#778DA9;margin:4px 0 0;font-size:13px">Powered by M.R.Rawther</p>
            </div>
            <hr style="border:none;border-top:1px solid #1E3A5F;margin:20px 0">
            <p style="color:#E0E1DD;margin:0 0 16px">Your one-time verification code is:</p>
            <div style="background:#1E3A5F;border-radius:8px;padding:20px;text-align:center;margin:16px 0">
              <span style="font-size:40px;font-weight:bold;letter-spacing:12px;color:#00BFFF">${otp}</span>
            </div>
            <p style="color:#778DA9;font-size:13px;margin:16px 0 0">
              This code expires in <strong>10 minutes</strong>. Do not share it with anyone.
            </p>
            <hr style="border:none;border-top:1px solid #1E3A5F;margin:20px 0">
            <p style="color:#4A6580;font-size:11px;text-align:center;margin:0">
              If you didn't request this code, please ignore this email.
            </p>
          </div>
        `,
      });
      console.log(`[EMAIL SUCCESS] OTP email sent to ${to}`);
    } else {
      console.log(`[EMAIL SKIPPED] No SMTP credentials configured. Use console OTP above.`);
    }
  } catch (err) {
    console.error(`[EMAIL ERROR] Failed to send email to ${to}:`, err.message);
  }
};

const sendMail = async (options) => {
  return transporter.sendMail(options);
};

module.exports = { transporter, sendOtpEmail, sendMail, createTransporter };
