const CHECKOUT_COLLECTION = "payment_checkouts";
const TICKETS_COLLECTION = "tickets";
const EVENTS_COLLECTION = "events";
const STATS_COLLECTION = "event_stats";

export default {
  async fetch(request, env) {
    try {
      if (request.method === "OPTIONS") return corsResponse(null, 204);

      const url = new URL(request.url);
      if (request.method === "POST" && url.pathname === "/create-ticket-checkout") {
        return corsResponse(await createTicketCheckout(request, env));
      }
      if (request.method === "POST" && url.pathname === "/confirm-ticket-checkout") {
        return corsResponse(await confirmTicketCheckout(request, env));
      }
      if (request.method === "POST" && url.pathname === "/safepay-webhook") {
        return await safepayWebhook(request, env);
      }
      if (request.method === "GET" && url.pathname === "/payment-complete") {
        return appRedirectResponse("complete", url);
      }
      if (request.method === "GET" && url.pathname === "/payment-cancel") {
        return appRedirectResponse("cancel", url);
      }

      return jsonResponse({error: "Not found"}, 404);
    } catch (error) {
      console.error("worker.unhandled_error", {
        message: error.message,
        status: error.status || 500,
        stack: error.stack,
      });
      return corsResponse({error: error.message || "Unexpected error"}, error.status || 500);
    }
  },
};

async function createTicketCheckout(request, env) {
  const input = await request.json();
  const eventId = requireString(input.eventId, "eventId");
  const userId = requireString(input.userId, "userId");
  const userName = requireString(input.userName, "userName");
  const userEmail = optionalString(input.userEmail);
  const amount = requirePositiveNumber(input.amount, "amount");
  const currency = requireString(input.currency, "currency").toUpperCase();

  console.log("checkout.create.request", {
    eventId,
    userId,
    amount,
    currency,
  });

  if (currency !== "PKR") throw httpError("Only PKR payments are supported", 412);

  const accessToken = await getGoogleAccessToken(env);
  const event = await getFirestoreDocument(env, accessToken, EVENTS_COLLECTION, eventId);
  if (!event) throw httpError("Event not found", 404);

  const eventPrice = numberField(event.fields.price) || 0;
  const isFree = booleanField(event.fields.isFree);
  const visibility = stringField(event.fields.visibility) || "PUBLIC";
  const requiresTicket = booleanField(event.fields.requiresTicket);
  const requiresPayment = visibility !== "PUBLIC" && requiresTicket && eventPrice > 0;

  console.log("checkout.create.event_state", {
    eventId,
    eventPrice,
    isFree,
    visibility,
    requiresTicket,
    requiresPayment,
  });

  if (!requiresPayment) throw httpError("This event does not require payment", 412);
  if (Math.round(eventPrice * 100) !== Math.round(amount * 100)) {
    console.error("checkout.create.amount_mismatch", {
      eventId,
      eventPrice,
      requestedAmount: amount,
    });
    throw httpError("Payment amount does not match the event price", 412);
  }

  const checkoutId = crypto.randomUUID();
  const amountInMinorUnits = Math.round(amount * 100);

  const session = await safepayFetch(env, "/order/payments/v3/", {
    method: "POST",
    body: {
      merchant_api_key: env.SAFEPAY_API_KEY,
      intent: env.SAFEPAY_INTENT || "CYBERSOURCE",
      mode: "payment",
      entry_mode: env.SAFEPAY_ENTRY_MODE || "raw",
      currency,
      amount: amountInMinorUnits,
      include_fees: false,
    },
  });

  const tracker = session?.data?.tracker?.token;
  if (!tracker) throw httpError("Safepay did not return a tracker", 502);
  console.log("checkout.create.tracker_created", {eventId, checkoutId, tracker});

  const passport = await safepayFetch(env, "/client/passport/v1/token", {method: "POST", body: {}});
  const tbt = passport?.data;
  if (!tbt) throw httpError("Safepay did not return an auth token", 502);
  console.log("checkout.create.passport_created", {eventId, checkoutId});

  const checkoutUrl = buildSafepayCheckoutUrl(env, {
    tracker,
    tbt,
    checkoutId,
    eventId,
    userId,
    origin: new URL(request.url).origin,
  });
  const checkout = {
    checkoutId,
    provider: "SAFEPAY",
    tracker,
    checkoutUrl,
    eventId,
    eventTitle: stringField(event.fields.title) || input.eventTitle || "",
    eventStartTime: event.fields.startTime || null,
    eventLocation: stringField(event.fields.address) || null,
    organizerId: stringField(event.fields.organizerId) || "",
    organizerName: stringField(event.fields.organizerName) || "",
    userId,
    userName,
    userEmail,
    amount,
    currency,
    amountInMinorUnits,
    status: "PENDING",
    ticketId: null,
    createdAt: nowIso(),
    updatedAt: nowIso(),
  };

  await setFirestoreDocument(env, accessToken, CHECKOUT_COLLECTION, checkoutId, checkout);
  console.log("checkout.create.saved", {eventId, checkoutId, tracker});

  return {
    checkoutId,
    checkoutUrl,
    provider: "SAFEPAY",
    transactionId: tracker,
    amount,
    currency,
  };
}

async function confirmTicketCheckout(request, env) {
  const input = await request.json();
  const checkoutId = requireString(input.checkoutId, "checkoutId");
  console.log("checkout.confirm.request", {checkoutId});
  return confirmCheckoutAndIssueTicket(env, checkoutId);
}

async function safepayWebhook(request, env) {
  if (!(await verifyWebhookSignature(request, env))) {
    return jsonResponse({error: "Invalid signature"}, 401);
  }

  const payload = await request.json();
  const tracker = findValue(payload, "tracker") || findValue(payload, "token");
  const success = findValue(payload, "success");

  console.log("safepay.webhook.received", {
    tracker: tracker || null,
    success: success === true,
  });

  const accessToken = await getGoogleAccessToken(env);
  await addFirestoreDocument(env, accessToken, "payment_webhooks", {
    provider: "SAFEPAY",
    tracker: tracker || null,
    success: success === true,
    payload,
    receivedAt: nowIso(),
  });

  if (tracker && success === true) {
    const checkout = await findCheckoutByTracker(env, accessToken, tracker);
    if (checkout) await confirmCheckoutAndIssueTicket(env, checkout.checkoutId);
  }

  return jsonResponse({ok: true});
}

async function confirmCheckoutAndIssueTicket(env, checkoutId) {
  const accessToken = await getGoogleAccessToken(env);
  const checkoutDoc = await getFirestoreDocument(env, accessToken, CHECKOUT_COLLECTION, checkoutId);
  if (!checkoutDoc) throw httpError("Checkout not found", 404);

  const checkout = documentToObject(checkoutDoc.fields);
  console.log("checkout.confirm.loaded", {
    checkoutId,
    eventId: checkout.eventId,
    status: checkout.status,
    tracker: checkout.tracker,
  });
  if (checkout.status === "PAID" && checkout.ticketId) return receiptResponse(checkout);

  let paymentState = null;
  for (let attempt = 1; attempt <= 4; attempt += 1) {
    const trackerResponse = await safepayFetch(env, `/reporter/api/v1/payments/${encodeURIComponent(checkout.tracker)}`, {
      method: "GET",
    });
    paymentState = getSafepayPaymentState(trackerResponse);
    if (paymentState.paid || attempt === 4) break;
    await sleep(1500);
  }
  const trackerState = paymentState.state;
  console.log("checkout.confirm.reporter_state", {
    checkoutId,
    tracker: checkout.tracker,
    paid: paymentState.paid,
    state: paymentState.state || null,
    matchedKey: paymentState.matchedKey || null,
    candidates: paymentState.candidates,
  });

  if (!paymentState.paid) {
    await patchFirestoreDocument(env, accessToken, CHECKOUT_COLLECTION, checkoutId, {
      status: "PENDING",
      trackerState: trackerState || null,
      updatedAt: nowIso(),
    });
    return {
      checkoutId,
      provider: "SAFEPAY",
      transactionId: checkout.tracker,
      amount: checkout.amount,
      currency: checkout.currency,
      paidAt: Date.now(),
      status: "PENDING",
    };
  }

  const existingTicket = await findTicketByEventAndUser(env, accessToken, checkout.eventId, checkout.userId);
  if (existingTicket) {
    const paidAt = checkout.paidAt || nowIso();
    await patchFirestoreDocument(env, accessToken, CHECKOUT_COLLECTION, checkoutId, {
      ...checkout,
      status: "PAID",
      trackerState,
      ticketId: existingTicket.ticketId,
      paidAt,
      updatedAt: nowIso(),
    });
    console.log("checkout.confirm.existing_ticket", {
      checkoutId,
      eventId: checkout.eventId,
      userId: checkout.userId,
      ticketId: existingTicket.ticketId,
    });
    return receiptResponse({
      ...checkout,
      status: "PAID",
      ticketId: existingTicket.ticketId,
      paidAt,
    });
  }

  const ticketId = crypto.randomUUID();
  const paidAt = nowIso();
  const ticket = {
    id: ticketId,
    ticketId,
    eventId: checkout.eventId,
    eventTitle: checkout.eventTitle,
    eventStartTime: checkout.eventStartTime,
    userId: checkout.userId,
    userName: checkout.userName,
    userEmail: checkout.userEmail || "",
    ticketType: "PAID",
    status: "PURCHASED",
    qrCodeData: `${checkout.eventId}_${checkout.userId}_${ticketId}_${Date.now()}`,
    purchasePrice: {doubleValue: checkout.amount},
    currency: checkout.currency,
    paymentStatus: "PAID",
    paymentProvider: "SAFEPAY",
    paymentTransactionId: checkout.tracker,
    paidAt,
    purchasedAt: paidAt,
    checkedInAt: null,
    checkedInBy: null,
    eventLocation: checkout.eventLocation || null,
    organizerId: checkout.organizerId || "",
    organizerName: checkout.organizerName || "",
  };

  await commitFirestoreWrites(env, accessToken, [
    {update: firestoreDocument(env, TICKETS_COLLECTION, ticketId, ticket)},
    {
      update: firestoreDocument(env, CHECKOUT_COLLECTION, checkoutId, {
        ...checkout,
        status: "PAID",
        trackerState,
        ticketId,
        paidAt,
        updatedAt: paidAt,
      }),
    },
    {
      transform: {
        document: firestoreDocName(env, EVENTS_COLLECTION, checkout.eventId),
        fieldTransforms: [
          {fieldPath: "currentParticipantCount", increment: {integerValue: "1"}},
        ],
      },
    },
  ]);

  const statsRef = firestoreDocName(env, STATS_COLLECTION, checkout.eventId);
  const statsDoc = await getFirestoreDocument(env, accessToken, STATS_COLLECTION, checkout.eventId);
  const currentStats = statsDoc ? documentToObject(statsDoc.fields) : null;
  const nextStats = currentStats ? {
    ...currentStats,
    totalTickets: Number(currentStats.totalTickets || 0) + 1,
    totalRevenue: Number(currentStats.totalRevenue || 0) + checkout.amount,
    lastUpdated: paidAt,
  } : {
    eventId: checkout.eventId,
    totalTickets: 1,
    checkedInCount: 0,
    reservedCount: 0,
    cancelledCount: 0,
    totalRevenue: checkout.amount,
    currency: checkout.currency,
    lastUpdated: paidAt,
  };
  await setFirestoreDocument(env, accessToken, STATS_COLLECTION, checkout.eventId, nextStats);
  console.log("checkout.confirm.ticket_issued", {
    checkoutId,
    eventId: checkout.eventId,
    ticketId,
    amount: checkout.amount,
    currency: checkout.currency,
  });

  return receiptResponse({...checkout, status: "PAID", ticketId, paidAt});
}

async function safepayFetch(env, path, options) {
  console.log("safepay.request", {path, method: options.method});
  const response = await fetch(`${safepayHost(env)}${path}`, {
    method: options.method,
    headers: {
      "Accept": "application/json",
      "Content-Type": "application/json",
      "x-sfpy-merchant-secret": env.SAFEPAY_SECRET_KEY,
    },
    body: options.body ? JSON.stringify(options.body) : undefined,
  });
  const body = await response.json().catch(() => ({}));
  console.log("safepay.response", {
    path,
    status: response.status,
    ok: response.ok,
    body: response.ok ? summarizeSafepayBody(body) : body,
  });
  if (!response.ok) throw httpError(`Safepay request failed: ${JSON.stringify(body)}`, response.status);
  return body;
}

function buildSafepayCheckoutUrl(env, params) {
  const environment = env.SAFEPAY_ENVIRONMENT || "sandbox";
  const base = environment === "production" ?
    "https://getsafepay.com/embedded/" :
    "https://sandbox.api.getsafepay.com/embedded/";
  const url = new URL(base);
  url.searchParams.set("environment", environment);
  url.searchParams.set("tbt", params.tbt);
  url.searchParams.set("tracker", params.tracker);
  url.searchParams.set("source", "hosted");
  const callbackParams = new URLSearchParams({
    checkoutId: params.checkoutId,
    eventId: params.eventId,
    userId: params.userId,
  });
  url.searchParams.set("redirect_url", `${params.origin}/payment-complete?${callbackParams}`);
  url.searchParams.set("cancel_url", `${params.origin}/payment-cancel?${callbackParams}`);
  return url.toString();
}

function appRedirectResponse(outcome, url) {
  const appUrl = new URL(`eventfinder://payment/${outcome}`);
  for (const [key, value] of url.searchParams.entries()) {
    appUrl.searchParams.set(key, value);
  }

  return new Response(
    `<!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1">` +
      `<meta http-equiv="refresh" content="0;url=${escapeHtml(appUrl.toString())}"></head>` +
      `<body><p>Returning to EventFinder...</p>` +
      `<p><a href="${escapeHtml(appUrl.toString())}">Open EventFinder</a></p>` +
      `</body></html>`,
    {
      status: 200,
      headers: {
        "Content-Type": "text/html; charset=utf-8",
        "Cache-Control": "no-store",
      },
    },
  );
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/"/g, "&quot;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

function safepayHost(env) {
  return (env.SAFEPAY_ENVIRONMENT || "sandbox") === "production" ?
    "https://api.getsafepay.com" :
    "https://sandbox.api.getsafepay.com";
}

function getSafepayPaymentState(response) {
  const candidates = [];
  collectStatusCandidates(response, candidates);

  const paidValues = new Set([
    "TRACKER_ENDED",
    "PAID",
    "SUCCESS",
    "SUCCEEDED",
    "APPROVED",
    "AUTHORIZED",
    "CAPTURED",
    "COMPLETED",
    "COMPLETE",
  ]);

  const paidCandidate = candidates.find((candidate) =>
    paidValues.has(String(candidate.value).toUpperCase()),
  );

  return {
    paid: Boolean(paidCandidate),
    state: paidCandidate?.value || candidates[0]?.value || null,
    matchedKey: paidCandidate?.key || candidates[0]?.key || null,
    candidates: candidates.slice(0, 12),
  };
}

function collectStatusCandidates(input, output, path = "") {
  if (!input || typeof input !== "object") return;

  for (const [key, value] of Object.entries(input)) {
    const nextPath = path ? `${path}.${key}` : key;
    const normalizedKey = key.toLowerCase();
    if (
      typeof value === "string" &&
      (
        normalizedKey.includes("state") ||
        normalizedKey.includes("status") ||
        normalizedKey.includes("result")
      )
    ) {
      output.push({key: nextPath, value});
    } else if (typeof value === "boolean" && normalizedKey.includes("success")) {
      output.push({key: nextPath, value: value ? "SUCCESS" : "FAILED"});
    } else if (value && typeof value === "object") {
      collectStatusCandidates(value, output, nextPath);
    }
  }
}

function summarizeSafepayBody(body) {
  return {
    hasData: Boolean(body?.data),
    tracker: body?.data?.tracker?.token || body?.data?.tracker?.state || null,
    keys: body && typeof body === "object" ? Object.keys(body) : [],
  };
}

async function getGoogleAccessToken(env) {
  const now = Math.floor(Date.now() / 1000);
  const claim = {
    iss: env.FIREBASE_CLIENT_EMAIL,
    scope: "https://www.googleapis.com/auth/datastore",
    aud: "https://oauth2.googleapis.com/token",
    exp: now + 3600,
    iat: now,
  };
  const assertion = await signJwt(env.FIREBASE_PRIVATE_KEY, claim);
  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: {"Content-Type": "application/x-www-form-urlencoded"},
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  const body = await response.json();
  if (!response.ok) throw httpError(`Google auth failed: ${JSON.stringify(body)}`, response.status);
  return body.access_token;
}

async function signJwt(privateKeyPem, payload) {
  const header = {alg: "RS256", typ: "JWT"};
  const encodedHeader = base64url(JSON.stringify(header));
  const encodedPayload = base64url(JSON.stringify(payload));
  const data = `${encodedHeader}.${encodedPayload}`;
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToArrayBuffer(privateKeyPem),
    {name: "RSASSA-PKCS1-v1_5", hash: "SHA-256"},
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(data));
  return `${data}.${base64url(signature)}`;
}

function pemToArrayBuffer(pem) {
  const normalized = pem.replace(/\\n/g, "\n");
  const base64 = normalized
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s/g, "");
  return Uint8Array.from(atob(base64), (char) => char.charCodeAt(0));
}

async function getFirestoreDocument(env, accessToken, collection, id) {
  const response = await fetch(firestoreDocUrl(env, collection, id), {
    headers: {Authorization: `Bearer ${accessToken}`},
  });
  if (response.status === 404) return null;
  const body = await response.json();
  if (!response.ok) throw httpError(`Firestore read failed: ${JSON.stringify(body)}`, response.status);
  return body;
}

async function setFirestoreDocument(env, accessToken, collection, id, value) {
  const response = await fetch(firestoreDocUrl(env, collection, id), {
    method: "PATCH",
    headers: firestoreHeaders(accessToken),
    body: JSON.stringify({fields: objectToFields(value)}),
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw httpError(`Firestore write failed: ${JSON.stringify(body)}`, response.status);
  return body;
}

async function patchFirestoreDocument(env, accessToken, collection, id, value) {
  return setFirestoreDocument(env, accessToken, collection, id, value);
}

async function addFirestoreDocument(env, accessToken, collection, value) {
  const id = crypto.randomUUID();
  return setFirestoreDocument(env, accessToken, collection, id, value);
}

async function commitFirestoreWrites(env, accessToken, writes) {
  const response = await fetch(
    `https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents:commit`,
    {
      method: "POST",
      headers: firestoreHeaders(accessToken),
      body: JSON.stringify({writes}),
    },
  );
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw httpError(`Firestore commit failed: ${JSON.stringify(body)}`, response.status);
  return body;
}

async function findCheckoutByTracker(env, accessToken, tracker) {
  const response = await fetch(
    `https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents:runQuery`,
    {
      method: "POST",
      headers: firestoreHeaders(accessToken),
      body: JSON.stringify({
        structuredQuery: {
          from: [{collectionId: CHECKOUT_COLLECTION}],
          where: {
            fieldFilter: {
              field: {fieldPath: "tracker"},
              op: "EQUAL",
              value: {stringValue: tracker},
            },
          },
          limit: 1,
        },
      }),
    },
  );
  const rows = await response.json();
  if (!response.ok) throw httpError(`Firestore query failed: ${JSON.stringify(rows)}`, response.status);
  const doc = rows.find((row) => row.document)?.document;
  return doc ? documentToObject(doc.fields) : null;
}

async function findTicketByEventAndUser(env, accessToken, eventId, userId) {
  const response = await fetch(
    `https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents:runQuery`,
    {
      method: "POST",
      headers: firestoreHeaders(accessToken),
      body: JSON.stringify({
        structuredQuery: {
          from: [{collectionId: TICKETS_COLLECTION}],
          where: {
            compositeFilter: {
              op: "AND",
              filters: [
                {
                  fieldFilter: {
                    field: {fieldPath: "eventId"},
                    op: "EQUAL",
                    value: {stringValue: eventId},
                  },
                },
                {
                  fieldFilter: {
                    field: {fieldPath: "userId"},
                    op: "EQUAL",
                    value: {stringValue: userId},
                  },
                },
              ],
            },
          },
          limit: 1,
        },
      }),
    },
  );
  const rows = await response.json();
  if (!response.ok) throw httpError(`Firestore ticket query failed: ${JSON.stringify(rows)}`, response.status);
  const doc = rows.find((row) => row.document)?.document;
  return doc ? documentToObject(doc.fields) : null;
}

function firestoreHeaders(accessToken) {
  return {
    Authorization: `Bearer ${accessToken}`,
    "Content-Type": "application/json",
  };
}

function firestoreDocUrl(env, collection, id) {
  return `https://firestore.googleapis.com/v1/${firestoreDocName(env, collection, id)}`;
}

function firestoreDocName(env, collection, id) {
  return `projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents/${collection}/${id}`;
}

function firestoreDocument(env, collection, id, value) {
  return {name: firestoreDocName(env, collection, id), fields: objectToFields(value)};
}

function objectToFields(value) {
  return Object.fromEntries(Object.entries(value).map(([key, fieldValue]) => [key, toFirestoreValue(fieldValue)]));
}

function toFirestoreValue(value) {
  if (value === null || value === undefined) return {nullValue: null};
  if (typeof value === "string") {
    if (/^\d{4}-\d{2}-\d{2}T/.test(value)) return {timestampValue: value};
    return {stringValue: value};
  }
  if (typeof value === "boolean") return {booleanValue: value};
  if (typeof value === "number") {
    return Number.isInteger(value) ? {integerValue: String(value)} : {doubleValue: value};
  }
  if (Array.isArray(value)) return {arrayValue: {values: value.map(toFirestoreValue)}};
  if (
    typeof value === "object" &&
    (value.stringValue || value.timestampValue || value.integerValue || value.doubleValue)
  ) return value;
  if (typeof value === "object") return {mapValue: {fields: objectToFields(value)}};
  return {stringValue: String(value)};
}

function documentToObject(fields = {}) {
  return Object.fromEntries(Object.entries(fields).map(([key, value]) => [key, fromFirestoreValue(value)]));
}

function fromFirestoreValue(value) {
  if (!value) return null;
  if ("stringValue" in value) return value.stringValue;
  if ("booleanValue" in value) return value.booleanValue;
  if ("integerValue" in value) return Number(value.integerValue);
  if ("doubleValue" in value) return Number(value.doubleValue);
  if ("timestampValue" in value) return value.timestampValue;
  if ("nullValue" in value) return null;
  if ("mapValue" in value) return documentToObject(value.mapValue.fields || {});
  if ("arrayValue" in value) return (value.arrayValue.values || []).map(fromFirestoreValue);
  return null;
}

function stringField(fields, key) {
  const value = key ? fields?.[key] : fields;
  return value?.stringValue || null;
}

function numberField(field) {
  if (!field) return null;
  if ("doubleValue" in field) return Number(field.doubleValue);
  if ("integerValue" in field) return Number(field.integerValue);
  return null;
}

function booleanField(field) {
  return field?.booleanValue === true;
}

async function verifyWebhookSignature(request, env) {
  const hmacKey = env.SAFEPAY_WEBHOOK_HMAC_KEY;
  if (!hmacKey) return true;
  const signature = request.headers.get("x-sfpy-signature") ||
    request.headers.get("x-safepay-signature") ||
    request.headers.get("signature");
  if (!signature) return false;
  const raw = await request.clone().arrayBuffer();
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(hmacKey),
    {name: "HMAC", hash: "SHA-256"},
    false,
    ["sign"],
  );
  const signed = await crypto.subtle.sign("HMAC", key, raw);
  return base64url(signed) === signature || hex(signed) === signature;
}

function receiptResponse(checkout) {
  return {
    checkoutId: checkout.checkoutId,
    provider: "SAFEPAY",
    transactionId: checkout.tracker,
    ticketId: checkout.ticketId || null,
    amount: checkout.amount,
    currency: checkout.currency,
    paidAt: checkout.paidAt ? Date.parse(checkout.paidAt) : Date.now(),
    status: "PAID",
  };
}

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type",
    },
  });
}

function corsResponse(body, status = 200) {
  return jsonResponse(body || {}, status);
}

function requireString(value, field) {
  if (typeof value !== "string" || !value.trim()) throw httpError(`Missing ${field}`, 400);
  return value.trim();
}

function optionalString(value) {
  return typeof value === "string" ? value : "";
}

function requirePositiveNumber(value, field) {
  const number = Number(value);
  if (!Number.isFinite(number) || number <= 0) throw httpError(`Invalid ${field}`, 400);
  return number;
}

function httpError(message, status) {
  const error = new Error(message);
  error.status = status;
  return error;
}

function nowIso() {
  return new Date().toISOString();
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function base64url(value) {
  const bytes = typeof value === "string" ? new TextEncoder().encode(value) : new Uint8Array(value);
  let binary = "";
  bytes.forEach((byte) => binary += String.fromCharCode(byte));
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

function hex(buffer) {
  return [...new Uint8Array(buffer)].map((byte) => byte.toString(16).padStart(2, "0")).join("");
}

function findValue(input, key) {
  if (!input || typeof input !== "object") return undefined;
  if (Object.prototype.hasOwnProperty.call(input, key)) return input[key];
  for (const value of Object.values(input)) {
    const found = findValue(value, key);
    if (found !== undefined) return found;
  }
  return undefined;
}
