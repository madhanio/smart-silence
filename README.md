# Smart Silence

> An Android automation utility that silences your phone during class — automatically.

---

## 🚀 Quick Overview

Motivation: Built to solve classroom disruptions caused by unsilenced phones.

Smart Silence is an Android automation app that automatically silences notification sounds during college hours based on a customizable schedule.

---

## APK Download

👉 [Download Latest APK](https://github.com/madhan929/smart-silence/releases/download/v5.5/app-release.apk)

---

## Features

- **Schedule-based automation** — Silences notifications during class hours and restores sound during lunch break and after college, following user-configured timings.
- **Custom schedule configuration** — Users can set their own college start time, lunch break time, and college end time.
- **Skip Today** — Allows users to skip automation for the current day only, without affecting future scheduled cycles.
- **Morning confirmation notification** — At 8:30 AM, the app sends a reminder asking whether the user is attending college that day, with a follow-up if the first notification is ignored.
- **Temporary Focus Mode** — Silences notifications for a user-selected duration (30 minutes, 1 hour, 2 hours, etc.) independently of the main automation scheduler.
- **Status card** — Displays current automation state and the next scheduled action on the home screen.
- **System theme support** — Automatically switches between dark and light themes based on the device's default theme setting.
- **Weekday-only execution** — Automation runs only on weekdays; weekend execution is explicitly excluded.
- **Hidden debug tools** — Developer testing tools (manual silence/normal/notification triggers) are accessible via a long-press gesture on the footer, keeping the production UI clean.

---

## Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| Platform | Android (API 21+) |
| Scheduling | Android AlarmManager |
| Notifications | Android NotificationManager |
| UI | XML Layouts — Card-based, minimal design |
| Theme | System-adaptive Dark / Light |

---

## Architecture Overview

- **Language:** Kotlin (Android)
- **Scheduling:** Android AlarmManager for time-based trigger execution
- **Background Execution:** Foreground service or equivalent mechanism to maintain automation reliability under Android's background restrictions
- **Notification System:** Android NotificationManager for morning confirmation and follow-up alerts
- **Persistence:** User schedule and automation state stored locally on device
- **UI:** Single-screen card-based layout with a status card at the top and settings below

### Automation Flow

```
App Launch
    └── Automation Toggle ON
            └── Schedule registered with AlarmManager
                    ├── Class Start Time  → Set notification sound: SILENT
                    ├── Lunch Break Time  → Set notification sound: NORMAL
                    └── College End Time  → Automation cycle complete

Morning Check
    └── Send confirmation notification
            ├── User confirms attending → Automation proceeds
            ├── User skips today       → Skip Today logic activates (current day only)
            └── No response            → Follow-up notification sent
```

---

## Technical Challenges

| Challenge | How It Was Addressed |
|---|---|
| Skip Today state conflict | Isolated Skip Today flag to affect only the current day's execution cycle, not the global automation state |
| DND causing call rejections | Replaced DND-based approach with direct notification sound control to avoid unintended side effects |
| Weekend automation misfiring | Added explicit weekday checks in the scheduling logic before executing any automation actions |
| Focus Mode and scheduler conflict | Focus Mode operates on its own independent timer and does not modify automation state, preventing permanent override |
| Android background execution limits | Automation registered through AlarmManager with appropriate permissions to maintain execution reliability |
| Debug tools in production UI | Moved all debug triggers behind a long-press gesture on the footer, invisible to regular users |
| Permission behaviour across devices | Handled differences in alarm and notification permission behaviour across Android versions and OEM customisations |

---

## Version Evolution

### v1.0 — Core Engine
- Implemented core automation engine with an automation toggle.
- Hardcoded college timings for initial proof of concept.
- Requested Do Not Disturb (DND) permission for sound control.
- Also handled notification access and alarm scheduling permissions.
- Introduced iOS-inspired dark minimal UI.

### v2.0 — Refinement & Custom Schedules
- Removed Do Not Disturb mode. DND caused unintended call rejection on many devices, making it unsuitable as the primary mechanism.
- Automation logic narrowed to control notification sound only.
- Removed media volume auto-control.
- Introduced custom schedule configuration (start time, lunch time, end time).
- Introduced Skip Today feature.
- Fixed a state conflict where Skip Today was incorrectly disabling future automation cycles instead of only the current day.

### v3.0 — Morning Confirmation System
- Added morning confirmation notification at 8:30 AM to check college attendance.
- Added follow-up notification for cases where the first prompt is ignored.
- Added internal debug tools for manually triggering silence mode, normal mode, and notifications during development and testing.

### v4.0 — UI and Reliability
- Introduced status card on the home screen showing current automation state and next action.
- Fixed weekend automation bug where the scheduler was incorrectly firing on Saturdays and Sundays.
- Improved time schedule execution reliability.
- Moved debug tools out of the main UI; accessible only via long-press on the footer.

### v5.0 — Focus Mode & Theming
- Added Temporary Focus Mode for on-demand notification silencing for a fixed duration.
- Focus Mode operates independently and does not interfere with or override the main automation scheduler.
- Implemented system theme auto-detection; app switches between dark and light themes based on device setting.

---

## Permissions Used

| Permission | Purpose |
|---|---|
| Notification Access | Read and control notification sound behaviour |
| Schedule Exact Alarm | Trigger automation at precise times using AlarmManager |
| Background Execution | Keep scheduling active while the app is not in the foreground |

---

## Known Limitations

- **Android background restrictions:** Automation reliability depends on the device manufacturer's background execution policies. Some aggressive OEM battery optimisations (e.g., MIUI, One UI) may delay or kill scheduled tasks unless the app is excluded from battery optimisation.
- **Permission dependency:** If the user revokes notification access or alarm scheduling permission after setup, automation will stop functioning until permissions are re-granted.
- **No system-level DND integration:** The app controls notification sound, not system DND. Calls and alarms are not affected by the automation.

---

## Future Scope

> The following are planned ideas and are **not part of the current implementation.**

- **Timetable extraction from image or text** — Allow users to photograph or paste their college timetable and have the app automatically parse and configure the schedule, eliminating manual time entry.

---

## Author

**Madhan Adepu**
Second Year B.Tech — Computer Science (CSD)
Hyderabad Institute of Technology & Management (HITAM)

---

⭐ This project demonstrates real-world Android skills including background scheduling, permission handling, state management, and user-centric automation design.
