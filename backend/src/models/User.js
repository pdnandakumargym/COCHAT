const mongoose = require('mongoose');

const refreshTokenSchema = new mongoose.Schema(
  {
    tokenId: { type: String, required: true },
    expiresAt: { type: Date, required: true },
  },
  { _id: false }
);

const userSchema = new mongoose.Schema(
  {
    fullName: { type: String, required: true, trim: true },
    email: {
      type: String,
      trim: true,
      lowercase: true,
      unique: true,
      sparse: true,
      index: true,
    },
    mobile: {
      type: String,
      trim: true,
      unique: true,
      sparse: true,
      index: true,
    },
    passwordHash: { type: String, required: true },
    designation: { type: String, trim: true, default: '' },
    profilePicture: { type: String, default: '' },
    status: {
      type: String,
      enum: ['online', 'away', 'offline'],
      default: 'offline',
    },
    lastSeen: { type: Date, default: Date.now },
    refreshTokens: { type: [refreshTokenSchema], default: [] },
    // FCM registration tokens for every device this user is signed into.
    // A device is added on login/app-start and removed on logout or once
    // FCM reports it as dead (see modules/push.js).
    pushTokens: { type: [String], default: [] },
  },
  { timestamps: true }
);

userSchema.index({ fullName: 'text', designation: 'text' });

userSchema.methods.toPublicJSON = function toPublicJSON() {
  return {
    id: this._id,
    fullName: this.fullName,
    email: this.email || null,
    mobile: this.mobile || null,
    designation: this.designation || '',
    profilePicture: this.profilePicture || '',
    status: this.status,
    lastSeen: this.lastSeen,
  };
};

module.exports = mongoose.model('User', userSchema);
