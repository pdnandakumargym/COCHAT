const mongoose = require('mongoose');

const memberSchema = new mongoose.Schema(
  {
    user: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    role: { type: String, enum: ['admin', 'member'], default: 'member' },
    joinedAt: { type: Date, default: Date.now },
    lastReadAt: { type: Date, default: Date.now },
  },
  { _id: false }
);

const chatSchema = new mongoose.Schema(
  {
    type: { type: String, enum: ['private', 'group'], required: true },
    name: { type: String, trim: true }, // group only
    avatar: { type: String, default: '' }, // group only
    createdBy: { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
    members: { type: [memberSchema], default: [] },
    lastMessage: {
      text: String,
      senderId: { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
      type: { type: String, enum: ['text', 'image', 'video', 'file'] },
      createdAt: Date,
    },
  },
  { timestamps: true }
);

chatSchema.index({ 'members.user': 1 });

module.exports = mongoose.model('Chat', chatSchema);
