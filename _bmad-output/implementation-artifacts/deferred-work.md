# Deferred Work

## Deferred from: code review of 1-1-multi-module-build-infrastructure-and-ci-cd (2026-05-24)

- Timber as debugImplementation - no logging abstraction in place; any release code calling Timber.* will crash with NoClassDefFoundError. Address when adding first real logging calls.
- `v*` tag release trigger has no branch or actor guard - any collaborator can push a v* tag from any branch and trigger a signed release. Consider adding a branch-protection check or restricting tag creation.
- CI `needs: build` sequential coupling - test and lint jobs each do a full recompile on separate runners without artifact sharing. Consider Gradle remote build cache or artifact upload/restore to speed up CI.
- Missing secrets produce unsigned APK silently - acknowledged as expected in spec dev notes; address when configuring repository secrets before first release.

## Deferred from: code review of 1-2-app-theme-navigation-shell-and-application-class (2026-05-24)

- `applicationScope` not registered in Koin DI - the CoroutineScope in MealieApplication is a local property not exposed through DI. Expose via Koin when a consumer needs it in later stories.
- `core:ui` no explicit coroutines main dependency - NavigationManager uses MutableSharedFlow but only gets kotlinx.coroutines transitively through koin.android. Make explicit in a dependency cleanup pass.
