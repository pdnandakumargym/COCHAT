const mongoose = require('mongoose');

const messageSchema = new mongoose.Schema(
  {
    chat: { type: mongoose.Schema.Types.ObjectId, ref: 'Chat', required: true, index: true },
    sender: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    type: { type: String, enum: ['text', 'image', 'video', 'file'], default: 'text' },
    text: { type: String, default: '' },
    attachment: {
      url: String,
      fileName: String,
      mimeType: String,
      size: Number,
    },
    readBy: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }],
    systemEvent: { type: String, default: null }, // e.g. 'member_added', 'member_removed', 'group_created', 'left'
  },
  { timestamps: true }
);

messageSchema.index({ chat: 1, createdAt: -1 });

module.exports = mongoose.model('Message', messageSchema);
