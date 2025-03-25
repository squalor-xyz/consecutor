# Consecutor: Specifications and Requirements Document

**Project Name**: Consecutor  
**Version**: 1.0  
**Date**: October 2024  
**Author**: Squalor, LLC

---

## 1. Project Overview

Consecutor is an Android application designed to assist users in tracking consecutive events and building habits. Users can create events, increment counters to log occurrences, and monitor their progress through a clean, modern interface. The app ensures data privacy by storing all information locally in an encrypted database.

Built with contemporary Android development practices, Consecutor leverages Jetpack Compose for its user interface, Room with SQLCipher for encrypted local storage, and the MVVM (Model-View-ViewModel) architecture to maintain a clear separation of concerns.

---

## 2. Features and Requirements

### 2.1 Event Creation

- **Goal**: Enable users to create new events with a name and an optional emoji for personalization.
- **Requirements**:
    - Users must provide a name for each event.
    - An optional emoji can be added to visually distinguish events.
    - New events must be saved to the local database upon creation.
- **Implementation**:
    - **User Interface**: Implemented in `AddEventDialog.kt` as a Jetpack Compose dialog featuring text fields for the event name and emoji.
    - **Logic**: Upon confirmation, the `onAdd` callback triggers the `insert` method in `EventViewModel.kt`.
    - **Data Layer**: `EventViewModel.kt` delegates the insertion to `EventRepository.kt`, which uses `EventDao.kt` to store the event in the database.
- **Files**:
    - `AddEventDialog.kt`
    - `EventViewModel.kt`
    - `EventRepository.kt`
    - `EventDao.kt`

---

### 2.2 Event Listing

- **Goal**: Display a comprehensive list of all created events with their details.
- **Requirements**:
    - The list must display each event’s name, emoji (if provided), consecutive count, total count, and last incremented date.
    - The list must dynamically update as events are added, modified, or removed.
- **Implementation**:
    - **User Interface**: Rendered in `EventListScreen.kt` using a `LazyColumn` in Jetpack Compose.
    - **Data Binding**: Observes the `allEvents` Flow from `EventViewModel.kt`, collected as state within the composable.
    - **Event Display**: Each event is shown via the `EventItem` composable (defined within `EventListScreen.kt`), presenting all relevant details.
- **Files**:
    - `EventListScreen.kt`
    - `EventViewModel.kt`

---

### 2.3 Incrementing Event Counters

- **Goal**: Allow users to increment event counters with a single tap, updating both consecutive and total counts.
- **Requirements**:
    - Tapping an event increments its total count.
    - The consecutive count increments only if the event was last incremented on the previous day; otherwise, it resets to 1.
    - The last incremented date updates to the current date.
- **Implementation**:
    - **User Interface**: In `EventListScreen.kt`, each `EventItem` is clickable, triggering the `onIncrement` callback to call `incrementEvent` in `EventViewModel.kt`.
    - **Logic**: `EventViewModel.kt` invokes `incrementEvent` in `EventRepository.kt`, which calculates the new consecutive count based on the last incremented date and updates the counters.
    - **Data Layer**: `EventRepository.kt` uses `EventDao.kt` to save the updated event.
- **Files**:
    - `EventListScreen.kt`
    - `EventViewModel.kt`
    - `EventRepository.kt`
    - `EventDao.kt`

---

### 2.4 Deleting Events

- **Goal**: Permit users to delete events using a swipe gesture.
- **Requirements**:
    - Users can swipe an event to initiate deletion.
    - A confirmation mechanism ensures accidental deletions are avoided.
- **Implementation**:
    - **User Interface**: In `EventListScreen.kt`, events are wrapped in a `SwipeToDismiss` composable.
    - **Logic**: Swiping triggers the `confirmStateChange` callback, which checks the dismiss direction and, if confirmed, calls `delete` on `EventViewModel.kt`.
    - **Data Layer**: `EventViewModel.kt` delegates to `EventRepository.kt`, which uses `EventDao.kt` to remove the event.
- **Files**:
    - `EventListScreen.kt`
    - `EventViewModel.kt`
    - `EventRepository.kt`
    - `EventDao.kt`

---

### 2.5 Exporting Data

- **Goal**: Enable users to export all events as a CSV file for sharing or backup purposes.
- **Requirements**:
    - An export option must be accessible from the UI.
    - The CSV must include all event details (ID, name, emoji, consecutive count, total count, last incremented date).
    - The file must be shareable via Android’s sharing system.
- **Implementation**:
    - **User Interface**: An `IconButton` with a share icon in `EventListScreen.kt`’s top app bar triggers `exportData` in `EventViewModel.kt`.
    - **Logic**: `EventViewModel.kt` retrieves events, generates a CSV string with `exportEventsToCsv`, writes it to a file, and shares it using `FileProvider` and an `Intent`.
- **Files**:
    - `EventListScreen.kt`
    - `EventViewModel.kt`

---

### 2.6 Encrypted Storage

- **Goal**: Securely store all user data in an encrypted local database.
- **Requirements**:
    - The database must use strong encryption.
    - The encryption passphrase should be securely managed (e.g., via Android Keystore in production).
- **Implementation**:
    - **Database Setup**: Defined in `AppDatabase.kt` with Room and SQLCipher via `SupportFactory`.
    - **Passphrase Management**: Initialized in `ConsecutorApp.kt` with a hardcoded passphrase (to be replaced with a secure key in production).
    - **Data Access**: `EventDao.kt` provides the interface for database operations, utilized by `EventRepository.kt`.
- **Files**:
    - `AppDatabase.kt`
    - `ConsecutorApp.kt`
    - `EventDao.kt`
    - `EventRepository.kt`

---

### 2.7 Theme Support

- **Goal**: Ensure a consistent and appealing visual style across the app.
- **Requirements**:
    - Support a light theme (dark theme planned).
    - Apply the theme uniformly to all UI elements.
- **Implementation**:
    - **Theme Definition**: Defined in `ConsecutorTheme.kt` with a light color scheme and typography.
    - **Usage**: Applied in `MainActivity.kt` by wrapping `EventListScreen` with `ConsecutorTheme`.
- **Files**:
    - `ConsecutorTheme.kt`
    - `MainActivity.kt`

---

## 3. Architecture and File Structure

Consecutor employs the MVVM architecture, separating the UI, business logic, and data layers for maintainability and scalability.

### 3.1 Key Files and Their Roles

- **MainActivity.kt**:
    - **Role**: App entry point, sets up Jetpack Compose UI, and provides `EventViewModel` to `EventListScreen`.
    - **Location**: `app/src/main/java/com/squalor/consecutor/MainActivity.kt`

- **EventListScreen.kt**:
    - **Role**: Main screen composable, includes event list, add event trigger, and export functionality. Contains `EventItem` composable.
    - **Location**: `app/src/main/java/com/squalor/consecutor/EventListScreen.kt`

- **AddEventDialog.kt**:
    - **Role**: Composable dialog for adding new events.
    - **Location**: `app/src/main/java/com/squalor/consecutor/ui/AddEventDialog.kt`

- **EventViewModel.kt**:
    - **Role**: Manages UI data and operations (insert, update, delete, increment, export).
    - **Location**: `app/src/main/java/com/squalor/consecutor/EventViewModel.kt`

- **EventRepository.kt**:
    - **Role**: Handles data operations and business logic, including counter increment rules.
    - **Location**: `app/src/main/java/com/squalor/consecutor/EventRepository.kt`

- **EventDao.kt**:
    - **Role**: Data Access Object for CRUD operations on the events table.
    - **Location**: `app/src/main/java/com/squalor/consecutor/EventDao.kt`

- **AppDatabase.kt**:
    - **Role**: Defines the Room database with SQLCipher encryption.
    - **Location**: `app/src/main/java/com/squalor/consecutor/AppDatabase.kt`

- **Converters.kt**:
    - **Role**: Type converters for `LocalDate` in Room.
    - **Location**: `app/src/main/java/com/squalor/consecutor/Converters.kt`

- **ConsecutorApp.kt**:
    - **Role**: Application class initializing the database and repository.
    - **Location**: `app/src/main/java/com/squalor/consecutor/ConsecutorApp.kt`

- **Event.kt**:
    - **Role**: Defines the `Event` data class, the database entity.
    - **Location**: `app/src/main/java/com/squalor/consecutor/Event.kt`

- **EventViewModelFactory.kt**:
    - **Role**: Factory for creating `EventViewModel` instances with repository dependency.
    - **Location**: `app/src/main/java/com/squalor/consecutor/EventViewModelFactory.kt`

- **ConsecutorTheme.kt**:
    - **Role**: Defines the app’s theme (colors and typography).
    - **Location**: `app/src/main/java/com/squalor/consecutor/ui/theme/ConsecutorTheme.kt`

---

## 4. Non-Functional Requirements

- **Security**: Data must be encrypted locally to protect user privacy.
- **Performance**: The app should efficiently handle up to 100 events without UI lag.
- **Usability**: The interface must be intuitive, with clear actions for all features.
- **Maintainability**: Code must be well-organized with documentation for future enhancements.

---

## 5. Future Enhancements

- **Dark Theme**: Add dark theme support with user or system-based toggling.
- **Filtering/Sorting**: Enable event filtering by folders or sorting by criteria.
- **Reminders**: Add notifications to prompt daily event increments.
- **Backup/Restore**: Implement data backup and restore functionality.

---

This document outlines Consecutor’s features, goals, and implementation details, mapping each to specific files for clarity. It serves as a foundation for understanding, maintaining, and extending the app. For additional details, please refer to the codebase or contact the development team.