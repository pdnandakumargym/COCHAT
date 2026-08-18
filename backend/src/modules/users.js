const express = require('express');
const Joi = require('joi');
const User = require('../models/User');
const ApiError = require('../utils/ApiError');
const asyncHandler = require('../utils/asyncHandler');
const { requireAuth } = require('../middleware/auth');
const { avatarUpload, toPublicUrl } = require('../utils/storage');
const presence = require('./presence');

const router = express.Router();
router.use(requireAuth);

function withStatus(user) {
  const json = user.toPublicJSON();
  json.status = presence.getStatus(user._id.toString());
  return json;
}

router.get(
  '/me',
  asyncHandler(async (req, res) => {
    res.json({ user: withStatus(req.user) });
  })
);

const updateSchema = Joi.object({
  fullName: Joi.string().trim().min(2).max(100).optional(),
  designation: Joi.string().trim().max(100).allow('').optional(),
});

router.patch(
  '/me',
  asyncHandler(async (req, res) => {
    const { value, error } = updateSchema.validate(req.body, { abortEarly: false });
    if (error) throw ApiError.badRequest('Invalid profile data', error.details);

    Object.assign(req.user, value);
    await req.user.save();
    res.json({ user: withStatus(req.user) });
  })
);

router.post(
  '/me/avatar',
  avatarUpload.single('avatar'),
  asyncHandler(async (req, res) => {
    if (!req.file) throw ApiError.badRequest('No avatar file provided');
    req.user.profilePicture = toPublicUrl(req.file.path);
    await req.user.save();
    res.json({ user: withStatus(req.user) });
  })
);

const pushTokenSchema = Joi.object({ token: Joi.string().required() });

router.post(
  '/me/push-token',
  asyncHandler(async (req, res) => {
    const { value, error } = pushTokenSchema.validate(req.body);
    if (error) throw ApiError.badRequest('token is required', error.details);
    if (!req.user.pushTokens.includes(value.token)) {
      req.user.pushTokens.push(value.token);
      await req.user.save();
    }
    res.status(204).send();
  })
);

router.delete(
  '/me/push-token',
  asyncHandler(async (req, res) => {
    const { value, error } = pushTokenSchema.validate(req.body);
    if (error) throw ApiError.badRequest('token is required', error.details);
    req.user.pushTokens = req.user.pushTokens.filter((t) => t !== value.token);
    await req.user.save();
    res.status(204).send();
  })
);

router.get(
  '/',
  asyncHandler(async (req, res) => {
    const { q } = req.query;
    const filter = { _id: { $ne: req.user._id } };
    if (q && q.trim()) {
      const regex = new RegExp(q.trim(), 'i');
      filter.$or = [{ fullName: regex }, { email: regex }, { mobile: regex }, { designation: regex }];
    }
    const users = await User.find(filter).sort({ fullName: 1 }).limit(500);
    res.json({ users: users.map(withStatus) });
  })
);

router.get(
  '/:id',
  asyncHandler(async (req, res) => {
    const user = await User.findById(req.params.id);
    if (!user) throw ApiError.notFound('User not found');
    res.json({ user: withStatus(user) });
  })
);

module.exports = router;
