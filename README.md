# Consecutor

Consecutor is a local-first Android tracker for habits, recurring actions, and time-series measurements. It stores real history, derives consecutive streaks from that history, and lets users move their data with explicit CSV export and full backup import/export.

Created by Squalor, LLC.

## Features

- Tracker-based model for habits, events, and measurements
- Today-first dashboard with one-tap logging and Undo
- Editable history instead of mutable counters as the source of truth
- Daily and weekly targets, with streaks derived from history for yes/no and count trackers
- Trend charts and a monthly calendar on each tracker's detail screen
- Archive, restore, and permanent delete for trackers
- Local reminders for scheduled trackers
- Save CSV and full JSON backup files to a location you choose, or share them
- Backup import with a preview, and a safety copy of your previous data
- No accounts, no cloud sync, no ads, and no analytics

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
- The app requests no network permission
- Automatic Android backup and device transfer are disabled
- Backup/import is user-driven through files
- Reminder notifications are local only

More detail is in [PRIVACY.md](PRIVACY.md).

## Building

See [BUILDING.md](BUILDING.md).

## Roadmap

Future work, including iOS support and stronger privacy/security upgrades, lives in [ROADMAP.md](ROADMAP.md).

Release changes are listed in [CHANGELOG.md](CHANGELOG.md).

## License

`MPL-2.0`

The launcher icon and notification glyph are original artwork, licensed MPL-2.0 with the app.
