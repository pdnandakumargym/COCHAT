const jwt = require('jsonwebtoken');
const env = require('../config/env');

function signAccessToken(user) {
  return jwt.sign({ sub: user._id.toString() }, env.jwtAccessSecret, {
    expiresIn: env.jwtAccessExpires,
  });
}

function signRefreshToken(user, tokenId) {
  return jwt.sign({ sub: user._id.toString(), jti: tokenId }, env.jwtRefreshSecret, {
    expiresIn: env.jwtRefreshExpires,
  });
}

function verifyAccessToken(token) {
  return jwt.verify(token, env.jwtAccessSecret);
}

function verifyRefreshToken(token) {
  return jwt.verify(token, env.jwtRefreshSecret);
}

module.exports = { signAccessToken, signRefreshToken, verifyAccessToken, verifyRefreshToken };
