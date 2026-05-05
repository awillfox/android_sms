# SMSNote

Android app that captures SMS and notifications from the device and POSTs them to a remote archive API. Displays archived records fetched from that same API.

**API base URL:** `https://smsarchive.tunabox.work`

---

## Requirements

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog or newer |
| JDK | 17+ (bundled with Android Studio) |
| Android SDK | API 36 (compileSdk) |
| AGP | 9.2.0 |
| Gradle | 9.4.1 (via wrapper) |
| Target device / emulator | Android 6.0+ (minSdk 23) |

> **Note:** The Kotlin plugin is bundled inside AGP 9.2.0. Do **not** add `alias(libs.plugins.kotlin.android)` or `kotlinOptions {}` — both break the build.

---

## Clone

```bash
git clone <repo-url>
cd SMSNote
```

---

## Build

### Windows

```powershell
.\gradlew.bat assembleDebug
```

### Linux / macOS

```bash
./gradlew assembleDebug
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## Deploy to device

Connect an Android device via USB with USB debugging enabled (or start an emulator).

### Windows

```powershell
# Build + install + launch
.\gradlew.bat installDebug
adb shell am start -n com.shipnity.smsnote/.MainActivity

# Install only (skip rebuild)
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Stream logs
adb logcat -s SMSNote
```

### Linux / macOS

```bash
# Build + install + launch
./gradlew installDebug
adb shell am start -n com.shipnity.smsnote/.MainActivity

# Install only (skip rebuild)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Stream logs
adb logcat -s SMSNote
```

---

## First-run setup (on device)

Two permissions require manual action — runtime dialogs alone are not enough:

1. **SMS** — grant `RECEIVE_SMS` / `READ_SMS` when prompted on launch.
2. **Notification listener** — app opens Settings → Notification Access automatically if missing; toggle **SMSNote** on.

The app will not capture SMS or notifications until both are granted.

---

## Architecture

```
SMS received        →  SmsReceiver (BroadcastReceiver)             →  ApiClient.postSms()
Notification posted →  AppNotificationListenerService              →  ApiClient.postNotification()
UI polling          →  ApiClient.getSmsMessages() / getNotifications()  →  RecyclerView (every 5s)
```

### Key files

| File | Role |
|------|------|
| `ApiClient.kt` | OkHttp singleton; all HTTP calls |
| `Models.kt` | `SmsMessage`, `Notification`, `ApiResponse<T>` data classes |
| `ArchiveService.kt` | Foreground keep-alive service; survives "Close All" via AlarmManager restart |
| `SmsReceiver.kt` | `RECEIVE_SMS` broadcast → API post |
| `AppNotificationListenerService.kt` | Notification listener → API post; deduplicates by `package:id:postTime` |
| `BootReceiver.kt` | Restarts `ArchiveService` after reboot |
| `MainActivity.kt` | ViewPager2 host; requests permissions; prompts for notification listener |
| `SmsFragment.kt` | Polls SMS list every 5s; swipe-to-refresh |
| `NotificationsFragment.kt` | Polls notifications list every 5s; swipe-to-refresh |

### Background persistence

`ArchiveService` runs as a foreground service with `stopWithTask="false"`. On `onTaskRemoved` it schedules a 1-second AlarmManager restart, so it survives "Close All" in recents. Only **Settings → Force Stop** kills it permanently.

### Permissions declared

| Permission | Purpose |
|-----------|---------|
| `INTERNET` | API calls |
| `RECEIVE_SMS` / `READ_SMS` | Capture incoming SMS |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` | Keep archive service alive |
| `POST_NOTIFICATIONS` | Foreground service notification (Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | Restart service after reboot |

---

## API contract

All responses use envelope:

```json
{ "data": <any | null>, "error": <string | null> }
```

HTTP status is always `200`. Clients must inspect the `error` field.

See [`ENDPOINT.md`](ENDPOINT.md) for full endpoint documentation.

---

## Dependencies

| Library | Version |
|---------|---------|
| AndroidX AppCompat | 1.7.1 |
| AndroidX Core KTX | 1.18.0 |
| Material Components | 1.13.0 |
| Fragment KTX | 1.8.6 |
| ViewPager2 | 1.1.0 |
| RecyclerView | 1.4.0 |
| SwipeRefreshLayout | 1.1.0 |
| OkHttp | 4.12.0 |
| Gson | 2.11.0 |
