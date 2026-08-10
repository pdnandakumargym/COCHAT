const mongoose = require('mongoose');

const notificationSchema = new mongoose.Schema(
  {
    user: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true, index: true },
    type: {
      type: String,
      enum: [
        'private_message',
        'group_message',
        'group_created',
        'member_added',
        'member_removed',
        'media_shared',
      ],
      required: true,
    },
    title: { type: String, required: true },
    body: { type: String, default: '' },
    chat: { type: mongoose.Schema.Types.ObjectId, ref: 'Chat' },
    actor: { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
    read: { type: Boolean, default: false },
  },
  { timestamps: true }
);

notificationSchema.index({ user: 1, createdAt: -1 });

module.exports = mongoose.model('Notification', notificationSchema);
