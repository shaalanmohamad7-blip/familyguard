# FamilyGuard — Deployment guide (getting it live)

This is the step-by-step runbook for taking FamilyGuard from source code to a
live service. It has three parts:

1. **What you must supply** — accounts and credentials only you can create.
2. **Deploy the web app + API + dashboard** (the part users and devices talk to).
3. **Build and distribute the Android companion app.**

Nothing here can be done from inside the Claude sandbox: a live deployment needs
hosting, a real database, and accounts tied to your identity and payment method.
These steps are written so you can do it yourself end to end, or hand them to a
developer.

> Before anything else, run the build locally once, because the sandbox this was
> generated in had no npm access and could not:
> ```bash
> cd web
> npm install
> npx prisma generate
> npm run typecheck
> npm run build
> ```
> Fix anything that surfaces here before deploying. See `web/README.md` →
> "Verification status" for exactly what was and wasn't checked.

---

## 1. What you must supply

| Thing | Why | Where to get it | Cost |
|---|---|---|---|
| **A Postgres database** | Stores families, devices, locations, alerts | Neon, Supabase, Railway, or Vercel Postgres | Free tier is enough to start |
| **A host for the web app** | Serves the site, dashboard, and API | Vercel (easiest for Next.js), or Railway/Render | Free/Hobby tier to start |
| **`NEXTAUTH_SECRET`** | Signs parent login sessions | `openssl rand -base64 32` | free |
| **`DEVICE_JWT_SECRET`** | Signs the separate device tokens | `openssl rand -base64 32` (different value) | free |
| **A domain** (optional) | e.g. `app.yourbrand.com` | Any registrar | ~$10–15/yr |
| **A Firebase project** (for push) | Wakes the companion app to send a location on request | console.firebase.google.com | free |
| **A Google Play Developer account** | To distribute the Android app on the Play Store | play.google.com/console | one-time $25 |

You do **not** need Apple anything yet — iOS is documented as a separate,
later track (see the architecture doc; screen-time on iOS needs Apple's
Family Controls entitlement, which is a per-app approval).

---

## 2. Deploy the web app (Vercel + managed Postgres)

The web app is Next.js 14 + Prisma. Vercel is the smoothest path; the same
env vars work on Railway or Render.

### 2a. Create the database
1. Create a Postgres instance on Neon/Supabase/Railway.
2. Copy its connection string — it looks like
   `postgresql://USER:PASSWORD@HOST:5432/DBNAME?sslmode=require`.

### 2b. Push the code to GitHub
```bash
cd web
git init && git add . && git commit -m "FamilyGuard web"
# create an empty repo on GitHub, then:
git remote add origin https://github.com/<you>/familyguard-web.git
git push -u origin main
```

### 2c. Import into Vercel
1. vercel.com → **Add New… → Project** → import the repo.
2. Framework preset: **Next.js** (auto-detected).
3. Add environment variables (from `web/.env.example`):
   - `DATABASE_URL` → your Postgres string from 2a
   - `NEXTAUTH_SECRET` → `openssl rand -base64 32`
   - `NEXTAUTH_URL` → your final URL, e.g. `https://familyguard.vercel.app`
   - `DEVICE_JWT_SECRET` → a **different** `openssl rand -base64 32`
   - (optional push) `FIREBASE_PROJECT_ID`, `FIREBASE_CLIENT_EMAIL`,
     `FIREBASE_PRIVATE_KEY` — from your Firebase service-account JSON (step 3b).
4. Deploy.

### 2d. Create the database tables
The project ships the Prisma schema but no migration files yet. From your
machine, pointed at the **production** `DATABASE_URL`:
```bash
cd web
# option A — simplest, no migration history:
npx prisma db push

# option B — versioned migrations (recommended for a real product):
npx prisma migrate dev --name init      # generates web/prisma/migrations
git add prisma/migrations && git commit -m "init migration" && git push
# then on the server, the deploy step should run:
npx prisma migrate deploy
```

### 2e. (Optional) seed demo data
```bash
npm run seed   # creates the "Demo family" flagged isDemo=true
```
This is clearly-labelled sample data, not live device data. Remove or skip it
for a clean production instance.

### 2f. Verify
- Visit your URL → the marketing site loads.
- `/dashboard` → sign in works (create an account or use the seeded one).
- The map, geofences, alerts pages render.

Your API base URL is now `https://<your-domain>/api` — you need this for the
Android app next.

---

## 3. Build and distribute the Android companion app

### 3a. Point the app at your server
In `android/`, set the base URL the app calls. It reads `BuildConfig.BASE_URL`
(used in `NetworkFactory.kt`). Set it in `android/app/build.gradle` (or
`gradle.properties`) to your deployed API, e.g.:
```
BASE_URL="https://<your-domain>/"
```
(Use `https://10.0.2.2:3000/` only for a local emulator against a local server.)

### 3b. Set up Firebase (for the "request location" push)
1. Firebase console → create a project.
2. Add an Android app with your package id `com.familyguard.app`.
3. Download `google-services.json` → put it in `android/app/`.
4. For the **server** to send pushes: Project settings → Service accounts →
   generate a private key. Put its `project_id`, `client_email`, and
   `private_key` into the Vercel env vars from step 2c.
   (Skip this and devices still register; the server just can't wake them.)

### 3c. Build
Open `android/` in Android Studio (it has the SDK/emulator this sandbox
lacked). Then:
```bash
cd android
./gradlew wrapper        # the wrapper jar isn't committed
./gradlew assembleRelease # or use Android Studio → Build → Generate Signed Bundle/APK
```
Test the pairing flow on a real device or emulator first (see step 4).

### 3d. Distribute
- **Play Store** (recommended): Play Console → create app → upload the signed
  **.aab** → complete the store listing and the **Data safety** form. Because
  FamilyGuard collects location, you'll declare that honestly; the transparent
  design (visible app, disclosure screen, no covert capture) is what keeps it
  inside Play's policies. Location-in-background use gets extra review — the
  persistent "sharing active" notice and the on-device disclosure screen are
  your justification.
- **Direct APK** (for a pilot with your own testers): share the signed `.apk`
  and have testers allow "install from unknown sources". Fine for a small trial,
  not for scale.

---

## 4. Test before you announce it

Walk these end to end on the live instance — they're the ones that matter most:

- [ ] **Device pairing** — generate a code in the dashboard, redeem it once on
      the phone; confirm a second attempt with the same code fails.
- [ ] **Permission revocation** — turn off background location on the phone;
      the dashboard should show the device as permission-disabled, not silently
      keep showing a stale location.
- [ ] **Family data isolation** — create two families; confirm neither can see
      the other's devices or locations (every API call is scoped by family).
- [ ] **Deletion** — run "delete all family data"; confirm devices, locations,
      geofences, and alerts are actually gone.
- [ ] **SOS** — press SOS on the phone; confirm the alert and location reach the
      dashboard.

---

## 5. Honest limits to know going in

- **No payment processor is wired up.** The pricing tiers are UI only. Adding
  billing (Stripe, or Play Billing for in-app) is its own task.
- **iOS is not built** — Android first, by design. iOS screen-time needs
  Apple's Family Controls entitlement (a per-app approval) and the child device
  enrolled in Apple Family Sharing; location/SOS/geofencing are feasible on iOS
  when you get there.
- **Geofencing on Android**: the app uses the standalone Geofence API, not a
  persistent foreground service — this matters because Google Play removes
  geofencing as an approved foreground-service use case as of Jan 27, 2027.
  You're already on the compliant path; don't "fix" it back to a foreground
  service.
- This stays a **transparent, consent-based** product: visible app, disclosure
  before pairing, no hidden install, no message/call/mic/camera capture. That's
  not just an ethics choice — it's what keeps it publishable on the Play Store.
