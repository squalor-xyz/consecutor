# Roadmap

This file tracks future plans that are intentionally outside the current Android-first MVP.

## Near Term

- Validate the rebuilt Android MVP on device
- Tighten export format and import error handling
- Improve reminder reliability and permission UX
- Add stronger tests around backup round-tripping and tracker editing
- Refine the CSV export into a cleaner multi-file or zip-based format if needed

## Platform Direction

- Keep Android as the first shipping priority
- Extract streak logic, import/export logic, and domain models so they can move into a shared multiplatform core
- Evaluate Kotlin Multiplatform for a shared library used by Android and an eventual iOS client
- Add an iOS app once the shared model, backup format, and reminder semantics are stable enough to avoid duplicate rework

## Privacy / Security

- Evaluate password-protected backup export
- Revisit local at-rest encryption once the data model is stable
- Add clearer user messaging around exported file sensitivity

## Product

- Better charts and trend visualizations
- Tags, filtering, and richer tracker organization
- More flexible targets and schedule rules
- Safer archive / restore flows

## Licensing

- Repository license is now `MPL-2.0`
- If any future code is imported from third parties, compatibility with `MPL-2.0` needs to be checked before merging
