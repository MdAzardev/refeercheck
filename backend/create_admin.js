const bcrypt = require('bcryptjs');
const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function main() {
  const adminEmail = 'admin@reefercheck.com';
  const adminPassword = 'admin123';
  const hashedAdmin = await bcrypt.hash(adminPassword, 10);

  // 1. Create dedicated admin account
  const adminUser = await prisma.user.upsert({
    where: { email: adminEmail },
    update: { password: hashedAdmin, isVerified: true, isAdmin: true },
    create: { email: adminEmail, phone: '+18005550199', password: hashedAdmin, isVerified: true, isAdmin: true },
  });

  const trialStart = new Date();
  const trialEnd = new Date(Date.now() + 365 * 24 * 60 * 60 * 1000);
  await prisma.subscription.upsert({
    where: { userId: adminUser.id },
    update: { status: 'active', subscriptionStart: trialStart, subscriptionEnd: trialEnd },
    create: { userId: adminUser.id, status: 'active', subscriptionStart: trialStart, subscriptionEnd: trialEnd },
  });

  // 2. Also promote Imrankmi7777@gmail.com to Admin
  const user2 = await prisma.user.findUnique({ where: { email: 'Imrankmi7777@gmail.com' } });
  if (user2) {
    await prisma.user.update({
      where: { id: user2.id },
      data: { isAdmin: true },
    });
    console.log(`Promoted Imrankmi7777@gmail.com to Admin as well.`);
  }

  console.log(`\n========================================`);
  console.log(`DEDICATED ADMIN CREATED SUCCESSFULLY!`);
  console.log(`Email: ${adminEmail}`);
  console.log(`Password: ${adminPassword}`);
  console.log(`isAdmin: true`);
  console.log(`========================================\n`);

  await prisma.$disconnect();
}

main().catch(console.error);
