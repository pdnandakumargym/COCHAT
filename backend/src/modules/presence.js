const env = require('../config/env');
const User = require('../models/User');

/**
 * In-memory presence tracker. Keyed by userId string.
 * socketCount tracks concurrent connections (multi-tab/multi-device);
 * a user only goes offline once their last socket disconnects.
 */
const presenceState = new Map(); // userId -> { socketCount, status, awayTimer }

let io = null;
function attachIO(socketServer) {
  io = socketServer;
}

function broadcast(userId, status) {
  if (io) io.emit('presence:update', { userId, status });
}

function clearAwayTimer(entry) {
  if (entry.awayTimer) {
    clearTimeout(entry.awayTimer);
    entry.awayTimer = null;
  }
}

function scheduleAway(userId) {
  const entry = presenceState.get(userId);
  if (!entry) return;
  clearAwayTimer(entry);
  entry.awayTimer = setTimeout(async () => {
    const current = presenceState.get(userId);
    if (current && current.socketCount > 0 && current.status === 'online') {
      current.status = 'away';
      await User.findByIdAndUpdate(userId, { status: 'away' }).catch(() => {});
      broadcast(userId, 'away');
    }
  }, env.presenceAwayMinutes * 60 * 1000);
}

async function markOnline(userId) {
  const entry = presenceState.get(userId) || { socketCount: 0, status: 'offline', awayTimer: null };
  entry.socketCount += 1;
  entry.status = 'online';
  presenceState.set(userId, entry);
  scheduleAway(userId);
  await User.findByIdAndUpdate(userId, { status: 'online', lastSeen: new Date() }).catch(() => {});
  broadcast(userId, 'online');
}

async function markActivity(userId) {
  const entry = presenceState.get(userId);
  if (!entry) return;
  if (entry.status !== 'online') {
    entry.status = 'online';
    await User.findByIdAndUpdate(userId, { status: 'online' }).catch(() => {});
    broadcast(userId, 'online');
  }
  scheduleAway(userId);
}

async function markOffline(userId) {
  const entry = presenceState.get(userId);
  if (!entry) return;
  entry.socketCount = Math.max(0, entry.socketCount - 1);
  if (entry.socketCount === 0) {
    clearAwayTimer(entry);
    entry.status = 'offline';
    const lastSeen = new Date();
    await User.findByIdAndUpdate(userId, { status: 'offline', lastSeen }).catch(() => {});
    broadcast(userId, 'offline');
    presenceState.delete(userId);
  } else {
    presenceState.set(userId, entry);
  }
}

function getStatus(userId) {
  const entry = presenceState.get(userId);
  return entry ? entry.status : 'offline';
}

function getStatuses(userIds) {
  const map = {};
  for (const id of userIds) map[id] = getStatus(id);
  return map;
}

module.exports = { attachIO, markOnline, markActivity, markOffline, getStatus, getStatuses };
