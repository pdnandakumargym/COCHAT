const admin = require('firebase-admin');

let app = null;
let attempted = false;

/**
 * Lazily initializes the firebase-admin app from the FIREBASE_SERVICE_ACCOUNT
 * env var (the full service-account JSON, minified to one line). Returns
 * null (and logs once) if it's not configured, so push notifications simply
 * no-op instead of crashing the server when the credential isn't set up yet.
 */
function getFirebaseApp() {
  if (app || attempted) return app;
  attempted = true;

  const raw = process.env.FIREBASE_SERVICE_ACCOUNT;
  if (!raw) {
    console.warn('[firebase] FIREBASE_SERVICE_ACCOUNT is not set — push notifications are disabled');
    return null;
  }

  try {
    const serviceAccount = JSON.parse(raw);
    app = admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
    console.log('[firebase] initialized for project', serviceAccount.project_id);
    return app;
  } catch (err) {
    console.error('[firebase] failed to initialize from FIREBASE_SERVICE_ACCOUNT', err);
    return null;
  }
}

module.exports = { getFirebaseApp };
