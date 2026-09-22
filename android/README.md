# FamilyGuard Android (Companion App)

Single-module Android app (Kotlin, Jetpack Compose + Material3, MVVM, manual `ServiceLocator`)
for the child/companion device. Package `com.familyguard.app`, `minSdk 26`, `targetSdk 35`.

## Before building

1. **Firebase project**: replace `app/google-services.json` with the real file from your
   Firebase console (Cloud Messaging only — no Firestore, no Firebase Auth are used).
2. **Backend base URL**: set in `app/build.gradle.kts` (`BASE_URL` build config field per
   build type) to point at your Next.js API deployment.
3. **Gradle wrapper**: `gradle/wrapper/gradle-wrapper.properties` is included, but the
   `gradle-wrapper.jar` binary itself isn't (this environment has no Android/Gradle tooling
   to generate it). Run `gradle wrapper --gradle-version 8.9` once from a machine with Gradle
   installed, or open the project in Android Studio, which will do this automatically.
4. **App icon**: a simple vector-based adaptive icon is included as a placeholder
   (`res/mipmap-anydpi-v26`, `res/drawable/ic_launcher_foreground.xml`) — swap in real brand
   assets before shipping.

## What this app deliberately does not do

No hidden/disguised app identity, no silent install, no reading of message content from any
app, no call recording, no keylogging, no remote microphone/camera activation. See
`ui/disclosure/DisclosureScreen.kt` for the exact, user-facing disclosure text.
