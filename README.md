# Consecutor

Consecutor is a local-first Android tracker for habits, recurring actions, and time-series measurements. It stores real history, derives consecutive streaks from that history, and lets users move their data with explicit CSV export and full backup import/export.

Created by Squalor, LLC.

## MVP Focus

- Tracker-based model for habits, events, and measurements
- Editable history instead of mutable counters as the source of truth
- Derived streaks for yes/no and count-based trackers
- Dashboard, tracker detail view, and recent trend view
- Local reminders for scheduled trackers
- CSV export plus full JSON backup import/export
- No accounts, no cloud sync, and no analytics SDKs

## Tracker Types

- `YES_NO` for habits like journaling, meditating, or flossing
- `COUNT` for things like pages read, glasses of water, or workouts
- `MEASURE` for values like weight, mood, or sleep hours

## Architecture

- `Room` for local persistence over SQLite
- Manual dependency wiring through `Application` and `ViewModelFactory`
- Jetpack Compose for UI
- Derived streak/trend calculations from `entries`
- File-based backup portability instead of background sync

## Privacy Model

- Data stays on-device unless the user explicitly exports it
- Automatic Android backup is disabled in v1
- Backup/import is user-driven through files
- Reminder notifications are local only

More detail is in [PRIVACY.md](PRIVACY.md).

## Building

See [BUILDING.md](BUILDING.md).

## Roadmap

Future work, including iOS support and stronger privacy/security upgrades, lives in [ROADMAP.md](ROADMAP.md).

## License

`MPL-2.0`
