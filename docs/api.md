# CoChat API Contract

Base URL: `http://localhost:4000` (dev). All REST endpoints are under `/api`.
All authenticated requests send `Authorization: Bearer <accessToken>`.
Errors: `{ "message": string, "details"?: any }` with a 4xx/5xx status.

## Auth

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/api/auth/register` | `{ fullName, email?, mobile?, password, designation? }` | one of email/mobile required |
| POST | `/api/auth/login` | `{ identifier, password }` | identifier = email or mobile |
| POST | `/api/auth/refresh` | `{ refreshToken }` | rotates refresh token |
| POST | `/api/auth/logout` | `{ refreshToken }` (auth required) | revokes that refresh token |

Register/login responses: `{ user, accessToken, refreshToken }`. Access token expires in 15m, refresh in 7d — clients should call `/auth/refresh` on 401s.

## Users / Team directory

| Method | Path | Notes |
|---|---|---|
| GET | `/api/users/me` | current profile |
| PATCH | `/api/users/me` | `{ fullName?, designation? }` |
| POST | `/api/users/me/avatar` | multipart `avatar` file |
| GET | `/api/users?q=` | team directory, searches name/email/mobile/designation |
| GET | `/api/users/:id` | single profile |

## Chats (private + group message threads)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/chats` | list of chats (private + group) with `lastMessage`, `unreadCount` |
| POST | `/api/chats/private` | `{ userId }` — get-or-create a private chat |
| GET | `/api/chats/:chatId/messages?before=&limit=` | paginate backwards by message id |
| POST | `/api/chats/:chatId/messages` | multipart: `text?`, `file?` (one required) |
| POST | `/api/chats/:chatId/read` | mark chat read up to now |

## Groups

| Method | Path | Notes |
|---|---|---|
| POST | `/api/groups` | `{ name, memberIds[] }`, creator becomes admin |
| GET | `/api/groups/:id` | group info + member list |
| PATCH | `/api/groups/:id` | admin only; `name` field + multipart `avatar` |
| POST | `/api/groups/:id/members` | admin only; `{ memberIds[] }` |
| DELETE | `/api/groups/:id/members/:userId` | admin only (or self) |
| POST | `/api/groups/:id/leave` | self-leave |

## Notifications

| Method | Path | Notes |
|---|---|---|
| GET | `/api/notifications` | latest 200, newest first |
| POST | `/api/notifications/:id/read` | mark one read |
| POST | `/api/notifications/read-all` | mark all read |

## Media

Uploaded files are served statically from `/uploads/<avatars\|media>/<filename>`; the REST responses embed the absolute URL, so clients never construct paths themselves.

## Socket.IO realtime

Connect to the same origin as the REST API with `auth: { token: accessToken }`. On connect the server joins the socket to `user:<id>` and every `chat:<chatId>` room the user belongs to.

**Server → client events**
- `presence:update` — `{ userId, status: 'online'|'away'|'offline' }`
- `message:new` — `{ message, chatId }`
- `chat:updated` — `{ chat }` — sent individually per member (their own `unreadCount`); use to resort/refresh the chat list
- `chat:read` — `{ chatId, userId }`
- `group:updated` — `{ chat, action: 'created'|'info_updated'|'members_added'|'members_removed'|'left' }`
- `typing:update` — `{ chatId, userId, isTyping }`
- `notification:new` — `{ notification }`

**Client → server events**
- `presence:activity` — call periodically while the app is foregrounded/active; resets the away timer (default 2 min of silence → `away`)
- `typing:start` / `typing:stop` — `{ chatId }`

Presence status is derived server-side from socket connection state, not stored client-side: `online` on connect/activity, `away` after `PRESENCE_AWAY_MINUTES` of no activity while still connected, `offline` when the last socket for that user disconnects.
