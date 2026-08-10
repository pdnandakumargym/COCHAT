const { Server } = require('socket.io');
const { verifyAccessToken } = require('../utils/jwt');
const Chat = require('../models/Chat');
const env = require('../config/env');
const presence = require('../modules/presence');

function createSocketServer(httpServer) {
  const io = new Server(httpServer, {
    cors: { origin: env.corsOrigins, credentials: true },
  });

  io.use((socket, next) => {
    try {
      const token = socket.handshake.auth?.token;
      if (!token) return next(new Error('Missing auth token'));
      const payload = verifyAccessToken(token);
      socket.userId = payload.sub;
      next();
    } catch (err) {
      next(new Error('Invalid or expired auth token'));
    }
  });

  io.on('connection', async (socket) => {
    const userId = socket.userId;
    socket.join(`user:${userId}`);

    const chats = await Chat.find({ 'members.user': userId }).select('_id');
    for (const chat of chats) socket.join(`chat:${chat._id}`);

    await presence.markOnline(userId);

    socket.on('presence:activity', () => {
      presence.markActivity(userId);
    });

    socket.on('typing:start', ({ chatId }) => {
      if (chatId) socket.to(`chat:${chatId}`).emit('typing:update', { chatId, userId, isTyping: true });
    });

    socket.on('typing:stop', ({ chatId }) => {
      if (chatId) socket.to(`chat:${chatId}`).emit('typing:update', { chatId, userId, isTyping: false });
    });

    socket.on('disconnect', () => {
      presence.markOffline(userId);
    });
  });

  presence.attachIO(io);
  return io;
}

module.exports = createSocketServer;
