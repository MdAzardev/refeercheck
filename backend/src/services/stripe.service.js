const Stripe = require('stripe');
const stripe = new Stripe(process.env.STRIPE_SECRET_KEY);

const createCheckoutSession = async (userEmail) => {
  const session = await stripe.checkout.sessions.create({
    payment_method_types: ['card'],
    mode: 'payment',
    customer_email: userEmail,
    line_items: [
      {
        price_data: {
          currency: 'usd',
          product_data: { name: 'Reefer Check Pro — Annual Subscription' },
          unit_amount: 9900, // $99.00
        },
        quantity: 1,
      },
    ],
    success_url: `${process.env.APP_URL}/payment-success`,
    cancel_url: `${process.env.APP_URL}/payment-cancel`,
  });
  return { sessionId: session.id, url: session.url };
};

module.exports = { createCheckoutSession };
