# Privacy Policy

Consecutor is designed to be local-first.

## What The App Does

- Stores tracker, history, target, and reminder data on the device
- Lets the user explicitly export CSV files and full JSON backup files
- Lets the user explicitly import a previously exported backup file
- Schedules local reminder notifications when the user enables them

## What The App Does Not Do In V1

- No account creation
- No background cloud sync
- No analytics SDKs
- No advertising SDKs
- No automatic Android backup

## Data Handling

- Tracker data remains on-device unless the user exports it
- Exported files are controlled by the user and should be treated as user-managed copies
- Backup import replaces the app database with the imported state

## Security Notes

- V1 prioritizes local-only storage and explicit user-controlled portability
- The Room database is not additionally encrypted in this MVP
- Automatic Android backup is disabled to avoid accidental restore conflicts and privacy surprises

If stronger at-rest encryption or password-protected backup files are added later, the privacy documentation should be updated to reflect the exact behavior.

Last updated: April 2026
