# CoChat Android

Native Kotlin + Jetpack Compose client (MVVM-ish: repositories + Compose-local state, one Socket.IO connection shared app-wide via `RealtimeStore`).

## Setup

1. Open in Android Studio, or build from the CLI with `./gradlew assembleDebug`.
2. The app talks to the backend at `10.0.2.2:4000` by default (`NetworkConfig.kt`) — the Android emulator's alias for the host machine's `localhost`, so it works out of the box against a locally running `../backend`.
3. For a physical device on the same LAN, change `BASE_URL`/`SOCKET_URL` in `data/remote/NetworkModule.kt` to the dev machine's LAN IP, and add that IP to `res/xml/network_security_config.xml` (cleartext HTTP is only allowlisted for `10.0.2.2`/`localhost`). A production build should point at an `https://` backend instead, which doesn't need that exemption at all.

## Structure

- `data/model` — Kotlin data classes mirroring the backend's JSON contract (`../docs/api.md`)
- `data/remote` — Retrofit `ApiService`, the Socket.IO wrapper (`SocketManager`), JWT storage (`TokenStore`, DataStore-backed), and the OkHttp auth interceptor/authenticator (adds the access token, refreshes on 401)
- `data/repository` — one repository per backend module, plus `RealtimeStore`: the single app-wide socket connection fanned out into `StateFlow`s that every screen collects
- `ui/*` — one package per feature (auth, chats, team, profile, notifications, settings), Composable screens driven by local `remember`/`LaunchedEffect` state talking directly to repositories
- `MainActivity.kt` — NavHost + bottom navigation wiring all screens together

No dependency-injection framework — `CoChatApp` (the `Application` subclass) is a small hand-rolled service locator; screens reach it via `(application as CoChatApp)` in `MainActivity`.
