# Payment Flow

This document describes the implemented EventFinder ticket payment flow using Safepay sandbox, a Cloudflare Worker backend, and Firebase Firestore.

## Current Status

The payment flow is functionally complete for sandbox card payments:

- Paid private ticket checkout opens Safepay.
- Successful Safepay card payment returns to the app.
- Cloudflare Worker verifies the Safepay tracker.
- Worker creates the ticket in Firestore.
- Android reads the ticket from Firestore.
- Event detail changes from `Buy Ticket` to `View Ticket`.
- Ticket detail shows the QR code.
- Tickets tab shows the purchased ticket.
- Free, zero-price, and public reservation events bypass Safepay.

Known Safepay sandbox caveat:

- Google Pay currently fails on Safepay's hosted page with `GPAY_INIT` / token signature errors.
- Use card payment for sandbox testing.
- JazzCash/Easypaisa are not currently available in this Safepay hosted checkout configuration.

## Payment Rules

The app treats payment as required only when all of these are true:

- event is not `PUBLIC`
- `requiresTicket == true`
- `price > 0`

Implemented helper:

```kotlin
Event.requiresPaidCheckout()
```

Behavior:

- Public events: free reservation, no Safepay.
- Free private tickets: direct QR ticket creation, no Safepay.
- Zero-price tickets: direct QR ticket creation, no Safepay.
- Paid private tickets: Safepay checkout.

## Main Components

Android:

- `EventDetailFragment`
- `EventDetailViewModel`
- `CloudflareWorkerPaymentGateway`
- `PurchaseTicketUseCase`
- `TicketDetailFragment`
- `TicketsFragment`
- `UserPreferences`

Backend:

- `workers/payment/src/index.js`
- Cloudflare Worker URL:

```text
https://eventfinder-payments.kh-basitmehmood.workers.dev
```

Worker endpoints:

- `POST /create-ticket-checkout`
- `POST /confirm-ticket-checkout`
- `POST /safepay-webhook`
- `GET /payment-complete`
- `GET /payment-cancel`

Firestore collections:

- `events`
- `payment_checkouts`
- `tickets`
- `event_stats`
- `payment_webhooks`

## Consumer Paid Ticket Flow

1. Consumer opens a paid private event in `EventDetailFragment`.
2. App shows `Buy Ticket`.
3. User confirms purchase.
4. App calls Worker:

```text
POST /create-ticket-checkout
```

5. Worker reads the event from Firestore and validates:

- event exists
- event is not public
- event requires a ticket
- event price is greater than zero
- request amount matches Firestore event price
- currency is `PKR`

6. Worker creates a Safepay tracker using:

```text
/order/payments/v3/
```

7. Worker gets a Safepay passport token using:

```text
/client/passport/v1/token
```

8. Worker writes a `payment_checkouts/{checkoutId}` document.
9. Worker returns the hosted checkout URL to Android.
10. Android stores pending checkout data locally before opening Safepay:

- checkout id
- event id
- user id

11. Android opens Safepay in the browser.
12. User pays by card in Safepay sandbox.
13. Safepay redirects to the Worker HTTPS callback:

```text
/payment-complete?checkoutId=...&eventId=...&userId=...
```

14. Worker returns a small page that redirects back into the app:

```text
eventfinder://payment/complete?checkoutId=...&eventId=...&userId=...
```

15. Android returns to `EventDetailFragment`.
16. App calls Worker:

```text
POST /confirm-ticket-checkout
```

17. Worker reads `payment_checkouts/{checkoutId}`.
18. Worker verifies the Safepay tracker through the reporter API:

```text
/reporter/api/v1/payments/{tracker}
```

19. Worker retries the reporter check briefly because Safepay status can lag after redirect.
20. Worker treats known paid states as successful, including:

- `TRACKER_ENDED`
- `PAID`
- `SUCCESS`
- `APPROVED`
- `CAPTURED`
- `COMPLETED`

21. Worker creates a Firestore ticket:

```text
tickets/{ticketId}
```

22. Worker updates:

- `payment_checkouts/{checkoutId}`
- `events/{eventId}.currentParticipantCount`
- `event_stats/{eventId}`

23. Worker returns a receipt with `ticketId`.
24. Android stores `confirmedTicketId` in UI state.
25. Button changes to `View Ticket`.
26. Popup `View Ticket` navigates to `TicketDetailFragment`.
27. Ticket detail loads the Firestore ticket and renders QR code.

## Free Ticket / Public Reservation Flow

Safepay is not involved.

1. Consumer taps:

- `I am going` for public reservation
- `Get Free Ticket` for free private ticket

2. Android calls `PurchaseTicketUseCase`.
3. App creates ticket directly in Firestore through the existing ticket repository.
4. Ticket QR is generated from stored `qrCodeData`.
5. User can view ticket immediately.

## Return And Recovery Behavior

The app supports three return scenarios:

1. App remains alive:

- `MainActivity` uses `singleTop`.
- Deep link reuses the existing app task.
- Existing `EventDetailFragment` verifies the checkout.

2. App is relaunched by deep link:

- Deep link includes `checkoutId`, `eventId`, and `userId`.
- `MainActivity` navigates to `EventDetailFragment`.
- Fragment verifies the checkout after event data loads.

3. User returns later or payment was incomplete:

- App stores pending checkout before opening Safepay.
- If verification returns incomplete/failed, local pending state is cleared.
- Next `Buy Ticket` tap starts a fresh checkout instead of repeatedly verifying the old checkout.

## Duplicate Protection

The Worker prevents duplicate paid tickets.

Before issuing a ticket, it checks for an existing ticket with the same:

- `eventId`
- `userId`

If one exists, the Worker marks the checkout paid and returns the existing ticket id instead of creating another ticket.

## Firestore Ticket Shape

Paid ticket documents include:

- `ticketId`
- `eventId`
- `eventTitle`
- `eventStartTime`
- `userId`
- `userName`
- `userEmail`
- `ticketType = PAID`
- `status = PURCHASED`
- `qrCodeData`
- `purchasePrice`
- `currency = PKR`
- `paymentStatus = PAID`
- `paymentProvider = SAFEPAY`
- `paymentTransactionId`
- `paidAt`
- `purchasedAt`
- `eventLocation`
- `organizerId`
- `organizerName`

Android ticket reading is tolerant of Firestore numeric types:

- old tickets may have `purchasePrice` as integer
- newer Worker writes use `doubleValue`
- Android maps either value safely

## Organizer Experience

Organizers do not manually handle payments.

After successful payment:

- ticket appears in attendee/booking data
- participant count is incremented
- event stats are updated
- revenue is added to event stats

The organizer should see the paid attendee through the same ticket/booking screens that read from Firestore.

## Consumer Experience

Before purchase:

- paid private event shows `Buy Ticket`
- free private event shows `Get Free Ticket`
- public reservation shows `I am going`

During checkout:

- app shows `Safepay checkout opened. Complete payment to receive your ticket.`
- button can show `Opening Safepay...` or `Processing...`

After successful payment:

- app shows `Payment confirmed. Your ticket is ready.`
- success dialog appears
- `View Ticket` opens the QR screen
- event detail button changes to `View Ticket`
- ticket appears in the Tickets tab

After failed/incomplete payment:

- app shows that payment is incomplete
- pending checkout is cleared
- next tap starts a new Safepay checkout

## Configuration

Android:

```properties
PAYMENT_API_BASE_URL=https://eventfinder-payments.kh-basitmehmood.workers.dev
```

Cloudflare Worker variables:

```text
SAFEPAY_ENVIRONMENT=sandbox
SAFEPAY_INTENT=CYBERSOURCE
SAFEPAY_ENTRY_MODE=raw
FIREBASE_PROJECT_ID=eventfinder-alpha-1
```

Cloudflare Worker secrets:

```text
SAFEPAY_SECRET_KEY
SAFEPAY_API_KEY
FIREBASE_CLIENT_EMAIL
FIREBASE_PRIVATE_KEY
```

Safepay dashboard:

```text
Webhook URL:
https://eventfinder-payments.kh-basitmehmood.workers.dev/safepay-webhook
```

## Technical Setup Details

### Cloudflare Worker

Worker directory:

```text
workers/payment
```

Main files:

```text
workers/payment/src/index.js
workers/payment/wrangler.toml
workers/payment/package.json
```

Deploy command:

```bash
cd workers/payment
npx wrangler deploy
```

Current Worker URL:

```text
https://eventfinder-payments.kh-basitmehmood.workers.dev
```

The Android app calls this Worker through:

```properties
PAYMENT_API_BASE_URL=https://eventfinder-payments.kh-basitmehmood.workers.dev
```

### Cloudflare Worker Variables

These are non-secret values in `workers/payment/wrangler.toml`:

```toml
[vars]
SAFEPAY_ENVIRONMENT = "sandbox"
SAFEPAY_INTENT = "CYBERSOURCE"
SAFEPAY_ENTRY_MODE = "raw"
SAFEPAY_REDIRECT_URL = "eventfinder://payment/complete"
SAFEPAY_CANCEL_URL = "eventfinder://payment/cancel"
FIREBASE_PROJECT_ID = "eventfinder-alpha-1"
```

Notes:

- `SAFEPAY_ENVIRONMENT=sandbox` means the Worker uses Safepay sandbox APIs.
- `SAFEPAY_INTENT=CYBERSOURCE` is the currently working intent for Safepay hosted card checkout.
- `MPGS` was tested and rejected by Safepay for this configuration with a Google Pay entry-mode error.
- `SAFEPAY_REDIRECT_URL` and `SAFEPAY_CANCEL_URL` are currently kept for reference, but the Worker now generates HTTPS callback URLs first and then redirects back to the app deep link.

### Cloudflare Worker Secrets

These must be stored as Cloudflare Worker secrets, not committed to Git:

```text
SAFEPAY_SECRET_KEY
SAFEPAY_API_KEY
FIREBASE_CLIENT_EMAIL
FIREBASE_PRIVATE_KEY
```

Set them with:

```bash
cd workers/payment
npx wrangler secret put SAFEPAY_SECRET_KEY
npx wrangler secret put SAFEPAY_API_KEY
npx wrangler secret put FIREBASE_CLIENT_EMAIL
npx wrangler secret put FIREBASE_PRIVATE_KEY
```

Important:

- `SAFEPAY_SECRET_KEY` is the Safepay sandbox secret/private key.
- `SAFEPAY_API_KEY` is the Safepay sandbox public/API key.
- `FIREBASE_CLIENT_EMAIL` comes from the Firebase service-account JSON `client_email`.
- `FIREBASE_PRIVATE_KEY` comes from the Firebase service-account JSON `private_key`.
- Do not put any of these values in Android code, `gradle.properties`, Git, or documentation.

### Local Development Secret Files

Local-only files:

```text
workers/payment/.dev.vars
workers/payment/firebase-service-account.json
```

These are for local testing and secret upload only.

They must remain ignored by Git. The service-account JSON includes a live private key and must be treated like a password.

If a Firebase service-account JSON is ever pasted into chat, committed, or shared:

1. Revoke/delete that key in Google Cloud IAM.
2. Generate a new Firebase service-account key.
3. Re-upload the new `FIREBASE_CLIENT_EMAIL` and `FIREBASE_PRIVATE_KEY` secrets to Cloudflare.

### Safepay Keys

Safepay sandbox dashboard provides:

```text
Public key
Secret key
```

Mapping:

```text
Safepay public key -> SAFEPAY_API_KEY
Safepay secret key -> SAFEPAY_SECRET_KEY
```

Where to find them:

- Safepay sandbox dashboard: `https://sandbox.api.getsafepay.com`
- Look for developer/API keys in the sandbox merchant dashboard.
- Copy the public key into Cloudflare as `SAFEPAY_API_KEY`.
- Copy the secret key into Cloudflare as `SAFEPAY_SECRET_KEY`.
- Safepay docs: `https://safepay-docs.netlify.app`
- Safepay API reference: `https://apidocs.getsafepay.com`

The Worker uses:

```http
x-sfpy-merchant-secret: SAFEPAY_SECRET_KEY
```

and sends:

```json
{
  "merchant_api_key": "SAFEPAY_API_KEY",
  "intent": "CYBERSOURCE",
  "mode": "payment",
  "entry_mode": "raw",
  "currency": "PKR",
  "amount": 20000
}
```

Amount is sent to Safepay in minor units:

```text
PKR 200.00 -> 20000
```

### Firebase Service Account

The Worker writes tickets to Firestore through the Firestore REST API using a Firebase service-account JWT.

Where to find/generate it:

- Firebase Console: `https://console.firebase.google.com`
- Project: `eventfinder-alpha-1`
- Go to Project settings -> Service accounts.
- Click `Generate new private key`.
- Download the JSON file locally.
- Use `client_email` as `FIREBASE_CLIENT_EMAIL`.
- Use `private_key` as `FIREBASE_PRIVATE_KEY`.
- Do not commit this JSON file.

Direct project settings URL:

```text
https://console.firebase.google.com/project/eventfinder-alpha-1/settings/serviceaccounts/adminsdk
```

Required service-account fields:

```text
client_email
private_key
project_id
token_uri
```

Only these are needed at runtime:

```text
FIREBASE_CLIENT_EMAIL
FIREBASE_PRIVATE_KEY
FIREBASE_PROJECT_ID
```

The Worker signs a JWT, exchanges it with Google OAuth, then calls Firestore REST endpoints.

This is why Firebase Blaze/Cloud Functions are not required for the current backend.

### Safepay Webhook

Configure this in Safepay sandbox dashboard:

```text
https://eventfinder-payments.kh-basitmehmood.workers.dev/safepay-webhook
```

Where to configure it:

- Safepay sandbox dashboard: `https://sandbox.api.getsafepay.com`
- Open the sandbox merchant/app settings.
- Find webhook/callback settings.
- Add the Worker webhook URL above.

### Cloudflare Account And Worker Secrets

Where to manage the Worker:

- Cloudflare dashboard: `https://dash.cloudflare.com`
- Workers & Pages: `https://dash.cloudflare.com/?to=/:account/workers-and-pages`

CLI login:

```bash
cd workers/payment
npx wrangler login
```

Secrets are normally set through Wrangler:

```bash
npx wrangler secret put SAFEPAY_SECRET_KEY
npx wrangler secret put SAFEPAY_API_KEY
npx wrangler secret put FIREBASE_CLIENT_EMAIL
npx wrangler secret put FIREBASE_PRIVATE_KEY
```

You can also view/manage deployed Worker settings in the Cloudflare dashboard after login.

The webhook stores incoming payloads in:

```text
payment_webhooks
```

The app does not depend only on the webhook. It also confirms payment directly when the user returns from Safepay.

### Safepay Hosted Checkout URL

The Worker builds a Safepay hosted checkout URL with:

```text
environment=sandbox
tbt=<passport-token>
tracker=<safepay-tracker>
source=hosted
redirect_url=https://eventfinder-payments.../payment-complete?checkoutId=...&eventId=...&userId=...
cancel_url=https://eventfinder-payments.../payment-cancel?checkoutId=...&eventId=...&userId=...
```

The HTTPS callback is needed because external browsers handle HTTPS more reliably than direct custom-scheme redirects from hosted payment pages.

The Worker callback then redirects to:

```text
eventfinder://payment/complete?checkoutId=...&eventId=...&userId=...
```

Android handles this deep link in `MainActivity`.

### Android Deep Link

Manifest entry:

```xml
<data
    android:host="payment"
    android:scheme="eventfinder" />
```

`MainActivity` uses:

```xml
android:launchMode="singleTop"
```

This allows Safepay return links to reuse the existing app task instead of starting a separate app session.

### What Is Safe In Android

Safe:

```text
PAYMENT_API_BASE_URL
Safepay checkout URL returned by Worker
checkoutId
eventId
userId
ticketId
```

Not safe:

```text
SAFEPAY_SECRET_KEY
FIREBASE_PRIVATE_KEY
Firebase service-account JSON
Webhook signing secret
```

Only the Worker should hold server-side payment and Firebase admin secrets.

## Testing Notes

Use Safepay sandbox card payment for testing.

Avoid Google Pay in sandbox until Safepay fixes/enables merchant tokenization for the sandbox account. Current observed error:

```text
GPAY_INIT: Failed to decrypt token: cannot verify signature
```

If a payment succeeds but the app cannot show the ticket:

1. Check Worker logs for `checkout.confirm.ticket_issued`.
2. Confirm `tickets/{ticketId}` exists in Firestore.
3. Check Logcat tag `FirestoreTicketDataSource`.
4. Confirm the app user id matches the ticket `userId`.

## Validation Commands

Android:

```bash
./gradlew :app:assembleDebug
```

Worker:

```bash
cd workers/payment
npm run check
npx wrangler deploy
```
