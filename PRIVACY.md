# Privacy Policy

Consecutor is a local-first Android app. Your data stays on your device unless you choose to export it.

## What The App Does

- Stores your trackers, entries, targets, and reminders on the device
- Schedules local reminder notifications when you turn them on
- Lets you export your entries as a CSV file and your full data as a JSON backup file
- Lets you import a backup file that you choose

## What The App Does Not Do

- It requests no network permission, so the app itself cannot connect to the internet
- It has no accounts, no cloud sync, no analytics, and no ads
- Automatic Android backup and device-to-device transfer are disabled, so Android does not copy the app's data to Google or to a new device

## Permissions

The app requests only these Android system permissions:

- `POST_NOTIFICATIONS`, to show reminder notifications on Android 13 and later
- `RECEIVE_BOOT_COMPLETED`, to schedule your reminders again after the device restarts

An AndroidX library also adds a permission that the app defines for itself (`com.squalor.consecutor.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`). It is not an Android system permission.

## Reminders

- Reminders are local notifications scheduled on your device. Nothing is sent to a server.
- Reminders are not exact. Android may deliver one later than the time you set, especially when the device is idle.

## Exports And Backups

- Exported files are not encrypted and contain your full history. Keep them somewhere private.
- When you save an export, the system file picker lets you choose where the file is written. The app does not keep track of exported files.
- When you share an export, the app writes a temporary copy to its cache and hands it to the app you pick. The app deletes that copy the next time you share an export or open the app.
- Importing a backup replaces your current data with the contents of the backup file. If you already have trackers, the app first saves a copy of your previous data to its private storage and keeps the last three such copies there.
- The app has no screen for opening those saved copies, and uninstalling the app removes them.

## Deleting Data

- Deleting a tracker permanently removes it, its entries, and its reminder.
- Deleting an entry keeps it for 24 hours so that you can undo the deletion. It is then removed the next time the app starts. Deleted entries are never written to exports.
- Uninstalling the app removes all data it stores on the device. Files that you exported or shared are separate copies that you control.

## Links

The Settings screen has a link to the source code. When you tap it, your device opens the link in another app, such as a browser, and that app makes the network connection.

## Security Notes

- The app database is not additionally encrypted. It is protected by the Android app sandbox.
- Exported files are not encrypted or password-protected.

If encryption or password-protected backup files are added later, this policy will be updated to describe the exact behavior.

Last updated: October 2026
