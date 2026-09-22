# FamilyGuard

A transparent, consent-based family-safety platform: a public website, a parent
web dashboard, and a visibly-installed Android companion app. Built as the
honest alternative to hidden-monitoring apps — no disguised install, no call
recording, no message reading, no keylogging, no remote mic/camera access,
anywhere in this codebase.

Full feasibility research, architecture, data model, API contract, security
model, phased plan, and known limitations are written up in the companion
document: **FamilyGuard — Feasibility, Architecture & Build Plan**
(shared as a Claude Docs link alongside this delivery).

## What's in this delivery

```
web/        Next.js 14 + TypeScript — marketing site, parent dashboard, API
android/    Kotlin companion app (child device) — Jetpack Compose, MVVM
```

Start with `web/README.md` and `android/README.md` for setup instructions
specific to each half. The two talk to each other only over the REST API
documented in `web/README.md` (and mirrored in the architecture doc) —
`android/` is the only client of that API besides the dashboard itself.

## Read this before you build

**`npm install` could not be run in the sandbox this was built in** —
`registry.npmjs.org` and every CDN mirror were blocked at the network layer
there, confirmed directly. `web/README.md` has the full verification status:
what was checked without network access (syntax parsing of all 66 TypeScript
files, schema/JSON validation, full API-route inventory) and what you still
need to run yourself (`npm install && npm run build`) before trusting this
as a working build. The Android app has the equivalent honest caveat: there's
no Android SDK/emulator in this sandbox, so it was rigorously
manually-reviewed (package/import consistency, EN/AR string-key parity,
forbidden-feature grep, wiring checks) but never compiled — open it in
Android Studio to get a real build signal.

Neither of these is a shortcut taken to save effort — they're genuine
environment limitations, called out plainly rather than papered over, in
keeping with the rest of this project's rule against ever showing something
as working when it hasn't actually been verified.

## iOS

Not built in this MVP. Location sharing, geofencing, and SOS/check-ins are
buildable on iOS with the same native APIs (CoreLocation) the Android app
uses. Screen-time and app-limit features are not currently feasible for a
third-party app outside Apple's own Family Sharing system — see the
Feasibility section of the architecture doc for the specifics and sources.
