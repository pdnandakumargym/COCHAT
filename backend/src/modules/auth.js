const express = require('express');
const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const Joi = require('joi');
const rateLimit = require('express-rate-limit');
const User = require('../models/User');
const ApiError = require('../utils/ApiError');
const asyncHandler = require('../utils/asyncHandler');
const { signAccessToken, signRefreshToken, verifyRefreshToken } = require('../utils/jwt');
const { requireAuth } = require('../middleware/auth');
const env = require('../config/env');

const router = express.Router();

const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 30,
  standardHeaders: true,
  legacyHeaders: false,
  message: { message: 'Too many attempts, please try again later' },
});

const registerSchema = Joi.object({
  fullName: Joi.string().trim().min(2).max(100).required(),
  email: Joi.string().trim().lowercase().email({ tlds: { allow: false } }).optional(),
  mobile: Joi.string()
    .trim()
    .pattern(/^\+?[0-9]{7,15}$/)
    .optional(),
  password: Joi.string().min(6).max(128).required(),
  designation: Joi.string().trim().max(100).allow('').optional(),
})
  .or('email', 'mobile')
  .required();

const loginSchema = Joi.object({
  identifier: Joi.string().trim().required(), // email or mobile
  password: Joi.string().required(),
});

const refreshSchema = Joi.object({
  refreshToken: Joi.string().required(),
});

function expiresToDate(expiresIn) {
  const match = /^(\d+)([smhd])$/.exec(expiresIn);
  const now = Date.now();
  if (!match) return new Date(now + 7 * 24 * 60 * 60 * 1000);
  const value = parseInt(match[1], 10);
  const unitMs = { s: 1000, m: 60 * 1000, h: 60 * 60 * 1000, d: 24 * 60 * 60 * 1000 }[match[2]];
  return new Date(now + value * unitMs);
}

async function issueTokenPair(user) {
  const accessToken = signAccessToken(user);
  const tokenId = crypto.randomBytes(16).toString('hex');
  const refreshToken = signRefreshToken(user, tokenId);
  user.refreshTokens.push({ tokenId, expiresAt: expiresToDate(env.jwtRefreshExpires) });
  // keep at most 10 concurrent device sessions
  if (user.refreshTokens.length > 10) user.refreshTokens.splice(0, user.refreshTokens.length - 10);
  await user.save();
  return { accessToken, refreshToken };
}

router.post(
  '/register',
  authLimiter,
  asyncHandler(async (req, res) => {
    const { value, error } = registerSchema.validate(req.body, { abortEarly: false });
    if (error) throw ApiError.badRequest('Invalid registration data', error.details);

    const orClauses = [];
    if (value.email) orClauses.push({ email: value.email });
    if (value.mobile) orClauses.push({ mobile: value.mobile });
    const existing = await User.findOne({ $or: orClauses });
    if (existing) throw ApiError.conflict('An account with this email or mobile already exists');

    const passwordHash = await bcrypt.hash(value.password, 10);
    const user = await User.create({
      fullName: value.fullName,
      email: value.email,
      mobile: value.mobile,
      designation: value.designation || '',
      passwordHash,
    });

    const tokens = await issueTokenPair(user);
    res.status(201).json({ user: user.toPublicJSON(), ...tokens });
  })
);

router.post(
  '/login',
  authLimiter,
  asyncHandler(async (req, res) => {
    const { value, error } = loginSchema.validate(req.body);
    if (error) throw ApiError.badRequest('Invalid login data', error.details);

    const identifier = value.identifier.trim().toLowerCase();
    const user = await User.findOne({ $or: [{ email: identifier }, { mobile: value.identifier.trim() }] });
    if (!user) throw ApiError.unauthorized('Invalid credentials');

    const ok = await bcrypt.compare(value.password, user.passwordHash);
    if (!ok) throw ApiError.unauthorized('Invalid credentials');

    const tokens = await issueTokenPair(user);
    res.json({ user: user.toPublicJSON(), ...tokens });
  })
);

router.post(
  '/refresh',
  asyncHandler(async (req, res) => {
    const { value, error } = refreshSchema.validate(req.body);
    if (error) throw ApiError.badRequest('Invalid refresh request', error.details);

    let payload;
    try {
      payload = verifyRefreshToken(value.refreshToken);
    } catch (err) {
      throw ApiError.unauthorized('Invalid or expired refresh token');
    }

    const user = await User.findById(payload.sub);
    if (!user) throw ApiError.unauthorized('User no longer exists');

    const tokenEntry = user.refreshTokens.find((t) => t.tokenId === payload.jti);
    if (!tokenEntry) throw ApiError.unauthorized('Refresh token has been revoked');

    // rotate: remove old, issue new
    user.refreshTokens = user.refreshTokens.filter((t) => t.tokenId !== payload.jti);
    const tokens = await issueTokenPair(user);
    res.json({ user: user.toPublicJSON(), ...tokens });
  })
);

router.post(
  '/logout',
  requireAuth,
  asyncHandler(async (req, res) => {
    const { refreshToken } = req.body || {};
    if (refreshToken) {
      try {
        const payload = verifyRefreshToken(refreshToken);
        req.user.refreshTokens = req.user.refreshTokens.filter((t) => t.tokenId !== payload.jti);
        await req.user.save();
      } catch (err) {
        // token already invalid/expired: nothing to revoke
      }
    }
    res.status(204).send();
  })
);

module.exports = router;
