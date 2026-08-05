# Smart Silence
 
> Android automation app that silences your phone during class — automatically.
 
---
 
## Download
 
👉 [Latest APK](https://github.com/madhan929/smart-silence/releases/download/v5.6/app-release.apk)
 
---
 
## Features
 
- **Schedule-based automation** — Silences notifications at class start, restores sound at lunch and after college
- **Custom schedule** — Set your own start, lunch, and end times
- **Skip Today** — Skip automation for the current day without affecting future cycles
- **Morning confirmation** — Notification at 8:30 AM asking if you're attending, with a follow-up if ignored
- **Temporary Focus Mode** — On-demand silence for a fixed duration, independent of the main scheduler
- **Status card** — Shows current automation state and next scheduled action
- **Weekday-only** — Automation skips weekends automatically
- **System theme support** — Follows device dark/light theme
---
 
## Tech Stack
 
| Component | Technology |
|---|---|
| Language | Kotlin |
| Platform | Android (API 21+) |
| Scheduling | AlarmManager |
| Notifications | NotificationManager |
| UI | XML Layouts |
 
---
 
## Permissions
 
| Permission | Purpose |
|---|---|
| Notification Access | Control notification sound |
| Schedule Exact Alarm | Trigger automation at precise times |
| Background Execution | Keep scheduling active in the background |
 
---
 
## Known Limitations
 
- Aggressive OEM battery optimisations (MIUI, One UI) may delay scheduled tasks — exclude the app from battery optimisation for best results
- Automation stops if notification or alarm permissions are revoked
- Controls notification sound only — calls and alarms are unaffected
---
 
## Author
 
**Madhan Adepu** — B.Tech CSE, HITAM Hyderabad
 
