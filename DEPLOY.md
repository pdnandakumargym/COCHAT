# Deploying the backend (Render)

Prep is done: this is now a git repo with an initial commit, and [`render.yaml`](render.yaml) at the root describes the service so Render can deploy it as a "Blueprint" with almost no manual config.

## 1. Push to GitHub

I can't do this step — it needs your GitHub login, which I don't have and shouldn't ask you to hand me. Easiest path given you have GitHub Desktop installed:

1. Open **GitHub Desktop** → File → Add Local Repository → point it at `D:\NANDAKUMARPD\CUSTOM\COCHAT`
2. Click **Publish repository** (choose public or private — either works for Render)

Or from the command line, after creating an empty repo on github.com:
```bash
git remote add origin https://github.com/<you>/<repo>.git
git branch -M main
git push -u origin main
```

## 2. Deploy on Render

1. Sign in at [render.com](https://render.com) (GitHub login works) — this also needs your login, so it's on you too.
2. **New +** → **Blueprint** → select the repo you just pushed. Render reads `render.yaml` automatically.
3. It'll prompt for the three secrets I marked `sync: false` (deliberately not stored in the repo):
   - `MONGODB_URI` — your Atlas connection string (the one already in `backend/.env` locally)
   - `CORS_ORIGINS` — leave blank for now, or set to your web app's URL once you have one; the mobile apps don't need CORS
   - `PUBLIC_BASE_URL` — set this to the Render URL Render shows you for this service (e.g. `https://cochat-backend.onrender.com`) — it's used to build links to uploaded files
4. Deploy. Render gives you a URL like `https://cochat-backend.onrender.com`.

## 3. Tell me the URL

Once deployed, send me that URL and I'll:
- Point the Android app's `NetworkConfig.kt` at it (switching from the emulator-only `10.0.2.2` to your real HTTPS URL, and dropping the cleartext-traffic exception since it'll be HTTPS)
- Rebuild the APK so it works for anyone you send it to over WhatsApp, not just this machine's emulator

## Know before you deploy

- **Free tier spins down after 15 minutes idle** and takes ~30–60s to wake back up on the next request — expect a slow first message after any quiet period. Fine for sharing with a few people to try out; upgrade the plan if that's annoying.
- **Free tier disk is ephemeral** — uploaded files (avatars, shared media) won't survive a redeploy or a spin-down/wake cycle. Fine for a demo; for real use you'd swap `backend/src/utils/storage.js` for an S3-compatible bucket (the storage interface is already isolated there specifically so that swap doesn't touch any callers).
