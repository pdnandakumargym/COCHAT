const express = require('express');
const Joi = require('joi');
const Chat = require('../models/Chat');
const Message = require('../models/Message');
const User = require('../models/User');
const ApiError = require('../utils/ApiError');
const asyncHandler = require('../utils/asyncHandler');
const { requireAuth } = require('../middleware/auth');
const { avatarUpload, toPublicUrl } = require('../utils/storage');
const presence = require('./presence');
const { createNotification } = require('./notifications');
const { loadChatOrThrow, isMember } = require('./chats');

const router = express.Router();
router.use(requireAuth);

let io = null;
function attachIO(socketServer) {
  io = socketServer;
}

function requireAdmin(chat, userId) {
  const member = chat.members.find((m) => m.user.toString() === userId.toString());
  if (!member || member.role !== 'admin') throw ApiError.forbidden('Only group admins can do this');
  return member;
}

async function groupInfo(chat) {
  const users = await User.find({ _id: { $in: chat.members.map((m) => m.user) } });
  const usersById = new Map(users.map((u) => [u._id.toString(), u]));
  return {
    id: chat._id,
    type: 'group',
    name: chat.name,
    avatar: chat.avatar,
    createdBy: chat.createdBy,
    createdAt: chat.createdAt,
    members: chat.members.map((m) => {
      const u = usersById.get(m.user.toString());
      // The Android GroupMember model has no default for `fullName` -- if `u`
      // were ever missing (e.g. a member whose account no longer exists) this
      // key would come out `undefined`, JSON.stringify would drop it from the
      // payload entirely, and the client's JSON decoder throws a
      // MissingFieldException with no try/catch around it wherever this
      // object reaches the app uncaught (this is what crashed the app when
      // adding a member -- see GroupInfoScreen.kt). Falling back to
      // placeholders guarantees every key the client requires is present.
      return {
        id: m.user,
        role: m.role,
        fullName: u?.fullName ?? 'Unknown user',
        profilePicture: u?.profilePicture ?? '',
        designation: u?.designation ?? '',
        status: presence.getStatus(m.user.toString()),
      };
    }),
  };
}

async function logSystemMessage(chat, text, event) {
  const message = await Message.create({
    chat: chat._id,
    sender: chat.createdBy,
    type: 'text',
    text,
    systemEvent: event,
    readBy: [],
  });
  // The client decodes `sender` as a full { _id, fullName, profilePicture }
  // object (see Android Message/MessageSender). Without this populate call,
  // `sender` is emitted as a bare ObjectId string, which the app's JSON
  // decoder cannot parse as an object -- that decode throws inside the
  // socket.io event callback (no try/catch there), which crashes the whole
  // app on every other member's device the instant they receive this
  // system message (group created/member added/removed/left).
  await message.populate('sender', 'fullName profilePicture');
  // senderId is required by the Android client's LastMessage model (no
  // default there) -- omitting it here is exactly what crashed the app the
  // instant anyone opened it after a group-created/member-added/removed/left
  // event, since GET /chats decodes every chat's lastMessage on login.
  chat.lastMessage = { text, senderId: chat.createdBy, type: 'text', createdAt: message.createdAt };
  await chat.save();
  io?.to(`chat:${chat._id}`).emit('message:new', { message, chatId: chat._id });
}

const createSchema = Joi.object({
  name: Joi.string().trim().min(1).max(100).required(),
  memberIds: Joi.array().items(Joi.string()).min(1).required(),
});

router.post(
  '/',
  asyncHandler(async (req, res) => {
    const { value, error } = createSchema.validate(req.body, { abortEarly: false });
    if (error) throw ApiError.badRequest('Invalid group data', error.details);

    const uniqueIds = [...new Set(value.memberIds.filter((id) => id !== req.user._id.toString()))];
    const members = [
      { user: req.user._id, role: 'admin' },
      ...uniqueIds.map((id) => ({ user: id, role: 'member' })),
    ];

    const chat = await Chat.create({
      type: 'group',
      name: value.name,
      createdBy: req.user._id,
      members,
    });

    for (const m of members) {
      io?.in(`user:${m.user}`).socketsJoin(`chat:${chat._id}`);
    }
    io?.to(`chat:${chat._id}`).emit('group:updated', { chat: await groupInfo(chat), action: 'created' });

    for (const id of uniqueIds) {
      await createNotification({
        userId: id,
        type: 'group_created',
        title: value.name,
        body: `${req.user.fullName} added you to ${value.name}`,
        chatId: chat._id,
        actorId: req.user._id,
      });
    }

    res.status(201).json({ group: await groupInfo(chat) });
  })
);

router.get(
  '/:id',
  asyncHandler(async (req, res) => {
    const chat = await loadChatOrThrow(req.params.id, req.user._id);
    if (chat.type !== 'group') throw ApiError.badRequest('Not a group chat');
    res.json({ group: await groupInfo(chat) });
  })
);

const updateSchema = Joi.object({
  name: Joi.string().trim().min(1).max(100).optional(),
});

router.patch(
  '/:id',
  avatarUpload.single('avatar'),
  asyncHandler(async (req, res) => {
    const chat = await loadChatOrThrow(req.params.id, req.user._id);
    if (chat.type !== 'group') throw ApiError.badRequest('Not a group chat');
    requireAdmin(chat, req.user._id);

    const { value, error } = updateSchema.validate(req.body);
    if (error) throw ApiError.badRequest('Invalid group update', error.details);

    if (value.name) chat.name = value.name;
    if (req.file) chat.avatar = toPublicUrl(req.file.path);
    await chat.save();

    io?.to(`chat:${chat._id}`).emit('group:updated', { chat: await groupInfo(chat), action: 'info_updated' });
    res.json({ group: await groupInfo(chat) });
  })
);

const membersSchema = Joi.object({ memberIds: Joi.array().items(Joi.string()).min(1).required() });

router.post(
  '/:id/members',
  asyncHandler(async (req, res) => {
    const chat = await loadChatOrThrow(req.params.id, req.user._id);
    if (chat.type !== 'group') throw ApiError.badRequest('Not a group chat');
    requireAdmin(chat, req.user._id);

    const { value, error } = membersSchema.validate(req.body);
    if (error) throw ApiError.badRequest('Invalid members list', error.details);

    const existingIds = new Set(chat.members.map((m) => m.user.toString()));
    const toAdd = value.memberIds.filter((id) => !existingIds.has(id));
    if (toAdd.length === 0) throw ApiError.badRequest('All specified users are already members');

    chat.members.push(...toAdd.map((id) => ({ user: id, role: 'member' })));
    await chat.save();

    for (const id of toAdd) io?.in(`user:${id}`).socketsJoin(`chat:${chat._id}`);

    const addedUsers = await User.find({ _id: { $in: toAdd } });
    const names = addedUsers.map((u) => u.fullName).join(', ');
    await logSystemMessage(chat, `${req.user.fullName} added ${names}`, 'member_added');
    io?.to(`chat:${chat._id}`).emit('group:updated', { chat: await groupInfo(chat), action: 'members_added' });

    for (const id of toAdd) {
      await createNotification({
        userId: id,
        type: 'member_added',
        title: chat.name,
        body: `${req.user.fullName} added you to ${chat.name}`,
        chatId: chat._id,
        actorId: req.user._id,
      });
    }

    res.json({ group: await groupInfo(chat) });
  })
);

async function removeMember(chat, targetUserId, actingUser, isSelfRemoval) {
  if (!isMember(chat, targetUserId)) throw ApiError.notFound('User is not a member of this group');

  const removedUser = await User.findById(targetUserId);
  chat.members = chat.members.filter((m) => m.user.toString() !== targetUserId);
  await chat.save();

  io?.in(`user:${targetUserId}`).socketsLeave(`chat:${chat._id}`);
  await logSystemMessage(
    chat,
    isSelfRemoval
      ? `${removedUser?.fullName} left the group`
      : `${actingUser.fullName} removed ${removedUser?.fullName}`,
    isSelfRemoval ? 'left' : 'member_removed'
  );
  io?.to(`chat:${chat._id}`).emit('group:updated', {
    chat: await groupInfo(chat),
    action: isSelfRemoval ? 'left' : 'members_removed',
  });

  if (!isSelfRemoval) {
    await createNotification({
      userId: targetUserId,
      type: 'member_removed',
      title: chat.name,
      body: `${actingUser.fullName} removed you from ${chat.name}`,
      chatId: chat._id,
      actorId: actingUser._id,
    });
  }
}

router.delete(
  '/:id/members/:userId',
  asyncHandler(async (req, res) => {
    const chat = await loadChatOrThrow(req.params.id, req.user._id);
    if (chat.type !== 'group') throw ApiError.badRequest('Not a group chat');

    const isSelfRemoval = req.params.userId === req.user._id.toString();
    if (!isSelfRemoval) requireAdmin(chat, req.user._id);

    await removeMember(chat, req.params.userId, req.user, isSelfRemoval);
    res.status(204).send();
  })
);

router.post(
  '/:id/leave',
  asyncHandler(async (req, res) => {
    const chat = await loadChatOrThrow(req.params.id, req.user._id);
    if (chat.type !== 'group') throw ApiError.badRequest('Not a group chat');

    await removeMember(chat, req.user._id.toString(), req.user, true);
    res.status(204).send();
  })
);

module.exports = { router, attachIO };
