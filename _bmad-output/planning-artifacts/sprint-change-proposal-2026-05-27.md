# Sprint Change Proposal: Add Multilingual/i18n Support

**Date:** 2026-05-27
**Triggered by:** Project owner requirement (no specific story trigger)
**Change scope:** Minor

---

## Section 1: Issue Summary

The app has no internationalization infrastructure. All user-facing strings are hardcoded in English. The project owner wants the app to support multiple UI languages, starting with English (default) and German for v1. Android's standard resource system (`res/values-XX/strings.xml`) provides this capability with zero additional dependencies, but the pattern must be established and existing code retrofitted.

**Why now:** The app is early in implementation (3 UI stories completed). Establishing the i18n pattern now is trivial; retrofitting after Epic 2 would be significantly more expensive and error-prone.

---

## Section 2: Impact Analysis

### Epic Impact

| Epic | Impact | Details |
| --- | --- | --- |
| Epic 1 | Low | Add story 1-4a (string extraction). Update implementation notes for stories 1-5+. No stories removed or reordered. |
| Epic 2 | None (structural) | All stories still in backlog. Implementation notes updated to require string resources. No code changes needed. |

### Artifact Conflicts

| Artifact | Changes Required |
| --- | --- |
| PRD | Add NFR-20 (Localization). Add non-goal (no in-app language toggle). |
| Architecture | Add "String Resources & Localization" subsection to Implementation Patterns. |
| UX Specification | None - designs are language-agnostic; English copy becomes source strings. |
| Epics | Add NFR-20 to requirements inventory. Add story 1-4a. Add localization to Additional Requirements. Add i18n notes to both epic implementation notes. |
| CI/CD | None - Android Lint's `MissingTranslation` check is already enabled by default. |
| Testing | None - no locale-switching UI tests for v1. |

### Technical Impact

- No new dependencies or libraries
- No module structure changes
- No schema or API changes
- Strings centralized in `:core:ui` module (dependency already exists from all feature modules)

---

## Section 3: Recommended Approach

**Selected:** Direct Adjustment

**Rationale:** This is a clean additive change. The app has minimal UI (3 stories with screens). Extraction is trivial today and prevents costly retrofit later. No rollback or MVP scope reduction needed - i18n adds no new features, it makes existing features available in two languages.

**Effort:** Low
**Risk:** Low
**Timeline impact:** One small story added (1-4a). Negligible impact on overall delivery.

---

## Section 4: Detailed Change Proposals

### PRD Changes

1. **Add NFR-20 (Localization)** to Cross-Cutting NFRs section: English (default) + German for v1, system locale, no in-app toggle, contributable via resource directories.
2. **Add non-goal** to Section 5: "No in-app language selector" with explanation of system locale and API 33+ per-app language support.

### Architecture Changes

3. **Add "String Resources & Localization" subsection** to Implementation Patterns: centralizes strings in `:core:ui`, defines naming convention (snake_case, screen-prefixed), parameterized strings rule, locale resolution note.

### Epics Changes

4. **Add NFR-20** to Requirements Inventory.
5. **Add Story 1-4a** after Story 1-4: dedicated extraction story covering stories 1-2 through 1-4, establishing the pattern, creating both English and German resource files.
6. **Add Localization** to Additional Requirements section.
7. **Add i18n implementation note** to Epic 1 (stories 1-5+).
8. **Add i18n implementation note** to Epic 2 (all stories).

---

## Section 5: Implementation Handoff

**Change scope classification:** Minor - direct implementation by Developer agent.

**Handoff plan:**

| Step | Who | Action |
| --- | --- | --- |
| 1 | Developer (you) | Apply the 8 document edits above to planning artifacts |
| 2 | Developer | Run `bmad-prd` in edit mode to apply PRD changes (or edit directly) |
| 3 | Developer | When story 1-4 review is complete, create story 1-4a via `bmad-create-story` |
| 4 | Developer | Implement story 1-4a (extract strings, add German translations) |
| 5 | Developer | Continue with story 1-5+ using string resources from the start |

**Success criteria:**
- No hardcoded user-facing strings in any Compose code
- `values/strings.xml` and `values-de/strings.xml` exist in `:core:ui`
- App displays German UI when device locale is set to German
- All subsequent stories include string resource entries in their acceptance criteria
