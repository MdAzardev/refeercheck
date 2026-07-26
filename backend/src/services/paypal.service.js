const fetch = require('node-fetch');

const getAccessToken = async () => {
  const res = await fetch(`${process.env.PAYPAL_API}/v1/oauth2/token`, {
    method: 'POST',
    headers: {
      Authorization: `Basic ${Buffer.from(
        `${process.env.PAYPAL_CLIENT_ID}:${process.env.PAYPAL_CLIENT_SECRET}`
      ).toString('base64')}`,
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: 'grant_type=client_credentials',
  });
  const data = await res.json();
  if (!res.ok) {
    throw new Error(data.error_description || 'Failed to authenticate with PayPal');
  }
  return data.access_token;
};

const createOrder = async () => {
  const token = await getAccessToken();
  const res = await fetch(`${process.env.PAYPAL_API}/v2/checkout/orders`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      intent: 'CAPTURE',
      purchase_units: [
        {
          amount: { currency_code: 'USD', value: '2.50' },
          description: 'Reefer Check Pro — Monthly Subscription (₹200)',
        },
      ],
      application_context: {
        return_url: `${process.env.APP_URL || 'http://localhost:8000'}/api/payment/paypal/success`,
        cancel_url: `${process.env.APP_URL || 'http://localhost:8000'}/api/payment/paypal/cancel`,
        user_action: 'PAY_NOW',
      },
    }),
  });
  const data = await res.json();
  if (!res.ok) {
    console.error('[PayPal Error]', data);
    throw new Error(data.message || (data.details && data.details[0] ? data.details[0].issue : 'Failed to create PayPal order'));
  }
  const approveUrl = data.links ? data.links.find(l => l.rel === 'approve')?.href : null;
  return { orderId: data.id, approveUrl };
};

const captureOrder = async (orderId) => {
  const token = await getAccessToken();
  const res = await fetch(
    `${process.env.PAYPAL_API}/v2/checkout/orders/${orderId}/capture`,
    {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
    }
  );
  const data = await res.json();
  return data;
};

module.exports = { createOrder, captureOrder };
