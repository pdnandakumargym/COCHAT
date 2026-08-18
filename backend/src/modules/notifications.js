const express = require('express');
const Notification = require('../models/Notification');
const asyncHandler = require('../utils/asyncHandler');
const { requireAuth } = require('../middleware/auth');
const ApiError = require('../utils/ApiError');
const { sendPushToUser } = require('./push');

const router = express.Router();
router.use(requireAuth);

let io = null;
function attachIO(socketServer) {
  io = socketServer;
}

async function createNotification({ userId, type, title, body, chatId, actorId }) {
  const notification = await Notification.create({
    user: userId,
    type,
    title,
    body,
    chat: chatId,
    actor: actorId,
  });
  if (io) io.to(`user:${userId}`).emit('notification:new', { notification });

  // Fire-and-forget: push delivery shouldn't block or fail the request that
  // triggered this notification (sending a message, adding a member, etc).
  sendPushToUser(userId, {
    title,
    body,
    data: {
      type,
      chatId: chatId ? chatId.toString() : '',
      notificationId: notification._id.toString(),
    },
  }).catch((err) => console.error('[push] failed to deliver', err));

  return notification;
}

router.get(
  '/',
  asyncHandler(async (req, res) => {
    const notifications = await Notification.find({ user: req.user._id })
      .sort({ createdAt: -1 })
      .limit(200)
      .populate('actor', 'fullName profilePicture');
    res.json({ notifications });
  })
);

router.post(
  '/:id/read',
  asyncHandler(async (req, res) => {
    const notification = await Notification.findOne({ _id: req.params.id, user: req.user._id });
    if (!notification) throw ApiError.notFound('Notification not found');
    notification.read = true;
    await notification.save();
    res.json({ notification });
  })
);

router.post(
  '/read-all',
  asyncHandler(async (req, res) => {
    await Notification.updateMany({ user: req.user._id, read: false }, { $set: { read: true } });
    res.status(204).send();
  })
);

module.exports = { router, createNotification, attachIO };
