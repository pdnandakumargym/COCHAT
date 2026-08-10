# CoChat iOS

Native Swift + SwiftUI client. Same shape as the Android app: repositories wrapping a URLSession `APIClient`, one shared `RealtimeStore` (`ObservableObject`) fanning a single Socket.IO connection out to every screen, Keychain-backed JWT storage.

## ⚠️ Unverified — built without Xcode

This was written entirely on Windows, where **no tool can compile Swift for iOS or open an `.xcodeproj`** — unlike the backend, Angular app, and Android app (all built and run successfully in this same session), this code has not been compiled, let alone run. Treat it as a solid, carefully-written starting point, not a working build. Expect to fix a handful of small issues on first compile — most likely spots:

- The exact API surface of `socket.io-client-swift` in `Networking/SocketManager.swift` (closure signatures, enum case names) — written from memory of that library's public API, not checked against a real checkout.
- Any Info.plist key XcodeGen/Xcode wants that isn't in the hand-written one in `CoChat/Resources/Info.plist`.

## Setup (on a Mac)

1. Install [XcodeGen](https://github.com/yonaskolb/XcodeGen) (`brew install xcodegen`) — this project is defined as a `project.yml` rather than a hand-written `.xcodeproj`, since a `.xcodeproj`'s file format is fragile to write by hand without Xcode to generate/validate it.
2. From this `ios/` directory: `xcodegen generate` — produces `CoChat.xcodeproj`.
3. Open `CoChat.xcodeproj`, let Swift Package Manager resolve the `socket.io-client-swift` dependency, then build/run on a simulator.
4. The app talks to `http://localhost:4000` by default (`NetworkConfig.swift`) — the iOS Simulator shares the host Mac's network namespace, so it reaches a locally running `../backend` directly, no emulator-alias workaround needed (unlike Android's `10.0.2.2`). For a physical device on the same LAN, swap in the dev machine's LAN IP and add it to the `NSAppTransportSecurity` exceptions in `Info.plist`; production should point at `https://` instead.

## Structure

- `Models/` — Codable structs mirroring the backend's JSON contract (`../docs/api.md`)
- `Networking/` — `APIClient` (actor; adds the JWT, retries once on 401 via `/auth/refresh`), `SocketManager` (Socket.IO wrapper emitting a typed `AppSocketEvent`), `TokenStore` (Keychain-backed)
- `Repositories/` — one per backend module, plus `RealtimeStore`: the single socket connection fanned into `@Published` state every view reads
- `Views/` — one folder per feature (Auth, Chats, Team, Profile, Notifications, Settings), matching the same 12 screens as the web and Android clients
- `App/` — `CoChatApp` (entry point) and `AppContainer` (hand-rolled DI, mirrors `CoChatApp.kt` on Android — no DI framework)
