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
          amount: { currency_code: 'USD', value: '99.00' },
          description: 'Reefer Check Pro — Annual Subscription',
        },
      ],
    }),
  });
  const data = await res.json();
  return { orderId: data.id };
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
  return res.json();
};

module.exports = { createOrder, captureOrder };
