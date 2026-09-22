# FamilyGuard — Web (marketing site + parent dashboard + API)

FamilyGuard is a transparent, consent-based family-safety platform. This package
contains the Next.js 14 (App Router) + TypeScript web app: the public marketing
site, the authenticated parent dashboard, and the Next.js API routes that both
the dashboard and the (separate) Android/iOS companion app talk to.

**What this product deliberately does not do:** hide its presence, record calls,
read message content from any app, log keystrokes, or activate the microphone
or camera remotely. If a feature would require any of that, it isn't here.

## Stack

- Next.js 14 App Router, TypeScript, Tailwind CSS
- Auth.js (NextAuth v4) — Credentials provider (bcrypt-hashed passwords), JWT sessions
- A separate, signed JWT "device token" for the companion app, verified via
  `Authorization: Bearer <token>` on every device-facing route — never the
  parent's session cookie
- PostgreSQL via Prisma
- Leaflet + react-leaflet + OpenStreetMap tiles for the live map (no API key,
  no Google Maps/Mapbox)
- i18n: English and Arabic, with full RTL layout for Arabic, via a
  `/en/...` / `/ar/...` locale-segment approach (no external i18n framework)

## Project layout

```
prisma/
  schema.prisma       Prisma schema (Postgres)
  seed.ts             Seeds one demo family (isDemo: true) with sample data
src/
  app/
    [locale]/          Marketing pages + dashboard (locale-prefixed, RTL-aware)
      dashboard/        Parent dashboard (auth-guarded by middleware)
    api/                Route handlers (parent session + device-token auth)
  components/           Shared UI + client components
  i18n/                 Locale config, dictionaries (en.json / ar.json)
  lib/                  Prisma client, NextAuth config, device-token signing,
                         parent-session helpers, access-log helper
  middleware.ts          Locale redirect + dashboard auth guard
```

## Local setup

1. **Install dependencies**

   ```bash
   npm install
   ```

2. **Configure environment**

   ```bash
   cp .env.example .env
   ```

   Fill in `DATABASE_URL` for a local Postgres instance, and generate two
   independent random secrets for `NEXTAUTH_SECRET` and `DEVICE_JWT_SECRET`
   (`openssl rand -base64 32` for each — keep them different; one signs parent
   sessions, the other signs device tokens, and neither should ever verify the
   other's tokens).

3. **Provision the database**

   ```bash
   npx prisma generate
   npx prisma migrate dev --name init
   ```

4. **Seed demo data**

   ```bash
   npm run seed
   ```

   This creates one family with `isDemo: true`, three sample devices (Android,
   iOS, and an offline device), sample geofences, ~7 days of location and
   usage history, and a few alerts including an unacknowledged SOS. Every
   dashboard view for a demo family shows a persistent "Demo data" banner.

   Demo login printed by the seed script: `demo@familyguard.app` /
   `DemoParent123!`.

5. **Run the dev server**

   ```bash
   npm run dev
   ```

   Visit `http://localhost:3000` (redirects to `/en`), or `http://localhost:3000/ar`
   for the Arabic/RTL layout.

## Verification commands

```bash
npm run typecheck   # tsc --noEmit
npm run build       # next build
```

Every route/page that touches Prisma is marked `export const dynamic =
"force-dynamic"` so `next build` does not attempt to execute database calls at
build time — it only needs `npx prisma generate` to have produced the client
types, not a live database connection.

## API contract

Parent-facing routes resolve the caller's family from their NextAuth session
(`requireParentContext()` in `src/lib/parentSession.ts`) — a `familyId` in the
request body is never trusted. Device-facing routes resolve the family from the
signed device-token claims (`authenticateDevice()` in `src/lib/deviceAuth.ts`)
and additionally check the token's `deviceId` matches the `:id` route param, so
one paired device can never act as another. Every mutating call writes an
`AccessLog` row.

See the route handlers under `src/app/api/` for the full contract: pairing
(generate/redeem), device status/location/usage/SOS/geofence-events/fcm-token,
parent-facing family/devices/geofences/alerts/limits, and account deletion.

## Deployment notes

- **Hosting:** Vercel is the natural fit for Next.js, but any Node host that
  supports the App Router works.
- **Database:** any managed Postgres works — Supabase, Neon, and Railway are
  all reasonable options; nothing here is hard-coded to a specific provider.
  Point `DATABASE_URL` at it and run `npx prisma migrate deploy` as part of
  your release step.
- **Secrets:** set `NEXTAUTH_SECRET`, `DEVICE_JWT_SECRET`, and `NEXTAUTH_URL`
  (your production origin) in your host's environment configuration. Never
  commit `.env`.
- **Push notifications:** the `FCM_TOKEN` registration endpoint
  (`POST /api/devices/:id/fcm-token`) is implemented and stores the token, but
  server-initiated push sending is not wired up in this MVP — the
  `FIREBASE_*` variables in `.env.example` are placeholders for that future
  work.
- **Data retention:** `Family.retentionDays` exists in the schema for a future
  scheduled-deletion job; no cron/worker is included in this MVP.

## Verification status (read this before assuming it builds)

The build sandbox this project was written in blocks outbound access to
`registry.npmjs.org` and every CDN mirror (jsdelivr, unpkg, esm.sh, GitHub
Packages) at the network layer — confirmed directly, not just by the AI
assistant's own attempt. `npm install` could not be run here, which means
`next build`, a real `tsc --noEmit` (with actual type packages present), and
a live-database smoke test were **not** performed in this environment.

To compensate, two checks were run without network access:

1. Every `.ts`/`.tsx` file (66 total) was parsed with `tsc` against a
   permissive, stub-typed config (`declare module "*"`) to catch syntax
   errors independent of missing packages. Result: **zero parse/syntax
   errors**; the only diagnostics were the expected fallout of stubbing out
   React/Next/Prisma/NextAuth types (missing `process`, generic type
   arguments on `any`-typed calls) — not real bugs.
2. `prisma/schema.prisma`, `package.json`, and both i18n dictionaries were
   validated as syntactically well-formed (Prisma DSL structure and JSON
   parsing respectively), and every route in the API contract above was
   confirmed present on disk.

**Before you rely on this build, run it yourself:**

```bash
npm install
npx prisma generate
npm run typecheck
npm run build
```

in an environment with normal registry access, and fix whatever that surfaces
— static review is a reasonable substitute for a compiler, not an equivalent
to one.

## Known limitations of this MVP (by design, not oversight)

- iOS supports location sharing, geofencing, and SOS/check-ins only. Screen
  time and app-limit features are Android-only, because Apple requires the
  Family Controls entitlement plus the child's device already being enrolled
  in Apple's own Family Sharing — see the in-app Compatibility page for the
  full explanation shown to users.
- Pricing tiers are a UI-only concept for this MVP; no payment processor is
  connected.
- The support-page contact form is a client-side stub (it confirms receipt in
  the UI) since no transactional email provider is wired up; see the
  commented `EMAIL_*` variables in `.env.example` for where that would plug in.
