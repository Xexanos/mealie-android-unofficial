# Mealie Android

> **Unofficial project.** This is an independent Android app and is not affiliated with, endorsed by, or part of the official Mealie project.

A native Android shopping list app backed by self-hosted [Mealie](https://mealie.io) instances.

## What it is

A focused shopping list app. Mealie is the sync backend — shared lists, household sync, and authentication all come from your Mealie server. The app does one thing: give you a shopping list that is simply there when you need it, regardless of signal.

## Why

Mealie's PWA on Android fails at the two moments that matter most: if you haven't opened the app in a couple of days, the short-lived auth token expires and you get silently logged out; and anywhere mobile data is unreliable, the app shows a blank screen. These are not edge cases — they are the core use cases.

Mealie Android fixes both without compromising security. The token lifetime stays short — that's a deliberate Mealie design choice. Instead, credentials are stored securely on-device so that when the token expires, re-authentication happens silently in the background. You never see a login screen. The shopping list is fully editable without signal, and changes sync automatically when connectivity returns.

## Status

Early development - not yet usable.

## Planned v1 Features

- **Persistent authentication** - one-time setup, silent token refresh, no login prompts after server updates
- **Offline shopping list** - full read/write access without connectivity, automatic sync on reconnect
- **Two-mode shopping** - Planning mode for building your list at home; Shopping mode optimised for one-handed use in the store
- **Sync status** - unobtrusive offline indicator and per-item sync badges so you always know what is pending

## Tech Stack

- Kotlin + Jetpack Compose
- Android API 26+ (Android 8.0 Oreo)
- MVVM + Repository architecture
- Room (local storage), WorkManager (background sync), OkHttp (networking), DataStore (credential storage)

## Planning and Documentation

Product requirements and all planning artifacts live in [`_bmad-output/planning-artifacts/`](_bmad-output/planning-artifacts/):

| Artifact | Location |
| --- | --- |
| Product Brief | [briefs/brief-mealie-android-2026-05-21/brief.md](_bmad-output/planning-artifacts/briefs/brief-mealie-android-2026-05-21/brief.md) |
| PRD | [prds/prd-mealie-android-2026-05-21/prd.md](_bmad-output/planning-artifacts/prds/prd-mealie-android-2026-05-21/prd.md) |
| PRD Decision Log | [prds/prd-mealie-android-2026-05-21/.decision-log.md](_bmad-output/planning-artifacts/prds/prd-mealie-android-2026-05-21/.decision-log.md) |
| UX Design Specification | [ux-design-specification.md](_bmad-output/planning-artifacts/ux-design-specification.md) |
| Design Direction Showcase | [ux-design-directions.html](_bmad-output/planning-artifacts/ux-design-directions.html) |

### BMad

This project uses [BMad](https://github.com/bmad-agent/bmad-method) - a structured planning workflow for AI-assisted software projects running inside [Claude Code](https://claude.ai/code).

BMad provides a set of specialist agents (product manager, architect, UX designer, developer) that guide you through creating, validating, and implementing a product from brief to code. All planning artifacts in `_bmad-output/` were produced using BMad skills in Claude Code.

**To use BMad in this project:**

1. Open the project in Claude Code
2. Type `/bmad-help` to see available skills and what to do next
3. Planning artifacts are in `_bmad-output/planning-artifacts/`; implementation artifacts (stories, tasks) will appear in `_bmad-output/implementation-artifacts/` as development progresses

The `_bmad/` directory contains the BMad module configuration installed for this project. It is checked in so contributors work from the same planning workflow.

## Contributing

Contributions are welcome. The project is in early development - read the [PRD](_bmad-output/planning-artifacts/prds/prd-mealie-android-2026-05-21/prd.md) to understand scope and decisions before opening a pull request, and check the issue tracker before starting work on something significant.

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
