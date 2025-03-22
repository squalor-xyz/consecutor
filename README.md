# Consecutor

**Track your streaks, build your habits.**

Consecutor is an Android app that helps you track consecutive events and build lasting habits. Create events, tap to increment counters, and monitor your progress with a modern interface. All data is stored locally with encryption.

Created by Squalor, LLC.

## Features

- Create events with names and optional emojis
- Increment counters with a tap, tracking consecutive days
- View consecutive and total counts
- Export data to CSV
- Swipe to delete events
- Encrypted local storage with SQLCipher

## Installation

1. Clone the repository: `git clone https://github.com/squalor-xyz/consecutor.git`
2. Open in Android Studio
3. Build and run on your device or emulator

## Usage

- Tap the + button to add a new event
- Enter a name and optional emoji
- Tap an event to increment its counter
- Swipe left to delete an event
- Use the share icon to export data as CSV

## Building and Deployment

See [BUILDING.md](BUILDING.md) for instructions.

## License

MIT License - see [LICENSE](LICENSE)

## Privacy

See [PRIVACY.md](PRIVACY.md)

## Contributing

Pull requests are welcome! Please follow standard GitHub contribution guidelines.

## project structure

```
Consecutor/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/
│   │   │   │   └── com/squalor/consecutor/
│   │   │   │       ├── ConsecutorApp.kt
│   │   │   │       ├── data/
│   │   │   │       │   ├── db/
│   │   │   │       │   │   ├── EventDao.kt
│   │   │   │       │   │   ├── EventDatabase.kt
│   │   │   │       │   │   └── SQLCipherHelper.kt
│   │   │   │       │   ├── model/
│   │   │   │       │   │   └── Event.kt
│   │   │   │       │   └── repository/
│   │   │   │       │       └── EventRepository.kt
│   │   │   │       ├── ui/
│   │   │   │       │   ├── MainActivity.kt
│   │   │   │       │   ├── eventlist/
│   │   │   │       │   │   └── EventListFragment.kt
│   │   │   │       │   ├── newevent/
│   │   │   │       │   │   └── NewEventFragment.kt
│   │   │   │       │   └── viewmodel/
│   │   │   │       │       └── EventViewModel.kt
│   │   │   │       └── util/
│   │   │   │           └── CsvExporter.kt
│   │   │   └── res/
│   │   │       ├── layout/
│   │   │       │   ├── fragment_event_list.xml
│   │   │       │   ├── fragment_new_event.xml
│   │   │       │   └── ...
│   │   │       └── values/
│   │   │           ├── colors.xml
│   │   │           ├── strings.xml
│   │   │           └── themes.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── settings.gradle
├── LICENSE
├── README.md
├── docs/
│   ├── requirements_spec.md
│   ├── help.md
│   ├── privacy_policy.md
│   └── deployment_instructions.md
└── index.html

```
