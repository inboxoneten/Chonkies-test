require('dotenv').config();
const express = require('express');
const cors = require('cors');
const Stripe = require('stripe');
const catalog = require('./catalog');
const app = express();
const stripe = process.env.STRIPE_SECRET_KEY ? new Stripe(process.env.STRIPE_SECRET_KEY) : null;
const port = Number(process.env.PORT || 4242);
const currency = (process.env.CURRENCY || 'usd').toLowerCase();
const taxRate = Number(process.env.TAX_RATE || 0);

// Webhooks need the raw request body; keep this route before express.json().
app.post('/stripe-webhook', express.raw({ type: 'application/json' }), (req, res) => {
  if (!stripe || !process.env.STRIPE_WEBHOOK_SECRET) return res.status(503).send('Webhook is not configured');
  let event;
  try { event = stripe.webhooks.constructEvent(req.body, req.headers['stripe-signature'], process.env.STRIPE_WEBHOOK_SECRET); }
  catch (err) { return res.status(400).send(`Webhook signature error: ${err.message}`); }
  if (event.type === 'payment_intent.succeeded') {
    const intent = event.data.object;
    console.log('PAID CHONKIES ORDER', { paymentIntent: intent.id, amount: intent.amount, customer: intent.metadata.customer_name, items: intent.metadata.items });
    // Production: persist to a database and send the order to the kitchen/POS here.
  }
  res.json({ received: true });
});
app.use(cors());
app.use(express.json({ limit: '32kb' }));
app.get('/health', (_req, res) => res.json({ ok: true, service: 'Chonkies Ordering API' }));

app.post('/create-payment-intent', async (req, res) => {
  try {
    if (!stripe || !process.env.STRIPE_PUBLISHABLE_KEY || process.env.STRIPE_SECRET_KEY.includes('REPLACE_ME'))
      return res.status(503).json({ error: 'Stripe is not configured. Add your test keys to backend/.env.' });
    const { items, customer } = req.body || {};
    if (!Array.isArray(items) || items.length < 1 || items.length > 30) return res.status(400).json({ error: 'Invalid cart.' });
    if (!customer || typeof customer.name !== 'string' || typeof customer.email !== 'string' || !customer.name.trim() || !/^\S+@\S+\.\S+$/.test(customer.email))
      return res.status(400).json({ error: 'A valid customer name and email are required.' });
    let subtotal = 0;
    const normalized = items.map((item) => {
      const product = catalog[item.id];
      const quantity = Number(item.quantity);
      if (!product || !Number.isInteger(quantity) || quantity < 1 || quantity > 20) throw new Error('Invalid item or quantity.');
      subtotal += product.price * quantity;
      return { id: item.id, name: product.name, quantity, options: String(item.options || '').slice(0, 300) };
    });
    const tax = Math.round(subtotal * taxRate);
    const amount = subtotal + tax;
    if (amount < 50 || amount > 1000000) return res.status(400).json({ error: 'Order total is outside the supported range.' });
    const intent = await stripe.paymentIntents.create({
      amount, currency,
      automatic_payment_methods: { enabled: true },
      receipt_email: customer.email,
      description: 'Chonkies pickup order',
      metadata: { customer_name: customer.name.slice(0, 100), customer_phone: String(customer.phone || '').slice(0, 40), pickup_type: 'pickup', items: JSON.stringify(normalized).slice(0, 480) }
    });
    res.json({ clientSecret: intent.client_secret, publishableKey: process.env.STRIPE_PUBLISHABLE_KEY, amount, currency });
  } catch (err) { console.error(err); res.status(400).json({ error: err.message || 'Unable to create payment.' }); }
});
app.listen(port, () => console.log(`Chonkies API listening on port ${port}`));
