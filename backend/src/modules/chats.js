const express = require('express');
const Joi = require('joi');
const mongoose = require('mongoose');
const Chat = require('../models/Chat');
const Message = require('../models/Message');
const User = require('../models/User');
const ApiError = require('../utils/ApiError');
const asyncHandler = require('../utils/asyncHandler');
const { requireAuth } = require('../middleware/auth');
const { mediaUpload, fileTypeFromMime, toPublicUrl } = require('../utils/storage');
const presence = require('./presence');
const { createNotification } = require('./notifications');

const router = express.Router();
router.use(requireAuth);

let io = null;
function attachIO(socketServer) {
  io = socketServer;
}

function isMember(chat, userId) {
  return chat.members.some((m) => m.user.toString() === userId.toString());
}

function memberOf(chat, userId) {
  return chat.members.find((m) => m.user.toString() === userId.toString());
}

async function loadChatOrThrow(chatId, userId) {
  if (!mongoose.isValidObjectId(chatId)) throw ApiError.notFound('Chat not found');
  const chat = await Chat.findById(chatId);
  if (!chat) throw ApiError.notFound('Chat not found');
  if (!isMember(chat, userId)) throw ApiError.forbidden('You are not a member of this chat');
  return chat;
}

async function chatSummary(chat, currentUserId) {
  const unreadCount = await Message.countDocuments({
    chat: chat._id,
    sender: { $ne: currentUserId },
    createdAt: { $gt: memberOf(chat, currentUserId).lastReadAt },
  });

  const summary = {
    id: chat._id,
    type: chat.type,
    // Also guard against already-stored chats whose lastMessage predates
    // the senderId fix above (e.g. system messages logged before this
    // change) -- the Android client's LastMessage.senderId has no default,
    // so an object missing it must never reach the client; null is safe
    // (renders as no preview) where a crash is not.
    lastMessage: chat.lastMessage?.createdAt && chat.lastMessage?.senderId ? chat.lastMessage : null,
    unreadCount,
    updatedAt: chat.updatedAt,
  };

  if (chat.type === 'group') {
    summary.name = chat.name;
    summary.avatar = chat.avatar;
    summary.memberCount = chat.members.length;
  } else {
    const otherMember = chat.members.find((m) => m.user.toString() !== currentUserId.toString());
    const otherUser = otherMember ? await User.findById(otherMember.user) : null;
    if (otherUser) {
      summary.peer = {
        id: otherUser._id,
        fullName: otherUser.fullName,
        profilePicture: otherUser.profilePicture,
        designation: otherUser.designation,
        status: presence.getStatus(otherUser._id.toString()),
      };
    }
  }
  return summary;
}

router.get(
  '/',
  asyncHandler(async (req, res) => {
    const chats = await Chat.find({ 'members.user': req.user._id }).sort({ updatedAt: -1 });
    const summaries = await Promise.all(chats.map((c) => chatSummary(c, req.user._id)));
    res.json({ chats: summaries });
  })
);

const privateSchema = Joi.object({ userId: Joi.string().required() });

router.post(
  '/private',
  asyncHandler(async (req, res) => {
    const { value, error } = privateSchema.validate(req.body);
    if (error) throw ApiError.badRequest('userId is required', error.details);
    if (value.userId === req.user._id.toString()) throw ApiError.badRequest('Cannot start a chat with yourself');

    const peer = await User.findById(value.userId);
    if (!peer) throw ApiError.notFound('User not found');

    let chat = await Chat.findOne({
      type: 'private',
      $and: [{ 'members.user': req.user._id }, { 'members.user': peer._id }],
    });

    if (!chat) {
      chat = await Chat.create({
        type: 'private',
        createdBy: req.user._id,
        members: [{ user: req.user._id }, { user: peer._id }],
      });
      io?.to(`user:${peer._id}`).socketsJoin(`chat:${chat._id}`);
      io?.to(`user:${req.user._id}`).socketsJoin(`chat:${chat._id}`);
    }

    res.status(201).json({ chat: await chatSummary(chat, req.user._id) });
  })
);

router.get(
  '/:chatId/messages',
  asyncHandler(async (req, res) => {
    const chat = await loadChatOrThrow(req.params.chatId, req.user._id);
    const limit = Math.min(parseInt(req.query.limit, 10) || 30, 100);
    const filter = { chat: chat._id };
    if (req.query.before && mongoose.isValidObjectId(req.query.before)) {
      const beforeMsg = await Message.findById(req.query.before);
      if (beforeMsg) filter.createdAt = { $lt: beforeMsg.createdAt };
    }
    const messages = await Message.find(filter)
      .sort({ createdAt: -1 })
      .limit(limit)
      .populate('sender', 'fullName profilePicture');
    res.json({ messages: messages.reverse() });
  })
);

const sendSchema = Joi.object({ text: Joi.string().trim().max(5000).allow('').optional() });

router.post(
  '/:chatId/messages',
  mediaUpload.single('file'),
  asyncHandler(async (req, res) => {
    const chat = await loadChatOrThrow(req.params.chatId, req.user._id);
    const { value, error } = sendSchema.validate(req.body);
    if (error) throw ApiError.badRequest('Invalid message', error.details);

    if (!value.text && !req.file) throw ApiError.badRequest('Message must include text or a file');

    const messageDoc = {
      chat: chat._id,
      sender: req.user._id,
      text: value.text || '',
      readBy: [req.user._id],
    };

    if (req.file) {
      messageDoc.type = fileTypeFromMime(req.file.mimetype);
      messageDoc.attachment = {
        url: toPublicUrl(req.file.path),
        fileName: req.file.originalname,
        mimeType: req.file.mimetype,
        size: req.file.size,
      };
    } else {
      messageDoc.type = 'text';
    }

    const message = await Message.create(messageDoc);
    await message.populate('sender', 'fullName profilePicture');

    chat.lastMessage = {
      text: message.type === 'text' ? message.text : `[${message.type}] ${req.file?.originalname || ''}`.trim(),
      senderId: req.user._id,
      type: message.type,
      createdAt: message.createdAt,
    };
    memberOf(chat, req.user._id).lastReadAt = message.createdAt;
    await chat.save();

    io?.to(`chat:${chat._id}`).emit('message:new', { message, chatId: chat._id });
    // unread counts are per-member, so chat:updated is sent as an individual summary per recipient
    for (const m of chat.members) {
      io?.to(`user:${m.user}`).emit('chat:updated', { chat: await chatSummary(chat, m.user) });
    }

    const otherMembers = chat.members.filter((m) => m.user.toString() !== req.user._id.toString());
    for (const m of otherMembers) {
      await createNotification({
        userId: m.user,
        type: chat.type === 'group' ? 'group_message' : 'private_message',
        title: chat.type === 'group' ? chat.name : req.user.fullName,
        body: message.type === 'text' ? message.text : `Sent a ${message.type}`,
        chatId: chat._id,
        actorId: req.user._id,
      });
    }

    res.status(201).json({ message });
  })
);

router.post(
  '/:chatId/read',
  asyncHandler(async (req, res) => {
    const chat = await loadChatOrThrow(req.params.chatId, req.user._id);
    memberOf(chat, req.user._id).lastReadAt = new Date();
    await chat.save();
    io?.to(`chat:${chat._id}`).emit('chat:read', { chatId: chat._id, userId: req.user._id });
    res.status(204).send();
  })
);

module.exports = { router, attachIO, chatSummary, loadChatOrThrow, isMember };
