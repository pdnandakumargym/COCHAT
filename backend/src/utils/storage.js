const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const multer = require('multer');
const env = require('../config/env');

const UPLOAD_ROOT = path.join(__dirname, '..', '..', 'uploads');

const ALLOWED_MIME = new Set([
  'image/jpeg',
  'image/png',
  'image/gif',
  'image/webp',
  'video/mp4',
  'video/quicktime',
  'video/webm',
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'text/plain',
  'application/zip',
]);

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function makeDiskStorage(subfolder) {
  const dest = path.join(UPLOAD_ROOT, subfolder);
  ensureDir(dest);
  return multer.diskStorage({
    // re-ensure on every write, not just at startup: the folder may be absent
    // on a fresh checkout/deploy or if something external cleared `uploads/`
    destination: (req, file, cb) => {
      ensureDir(dest);
      cb(null, dest);
    },
    filename: (req, file, cb) => {
      const unique = crypto.randomBytes(16).toString('hex');
      const ext = path.extname(file.originalname).toLowerCase();
      cb(null, `${Date.now()}_${unique}${ext}`);
    },
  });
}

function fileFilter(req, file, cb) {
  if (ALLOWED_MIME.has(file.mimetype)) {
    cb(null, true);
  } else {
    cb(new Error(`Unsupported file type: ${file.mimetype}`));
  }
}

const avatarUpload = multer({
  storage: makeDiskStorage('avatars'),
  limits: { fileSize: 5 * 1024 * 1024 },
  fileFilter: (req, file, cb) => {
    if (file.mimetype.startsWith('image/')) cb(null, true);
    else cb(new Error('Avatar must be an image'));
  },
});

const mediaUpload = multer({
  storage: makeDiskStorage('media'),
  limits: { fileSize: 50 * 1024 * 1024 },
  fileFilter,
});

function fileTypeFromMime(mimetype) {
  if (mimetype.startsWith('image/')) return 'image';
  if (mimetype.startsWith('video/')) return 'video';
  return 'file';
}

function toPublicUrl(absolutePath) {
  const rel = path.relative(UPLOAD_ROOT, absolutePath).split(path.sep).join('/');
  return `${env.publicBaseUrl}/uploads/${rel}`;
}

module.exports = { UPLOAD_ROOT, avatarUpload, mediaUpload, fileTypeFromMime, toPublicUrl };
