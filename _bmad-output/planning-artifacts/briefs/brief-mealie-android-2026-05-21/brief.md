---
title: "Product Brief: Mealie Android"
status: final
created: 2026-05-21
updated: 2026-05-21
---

# Product Brief: Mealie Android

## Executive Summary

Mealie is a powerful self-hosted recipe manager with a capable web interface - but on Android, the PWA wrapper fails at the two moments that matter most. Every server update silently logs users out, forcing a password manager detour before they can access their content. And in the places people most need a shopping list - supermarkets, basements, anywhere with unreliable mobile coverage - the PWA shows a blank screen and nothing more. These are not edge cases. They are the core use cases.

Mealie Android is a native Android client that fixes both problems and goes further. Authentication is handled once and silently maintained in the background. Recipes and shopping lists are stored locally and available without any signal - changes made offline sync automatically when connectivity returns. The app integrates with Android's share system for seamless recipe import. It is not a reimagining of Mealie; it is Mealie's web experience completed for Android, with the offline and authentication limitations that a PWA fundamentally cannot overcome.

The project is open source and built for the whole Android Mealie community. The initial audience is small - people who already run Mealie and have felt these frustrations - but the aspiration is broader: to become the native Android client the Mealie project itself can point to, or one day ship alongside.

## The Problem

Mealie's web app is powerful, but its PWA wrapper on Android lets users down at the two moments that matter most: authentication and connectivity.

Every time a Mealie server is updated, users are silently logged out. Getting back in requires a detour through a password manager - friction that would be acceptable once, but recurs unpredictably. The cost is higher when the session breaks mid-flow: sharing a recipe URL to the PWA while not logged in dumps the user at a login screen, and after authenticating, the original share is lost. The user has to go back to the source app and start again.

The offline problem is harder to ignore. Mobile data coverage in Germany is unreliable, and supermarkets - exactly where a shopping list matters most - are frequent dead zones. If the app is opened fresh without connectivity, the PWA responds with a full-screen block: "You are offline." If the shopping list was already open before signal was lost, it remains usable - but navigating away from it means it's gone. Users are left hunting for signal, keeping the app frozen on one screen, or committing the list to memory and hoping for the best. The PWA acknowledges the problem - it shows a sync warning on the shopping list - but does nothing to prevent it.

The practical consequences are real: monthly or occasional items get forgotten on grocery runs. And at least one family member - frustrated enough - abandoned Mealie for a basic notes app, splitting household coordination across two systems.

## The Solution

Mealie Android is a native Android client for self-hosted Mealie instances that fixes the two moments the PWA gets wrong - staying logged in and working without signal - and extends the experience in ways a browser wrapper never can.

Authentication happens once. The app stores credentials securely on the device and handles token refresh silently in the background. Server updates no longer break sessions visibly, and if re-authentication is needed it happens automatically. Sharing a recipe URL from any browser or app opens the import flow directly, with no login interruption.

Offline is not a special state - it is just how the app works when there is no signal. Recipes are browsable and the shopping list is fully editable without connectivity. A subtle indicator communicates that the app is offline; changes that are queued for sync - a new item on the shopping list, an added recipe - are flagged more explicitly, so the user always knows what is pending. When signal is restored, sync happens automatically in the background.

A configurable server URL means the app works across different Mealie instances - households, friends, shared setups - without rebuilding or hardcoding anything.

## Who This Serves

Mealie Android is built for people who already use Mealie - the self-hosted recipe manager - and want a mobile experience that works reliably.

There are two kinds of users, distinguished only by how they get started. **Operators** run their own Mealie instance: they configure the app, handle the server URL, and often set it up for others. **Passengers** share someone else's instance - a partner, a parent, a housemate - and need a smooth onboarding path that doesn't require technical knowledge. Once past setup, both groups want exactly the same thing: a shopping list that works in the supermarket, recipes that are available when needed, and an app that doesn't ask them to log in again.

The initial audience is small and self-selected - Mealie users are already comfortable with self-hosted software. A potential future audience on the Play Store or F-Droid would be similar: technically inclined people, or people adjacent to someone who is.

## Scope

**Version 1 includes:**
- Configurable Mealie server URL and credentials, entered once at first launch via a setup dialog
- Persistent authentication - one-time login, silent token refresh in the background
- Offline shopping list - full read and write access without connectivity, with automatic sync on reconnect
- Offline indicator - subtle global status when the app is offline, explicit pending-sync feedback on individual changes

**Post-v1:**
- Offline recipe browsing - full recipe library cached locally and browsable without signal
- Recipe import via URL share - sharing a URL from any app opens the Mealie import flow directly, with silent re-authentication if needed
- Streamlined onboarding via QR code or setup link - operator generates, non-technical users scan or tap to configure

No features are explicitly ruled out for future versions.

## Success Criteria

The primary signal for v1 is simple: a Mealie server update no longer prompts a login. Opening the app after an update and landing directly on your content - that is when the authentication problem is solved.

The secondary signal is field use: the shopping list is accessible and editable in the supermarket without signal, and sync resolves correctly on the way home. No winging it, no hunting for signal.

For the broader audience - family, friends, the small circle of people already using Mealie - success is adoption without complaint. They use the app instead of the PWA because it genuinely works better, not because they were asked to.

If published to the Play Store or F-Droid, success is not a download count. It is other Mealie users reporting that the same problems - auth breakage, offline failure - are solved for them too.

## Vision

Mealie Android exists to close the gap between what Mealie promises and what Android can actually deliver. A PWA cannot do true background sync - Android reclaims those resources aggressively. A native app can. That difference is invisible when everything works, and total when it doesn't.

The long-term goal is simple: the app should feel exactly like Mealie's web interface, with one silent addition - connectivity is no longer a constraint. Your recipes and lists are there when you need them, changes sync in the background when signal returns, and you never have to think about whether you have data left on your plan.

The project is open source from the start, welcoming contributions from the Mealie community. The aspiration - if the app earns it - is to become the endorsed Android client for Mealie: something the project points to, or eventually ships alongside. Not because it was planned that way, but because it solved a real problem well enough that the community made it their own.
