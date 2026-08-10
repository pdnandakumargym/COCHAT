const ApiError = require('../utils/ApiError');
const multer = require('multer');

function notFoundHandler(req, res) {
  res.status(404).json({ message: `Route not found: ${req.method} ${req.originalUrl}` });
}

function errorHandler(err, req, res, next) { // eslint-disable-line no-unused-vars
  if (err instanceof multer.MulterError || /Unsupported file type|must be an image/.test(err.message || '')) {
    return res.status(400).json({ message: err.message });
  }

  if (err.name === 'ValidationError' || err.isJoi) {
    return res.status(400).json({ message: err.message, details: err.details });
  }

  if (err.code === 11000) {
    return res.status(409).json({ message: 'A record with these details already exists' });
  }

  if (err instanceof ApiError) {
    return res.status(err.statusCode).json({ message: err.message, details: err.details });
  }

  console.error(err);
  return res.status(500).json({ message: 'Internal server error' });
}

module.exports = { notFoundHandler, errorHandler };
