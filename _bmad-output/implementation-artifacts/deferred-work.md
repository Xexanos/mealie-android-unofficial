# Deferred Work

## Deferred from: code review of 1-1-multi-module-build-infrastructure-and-ci-cd (2026-05-24)

- Timber as debugImplementation - no logging abstraction in place; any release code calling Timber.* will crash with NoClassDefFoundError. Address when adding first real logging calls.
- `v*` tag release trigger has no branch or actor guard - any collaborator can push a v* tag from any branch and trigger a signed release. Consider adding a branch-protection check or restricting tag creation.
- CI `needs: build` sequential coupling - test and lint jobs each do a full recompile on separate runners without artifact sharing. Consider Gradle remote build cache or artifact upload/restore to speed up CI.
- Missing secrets produce unsigned APK silently - acknowledged as expected in spec dev notes; address when configuring repository secrets before first release.
