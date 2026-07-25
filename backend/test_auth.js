const http = require('http');

function post(path, data) {
  return new Promise((resolve, reject) => {
    const payload = JSON.stringify(data);
    const req = http.request({
      hostname: 'localhost',
      port: 8000,
      path,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(payload)
      }
    }, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => resolve({ status: res.statusCode, data: JSON.parse(body) }));
    });
    req.on('error', reject);
    req.write(payload);
    req.end();
  });
}

async function run() {
  const email = `user_${Date.now()}@test.com`;
  console.log('1. Registering user:', email);
  const reg = await post('/api/auth/register', {
    email,
    phone: '+1987654321',
    password: 'password123'
  });
  console.log('Register Response:', reg);

  // Read OTP from DB using Prisma
  const { PrismaClient } = require('@prisma/client');
  const prisma = new PrismaClient();
  const user = await prisma.user.findUnique({ where: { email }, include: { otps: true } });
  const otpCode = user.otps[0].code;
  console.log('2. Found OTP in DB:', otpCode);

  console.log('3. Verifying OTP...');
  const verify = await post('/api/auth/verify-otp', { email, otp: otpCode });
  console.log('Verify Response:', verify);

  await prisma.$disconnect();
}

run().catch(console.error);
