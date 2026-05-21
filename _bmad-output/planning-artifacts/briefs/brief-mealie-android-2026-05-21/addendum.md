# Addendum — Mealie Android Brief

## Onboarding UX: QR Code / Setup Link (Post-v1)

For non-technical users (family members, partners) who share someone else's Mealie instance, typing a server URL in a setup dialog is a friction point. Two post-v1 options to consider:

- **QR code pairing**: The operator generates a QR code (from within the app or Mealie's interface) encoding the server URL. The new user scans it on first launch — no typing required.
- **Setup link**: Operator shares a deep link (e.g. `mealie://setup?server=https://...`) via WhatsApp or SMS. Tapping it on a device with the app installed auto-configures the server URL.

Both approaches require the operator to do a one-time action; the passenger just scans or taps. Local network discovery (mDNS) was considered but ruled out — it only works on the same wifi network and is useless for remote setup.

## Authentication Approach

API tokens were considered but ruled out: only Mealie admins can create them, making them unsuitable for non-admin household members. The JWT flow (username + password → access token + refresh token, stored in Android's EncryptedSharedPreferences) is the correct approach for all user types.

## Tech Stack Decision

Native Android (Kotlin + Jetpack Compose) chosen over Flutter and React Native. Flutter was considered as a future iOS path, but ruled out: the developer has no Mac available for iOS compilation. React Native ruled out for offline-first use case — offline-first apps and Android-specific features (share intents, background sync) are better served by native APIs.
