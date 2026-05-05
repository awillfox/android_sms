# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Deploy

```bash
# Build debug APK
./gradlew assembleDebug

# Full deploy to connected device (build + install + launch)
task run

# Install only (no rebuild)
task install

# Stream app logs
task logs
```

The Kotlin plugin is NOT explicitly applied in `build.gradle.kts` — AGP 9.2.0 bundles Kotlin internally. Do not add `alias(libs.plugins.kotlin.android)` or `kotlinOptions {}` blocks; both cause build failures.

## Architecture

Internal-use app. Captures SMS and Android notifications, POSTs them to a remote API, and displays archived records fetched from that same API.

**Base URL:** `https://smsarchive.tunabox.work`  
**API contract:** see `ENDPOINT.md` — all responses use `{"data": ..., "error": ...}` envelope; HTTP status is always 200, clients must check `error` field.

### Data flow

```
SMS received  →  SmsReceiver (BroadcastReceiver)  →  ApiClient.postSms()
Notification posted  →  AppNotificationListenerService  →  ApiClient.postNotification()
UI polling  →  ApiClient.getSmsMessages() / getNotifications()  →  RecyclerView (every 5s)
```

### Background persistence

`ArchiveService` is a foreground service with `stopWithTask="false"`. On `onTaskRemoved`, it schedules a 1-second `AlarmManager` restart so it survives "Close All" in recents. Only Settings → Force Stop kills it permanently. `BootReceiver` restarts it after reboot.

### Key files

| File | Role |
|---|---|
| `ApiClient.kt` | OkHttp singleton; all HTTP calls |
| `Models.kt` | `SmsMessage`, `Notification`, `ApiResponse<T>` data classes |
| `ArchiveService.kt` | Foreground keep-alive service |
| `SmsReceiver.kt` | `RECEIVE_SMS` broadcast → API post |
| `AppNotificationListenerService.kt` | Notification listener → API post; deduplicates by `package:id:postTime` key |
| `MainActivity.kt` | ViewPager2 host; requests SMS + `POST_NOTIFICATIONS` permissions; prompts for notification listener access |
| `SmsFragment` / `NotificationsFragment` | Poll API every 5s via `Handler`; `SwipeRefreshLayout` for manual refresh |

### First-run requirements

Two permissions require user action that cannot be granted via runtime dialog alone:

1. **SMS** — `RECEIVE_SMS` / `READ_SMS`: standard runtime permission dialog (auto-prompted).
2. **Notification listener** — must be enabled in Settings → Notification Access. `MainActivity` detects if missing and opens `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`.
