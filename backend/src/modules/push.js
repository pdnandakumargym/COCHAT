const admin = require('firebase-admin');
const { getFirebaseApp } = require('../config/firebase');
const User = require('../models/User');

/**
 * Sends a data-only push to every device a user is registered on. Data-only
 * (no top-level `notification` field) is deliberate: it guarantees
 * onMessageReceived() runs on the Android client in every app state
 * (foreground, background, killed), so the client builds the notification
 * itself with the right channel and a deep link -- an FCM `notification`
 * payload instead gets auto-displayed by the OS when the app isn't in the
 * foreground, bypassing app code (and our deep-link data) entirely.
 */
async function sendPushToUser(userId, { title, body, data = {} }) {
  const app = getFirebaseApp();
  if (!app) return;

  const user = await User.findById(userId).select('pushTokens');
  if (!user || user.pushTokens.length === 0) return;

  const stringData = {};
  for (const [key, value] of Object.entries(data)) {
    stringData[key] = value === undefined || value === null ? '' : String(value);
  }

  const message = {
    tokens: user.pushTokens,
    data: { title, body, ...stringData },
    android: { priority: 'high' },
  };

  let response;
  try {
    response = await admin.messaging().sendEachForMulticast(message);
  } catch (err) {
    console.error('[push] sendEachForMulticast failed', err);
    return;
  }

  const deadTokens = [];
  response.responses.forEach((r, idx) => {
    if (r.success) return;
    const code = r.error?.code;
    if (code === 'messaging/registration-token-not-registered' || code === 'messaging/invalid-registration-token') {
      deadTokens.push(user.pushTokens[idx]);
    }
  });

  if (deadTokens.length > 0) {
    await User.updateOne({ _id: userId }, { $pull: { pushTokens: { $in: deadTokens } } });
  }
}

module.exports = { sendPushToUser };
