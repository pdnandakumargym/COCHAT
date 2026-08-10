# CoChat Backend

Node.js + Express + MongoDB (Mongoose) + Socket.IO backend for the CoChat team messaging app.

## Setup

```bash
npm install
cp .env.example .env   # then fill in JWT secrets and MONGODB_URI
npm run dev
```

Requires a reachable MongoDB instance — local `mongod` or Atlas — via `MONGODB_URI` in `.env`. Uploaded media is stored on local disk under `uploads/` and served at `/uploads/...`; swap `src/utils/storage.js` for an S3-backed implementation later without touching callers.

See [`../docs/api.md`](../docs/api.md) for the full REST + Socket.IO contract shared by the Angular, Android, and iOS clients.

## Structure

- `src/models` — Mongoose schemas (User, Chat, Message, Notification)
- `src/modules` — one file per feature area (auth, users, chats, groups, notifications, presence), each exporting an Express router
- `src/middleware` — JWT auth guard, centralized error handler
- `src/realtime/socket.js` — Socket.IO gateway (JWT handshake auth, presence, typing, room management)
- `src/utils` — JWT signing, multer storage config, shared error/async helpers
