# CHONKIES Android Ordering App

A Kotlin + Jetpack Compose Android starter app using the established Chonkies look: dark ink `#17130F`, cream `#FFF3DF`, burnt orange `#F47716`, golden yellow `#FFB43B`, and the sunglasses-wearing burrito mascot.

## Included

- Android app source (Kotlin / Jetpack Compose)
- Menu categories: Burritos, Tacos, Quesadillas, Burrito Bowls, Shakes, Smoothies, Tots, Fries, Bubble Tea
- Menu browsing and category filters
- Burrito / bowl customization (protein, rice, beans, salsa)
- Cart quantity controls and checkout contact fields
- Stripe Android PaymentSheet integration
- Node/Express backend that creates Stripe PaymentIntents and calculates the order amount from its own catalog (not from a client-supplied price)
- Stripe webhook endpoint for successful payment events
- Chonkies mascot crop included in Android resources

## Important: demo data, not ready to take live orders

The item names, descriptions, options, and prices currently in `MainActivity.kt` and `backend/catalog.js` are starter/demo content. The dollar amounts are examples only and must be replaced with the approved Chonkies menu and prices in **both** places before testing checkout. Tax is set to zero until the correct local tax treatment/rate is configured. The current webhook logs paid orders; it does not yet send orders to a kitchen printer, POS, or persistent order database. Do not use this starter as a live production ordering service until those pieces, pickup hours, service area, refund policy, privacy policy, and operational testing are complete.

## Run the backend in Stripe test mode

1. Install Node.js 20 or later.
2. In `backend/`, run `npm install`.
3. Copy `.env.example` to `.env`.
4. From your Stripe Dashboard, copy your **test-mode** publishable key and secret key into `.env` (`pk_test_...` and `sk_test_...`). Never put the secret key in Android source code or share it publicly.
5. Set `STRIPE_WEBHOOK_SECRET` using Stripe CLI forwarding, for example: `stripe listen --forward-to localhost:4242/stripe-webhook`. Copy the displayed `whsec_...` value into `.env`.
6. Start the server with `npm start`.

The API listens on port 4242. `GET /health` should return `{ "ok": true }`.

## Open/build the Android app

1. Install Android Studio with Android SDK Platform 35 and JDK 17.
2. Open this project folder (`chonkies-android`) in Android Studio and allow Gradle sync.
3. Start an Android emulator. The default API address is `http://10.0.2.2:4242`, which reaches your computer from the emulator.
4. Press Run to install the app on the emulator.
5. Use Stripe test card `4242 4242 4242 4242`, any future expiry date, any CVC, and any postal code.

For a physical Android phone, change `API_BASE_URL` in `app/build.gradle.kts` to your computer's LAN address for local testing, or preferably your deployed HTTPS API URL. Do not expose a development server to the public internet.

## Before publishing to Google Play / accepting real payments

- Replace all demo menu products, prices, option groups, and availability with the actual Chonkies menu.
- Set correct sales tax and verify total display/receipt calculations.
- Add a real database and durable order status workflow; secure admin access and order-management tools.
- Configure Stripe live keys only on the backend and set a verified production webhook.
- Add kitchen/POS integration, pickup lead times, store hours, sold-out controls, cancellation/refund handling, and customer support details.
- Add privacy policy, terms, data retention practices, and Play Store listing assets.
- Use HTTPS, restrict CORS to your app/API clients as appropriate, add rate limiting, monitoring, backups, and test the complete payment/refund lifecycle.
- Generate a signed release build and enroll the app in Google Play Console.

## Project layout

- `app/` — Android application source and mascot artwork
- `backend/` — Stripe payment API and server-side demo catalog

Package/application ID: `com.chonkies.app`
