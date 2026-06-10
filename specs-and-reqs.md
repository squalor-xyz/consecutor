# Consecutor MVP Specification

**Project Name**: Consecutor  
**Version**: 1.0 MVP
**Date**: April 2026
**Author**: Squalor, LLC

## 1. Product Goal

Consecutor helps users build better habits by tracking actions and measurements over time, deriving consecutive streaks from history, and keeping the resulting data local and portable.

The v1 MVP is intentionally lean:

- Android only
- local-first
- no accounts
- no cloud sync
- editable history
- explicit export/import
- local reminders

## 2. Core Domain Model

### 2.1 Tracker

A tracker is the thing the user wants to monitor.

Supported tracker types:

- `YES_NO`
- `COUNT`
- `MEASURE`

Trackers can have:

- name
- emoji
- description
- unit
- optional streak target
- optional reminder
- archived state

### 2.2 Entry

An entry is a dated record attached to a tracker.

Entries support:

- effective date
- optional numeric value
- optional note
- edit and soft delete

Entries are the source of truth. Streaks and totals are derived from them.

### 2.3 Target

Targets define what counts as success for streak calculations.

Supported periods:

- `DAILY`
- `WEEKLY`

### 2.4 Reminder

Reminders are per-tracker local notifications with:

- enabled state
- time of day
- optional selected weekdays

## 3. MVP Features

### 3.1 Dashboard

- list active trackers
- show current streak, longest streak, total value, and recent completion rate
- quick-log a tracker
- open tracker detail

### 3.2 Tracker Detail

- show tracker metadata
- show current and longest streak
- show recent trend
- show editable history
- add, edit, and delete entries
- archive tracker

### 3.3 Tracker Editor

- create or edit trackers
- select tracker type
- configure optional target
- configure optional reminder

### 3.4 Export / Import

- export CSV snapshot
- export full JSON backup
- import full JSON backup

Import replaces the current local state.

### 3.5 Reminders

- local-only notifications
- no remote services
- notification permission required on Android 13+

## 4. Architecture

### 4.1 Storage

- `Room` over SQLite
- entities:
  - `TrackerEntity`
  - `EntryEntity`
  - `TargetEntity`
  - `ReminderEntity`

### 4.2 App Layers

- `TrackerRepository`
  - persistence
  - import/export
  - tracker and entry mutations
- `TrackerAnalytics`
  - streak and summary calculations
- `TrackerViewModel`
  - screen state
  - user actions
- Compose UI
  - dashboard
  - detail
  - forms
  - settings

### 4.3 Dependency Strategy

- manual DI via `ConsecutorApp`
- `TrackerViewModelFactory`
- no Hilt in MVP

## 5. Privacy Requirements

- all app data stays local unless explicitly exported
- automatic Android backup disabled
- no analytics SDKs
- no ad SDKs
- no account system
- exported files are user-managed copies

## 6. Non-Goals For MVP

- cross-platform runtime sharing
- cloud sync
- collaboration
- widgets
- advanced analytics suite
- password-encrypted backup files

Cross-platform support remains on the roadmap, but the current repository and implementation are Android-first. Future plans are tracked in `ROADMAP.md`.
