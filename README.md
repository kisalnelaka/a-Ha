# a-Ha — AuDHD Launcher

> A dopamine-neutral Android home screen for ADHD/Autism brains.  
> No icons. No color noise. No algorithmic manipulation. Just executive function scaffolding.

---

## About

**a-Ha** is a minimal, text-only Android launcher built specifically for the AuDHD brain. Every design decision is grounded in reducing unnecessary cognitive load while providing practical executive functioning tools.

The core thesis: your phone's home screen should be a **tool**, not a dopamine delivery system.

---

## Features

### Executive Function Scaffolding
- **Daily Anchor** — one pinned "focus of the day" field. One goal, persisted across reboots
- **QuestBoard** — max 3 concurrent tasks with AI-powered decomposition (Groq / OpenRouter / Gemini, BYOK)
- **Flowmodoro Timer** — count-up focus sessions with intuitive break scaffolding
- **Working Memory Scratchpad** — swipe-down persistent notepad, never lost between app switches

### Impulse Friction System
- **Mindful Delay Gate** — configurable 5/15/30/60s breathing pause before designated distraction apps
- **Intention Capture** — optional "I'm opening this to..." text box before launching social media
- **App Hiding** — remove apps from the drawer without uninstalling (long-press → hide)
- **Per-app Friction Level** — set delay per app independently

### Sensory Regulation
- **Ambient Soundscape** — Brown / Pink / White noise generated on-device via AudioTrack, zero network
- **Low-Spoon Mode** — reduces visual density and cognitive demands during executive fatigue
- **Circadian Tinting** — warm amber overlay after 8pm, pure black at midnight
- **AMOLED Pure Black** — true zero-pixel power draw on OLED/AMOLED panels

### Context & Glance
- **Calendar Glance** — next event within 24h via CalendarContract, no full calendar
- **Last Opened Bar** — "Last: Chrome · 4min ago" with one-tap reopen
- **Active Task Count** — live badge on homescreen showing pending quest count

### Wallpapers
- **Custom Photo Import** — pick any image from storage, apply to Home/Lock/Both
- **Wallhaven API** — minimal dark wallpaper catalog (BYOK)
- **Procedural AMOLED Generator** — on-device geometry-based dark wallpapers, no network required

### Privacy
- **Zero telemetry** — no analytics, no crash reporting, no network calls without explicit user action
- **BYOK AI** — bring your own API key; keys stored in Android Keystore (hardware-backed encryption)
- **No icon loading** — launcher indexes app metadata only, no bitmap decoding
- **On-device everything** — UsageStats, calendar, tasks all local-only

---

## Architecture

```
app/
├── data/
│   ├── local/          # Room database (tasks, scratchpad)
│   ├── model/          # AppInfo with pre-normalized searchIndex
│   └── repository/     # AppRepository, TaskRepository, WallpaperRepository,
│                       # DailyAnchorRepository, HiddenAppsRepository, FrictionRepository
├── domain/
│   ├── audio/          # SoundScapeEngine — AudioTrack noise generation
│   ├── calendar/       # CalendarGlanceProvider
│   ├── decomposer/     # TaskDecomposerEngine — multi-provider AI task decomposition
│   ├── screentime/     # ScreenTimeTintController — circadian tinting
│   ├── typography/     # FixationPointParser — bionic reading with LRU cache
│   └── usagestats/     # LastOpenedProvider — UsageStatsManager wrapper
└── presentation/
    ├── desk/           # DeskWidget — Flowmodoro timer + DeskModeDreamService
    ├── drawer/         # AppDrawer — text-only, long-press context menu
    ├── friction/       # MindfulDelayActivity — configurable breathing gate
    ├── home/           # DailyAnchorWidget, CalendarGlanceCard, LastOpenedBar
    ├── onboarding/     # OnboardingScreen — interactive first-launch walkthrough
    ├── quest/          # QuestBoard — task management with AI decomposition
    ├── scratchpad/     # ScratchpadDialog — working memory notepad
    ├── settings/       # SettingsSheet — BYOK key management, profile config
    ├── theme/          # AHaTheme — pure black AMOLED palette, monospace tokens
    └── wallpaper/      # WallpaperPickerSheet — custom + Wallhaven + procedural
```

**Stack:** Kotlin · Jetpack Compose · Room · AndroidX Security Crypto · Coroutines · R8

---

## Performance Targets

| Metric | Target | Implementation |
|--------|--------|----------------|
| Cold start | < 400ms | `singleTask` launch mode, no bitmap decoding |
| Idle RAM | < 120MB | No icon cache, no background services |
| Search latency | < 16ms | Pre-normalized `searchIndex`, LRU annotation cache |
| Clock wakeups | 1/min | Aligned tick to minute boundary |
| APK size | < 5MB | R8 + resource shrinking |

---

## Permissions

| Permission | Why |
|---|---|
| `QUERY_ALL_PACKAGES` | Index all installed apps |
| `SET_WALLPAPER` | Apply wallpapers |
| `READ_CALENDAR` | Calendar glance (one event, read-only) |
| `PACKAGE_USAGE_STATS` | Last-opened app context (special app op, opt-in) |
| `INTERNET` | BYOK AI endpoints only — user-initiated |
| `VIBRATE` | Haptic feedback for friction pacing |
| `POST_NOTIFICATIONS` | Android 13+ notification dispatch |
| `READ_EXTERNAL_STORAGE` | Custom wallpaper import (Android ≤12) |

---

## Setup

1. Install the APK — sideload or build from source
2. Go to **Settings → Default apps → Home app → a-Ha**
3. Optional: **Settings → Special app access → Usage access → a-Ha** (for Last Opened bar)
4. On first launch, complete the interactive onboarding walkthrough
5. Add your AI API key in **SETTINGS → BYOK KEYS** to unlock task decomposition

### Build from source

```bash
git clone https://github.com/[YOUR_USERNAME]/a-ha-launcher
cd a-ha-launcher
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Contributing

This is a personal-use launcher built for a specific neurotype. PRs welcome for:
- Accessibility improvements
- Additional executive function tools
- Performance optimizations
- Bug fixes

Not accepting: icon packs, gamification, social features, anything that increases dopamine manipulation.

---

## License

MIT — see [LICENSE](LICENSE)
