# CoChat — Team Chat Application

One-to-one and group messaging for teams: real-time chat, presence, media sharing, and a team directory — across a Node.js/MongoDB backend, an Angular website, and native Android (Kotlin) and iOS (Swift) apps.

## Structure

| Path | What | Status |
|---|---|---|
| [`backend/`](backend/README.md) | Node.js + Express + MongoDB + Socket.IO API | ✅ Built, running, verified end-to-end (REST + realtime) |
| [`web/`](web/) | Angular 21 website | ✅ Built, verified live in-browser; real bugs found & fixed |
| [`android/`](android/README.md) | Native Kotlin + Jetpack Compose app | ✅ Built, installed, run on an emulator, verified end-to-end incl. cross-client data with the web app |
| [`ios/`](ios/README.md) | Native Swift + SwiftUI app | ⚠️ Written, **not compiled** — no iOS toolchain exists on Windows |
| [`docs/api.md`](docs/api.md) | Shared REST + Socket.IO contract all four clients implement | — |

## Quick start

```bash
# 1. Backend (needs MongoDB reachable via MONGODB_URI — see backend/.env.example)
cd backend && npm install && cp .env.example .env && npm run dev

# 2. Web
cd web && npm install && ng serve   # http://localhost:4200

# 3. Android — open android/ in Android Studio, or:
cd android && ./gradlew assembleDebug

# 4. iOS — on a Mac only, see ios/README.md (requires XcodeGen)
```

## Auth

Email-or-mobile + password, JWT access (15m) + refresh (7d) token pair, refreshed transparently by all three built clients on a 401.

## What's genuinely verified vs. not

The backend, web app, and Android app were each built and exercised against real running instances in this session — including cross-client checks (e.g. the Android app registering and messaging a user that was created through the web app, against the same backend). Two real bugs were caught this way and fixed: a per-recipient unread-count broadcast bug in the backend, and a signal-timing race in the Angular chat thread. The iOS app follows the same architecture but is unverified — see [`ios/README.md`](ios/README.md) for what to expect on first build.
