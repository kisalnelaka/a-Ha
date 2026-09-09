# a-Ha: Open-Source AuDHD Android Launcher

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](file:///home/kisalnelaka/Work/a-Ha/LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20%28API%2026--33%2B%29-green.svg)]()
[![Architecture](https://img.shields.io/badge/Architecture-UDF%20%2F%20MVI%20Clean%20Compose-purple.svg)]()

**a-Ha** is an open-source, privacy-first, dopamine-neutral operating environment architected specifically for individuals with ADHD and AuDHD (Autism + ADHD). 

Rather than acting as a paternalistic screen-time blocker, **a-Ha** functions as an executive functioning exoskeleton—providing externalized working memory scaffolding, fixation-point reading acceleration, impulse friction, and sensory regulation directly through standard, non-root Android SDK APIs.

---

## 1. Architectural Blueprint & Layer Isolation

The system enforces strict Unidirectional Data Flow (UDF) and Clean MVI separation across four architectural tiers:

```
┌──────────────────────────────────────────────────────────┐
│                   Presentation Layer                     │
│  Jetpack Compose, Custom Canvas Visualizers, Circadian   │
│  Themes, Monochromatic Utility Dock, Desk Mode           │
└────────────────────────────┬─────────────────────────────┘
                             │ State / Intents
┌────────────────────────────▼─────────────────────────────┐
│                     Domain Layer                         │
│  FixationPointParser, Flowmodoro Engine, PDA Quest Board │
│  Task Decomposer, Impulse Friction Gates                 │
└────────────────────────────┬─────────────────────────────┘
                             │ Repositories / Data Drivers
┌────────────────────────────▼─────────────────────────────┐
│                      Data Layer                          │
│  Room DB (FTS4 Search), DataStore Preferences,           │
│  EncryptedSharedPreferences (Hardware Android Keystore)  │
└────────────────────────────┬─────────────────────────────┘
                             │ System Bridges
┌────────────────────────────▼─────────────────────────────┐
│                   Integration Layer                      │
│  LauncherApps API, NotificationListenerService,          │
│  UsageStatsManager Guardrails, AudioTrack Noise Engine,  │
│  DreamService (Landscape Charging StandBy)               │
└──────────────────────────────────────────────────────────┘
```

---

## 2. Core Scaffolding Specifications

### A. Fixation-Point Typography Engine
- **Algorithmic Leverage**: Single-pass $O(N)$ lexical scanner with $O(1)$ intermediate allocations. Automatically isolates lexical tokens and applies bold emphasis to the initial fixation cluster ($\lceil \text{length} \times 0.4 \rceil$) without creating substring garbage.
- **Foveal Anchoring**: Prevents wandering saccadic eye movements during high-speed app drawer and task scrolling.

### B. Monochromatic Utility Bar & App Indexing
- Bypasses algorithmic feeds by repainting device essentials directly via clean system intents:
  - `Phone`: Dispatches `Intent(Intent.ACTION_DIAL)`.
  - `Messages`: Dispatches `Intent(Intent.ACTION_VIEW, Uri.parse("sms:"))`.
  - `Navigate`: Global search queries parse locations and fire `geo:0,0?q=` directly to bypass map discovery/recommendation engines.
- Text-only app drawer querying `LauncherApps` without icon bitmap decoding, keeping idle RAM well under 120MB.

### C. Sensory & Dopamine Regulation
- **Zero Telemetry**: 100% offline, zero trackers, zero proprietary analytics.
- **Dopamine-Neutral Palettes**: Monochromatic pure black (`#000000`) for OLED power conservation and zero sensory glare, switching to circadian warm amber/red past sunset.
- **Native Ambient Noise Engine**: Zero-dependency white, pink, and brown noise generation via direct PCM buffer streaming to `android.media.AudioTrack`.

---

## 3. Hardware Prerequisites & Performance Budgets

| Metric | Target Budget | Benchmark Hardware (Nokia G50) |
|---|---|---|
| **Cold Start to Interactive UI** | `< 400ms` | Snapdragon 480 5G, 4GB RAM |
| **Idle RAM Footprint** | `< 120MB` | Zero large icon bitmap caches |
| **Background CPU Utilization** | `~0%` | Event-driven (no polling loops/wake locks) |
| **UI Rendering Rate** | Locked `60fps` / `90Hz` | Zero frame drops during drawer gestures |

---

## 4. Build & Verification Instructions

### Environment Prerequisites
- **JDK**: OpenJDK 17 or higher
- **Android SDK**: Build-Tools `34.0.0`+, Platforms `android-33` & `android-34`
- **Gradle**: 8.14 (Wrapper included)

### Local Build Setup
1. Configure Android SDK location in `local.properties`:
   ```properties
   sdk.dir=/path/to/your/android/sdk
   ```
2. Execute automated unit test suite:
   ```bash
   ./gradlew testDebugUnitTest
   ```
3. Assemble the debug launcher APK:
   ```bash
   ./gradlew assembleDebug
   ```
   Output APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## 5. Licensing

Distributed under the **GNU General Public License v3.0 (GPLv3)**. See [LICENSE](file:///home/kisalnelaka/Work/a-Ha/LICENSE) for the complete legal text.
