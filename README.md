# Weather App

An Android weather app built with Kotlin and Jetpack Compose. Searches current conditions by city name or GPS location using the [OpenWeatherMap API](https://openweathermap.org/api).

---

## Features

- Search weather by city name
- Auto-detect location via GPS (FusedLocationProvider)
- Auto-load last searched city on launch
- Offline detection with immediate user-facing message
- Retry failed searches without retyping
- Weather icons cached to disk — no re-fetching across sessions
- Handles all API error states (invalid key, city not found, rate limit, server errors)

---

## Tech stack

| Concern | Library |
|---|---|
| UI | Jetpack Compose + Material3 |
| Architecture | MVVM — ViewModel, Repository, data models |
| Networking | OkHttp (no Retrofit) |
| JSON parsing | `org.json.JSONObject` (built into Android) |
| Location | FusedLocationProvider (Google Play Services) |
| Icon caching | Custom file cache (`Context.cacheDir`) |
| State management | `StateFlow` + `collectAsStateWithLifecycle` |
| Local persistence | `SharedPreferences` |
| Async | Kotlin coroutines |

No third-party serialisation, image-loading, or DI libraries.

---

## Setup

### 1. Get an API key

Sign up at [openweathermap.org](https://openweathermap.org) and copy your API key from the dashboard.

### 2. Add the key to the build

Open `app/build.gradle.kts` and replace the placeholder:

```kotlin
buildConfigField("String", "OPENWEATHER_API_KEY", "\"YOUR_KEY_HERE\"")
```

> In a production app you would load this from `local.properties` or a CI secret rather than committing it to source control.

### 3. Open in Android Studio

Open the project root in Android Studio (Hedgehog or newer). Let Gradle sync complete, then run on a device or emulator with API 24+.

### 4. Generate launcher icons (optional)

The repo ships with XML vector launcher icons that compile and render correctly. For crisp density-specific PNGs, right-click `app/src/main/res` → **New → Image Asset** in Android Studio and replace the mipmap directories.

---

## Project structure

```
app/src/main/java/com/example/weatherapp/
├── data/
│   ├── model/
│   │   ├── NetworkResult.kt        # Success / Error / Loading sealed wrapper
│   │   ├── WeatherResponse.kt      # Top-level API response, fromJson() parser
│   │   ├── WeatherCondition.kt     # Weather group, icon code, description
│   │   ├── WeatherMain.kt          # Temperatures, humidity, pressure
│   │   └── Wind.kt                 # Speed, optional direction
│   ├── remote/
│   │   ├── WeatherApiService.kt    # OkHttp wrapper, fetchByCity / fetchByCoordinates
│   │   └── WeatherApiException.kt  # HTTP error codes → readable messages
│   └── repository/
│       ├── WeatherRepository.kt    # Interface
│       └── WeatherRepositoryImpl.kt # Network + offline check + error mapping
├── ui/
│   ├── screen/
│   │   ├── SearchScreen.kt         # City input, loading state, error card + retry
│   │   └── WeatherDetailScreen.kt  # Icon, temp, stats grid, sunrise/sunset
│   └── viewmodel/
│       ├── WeatherUiState.kt       # Idle / Loading / Success / Error
│       └── WeatherViewModel.kt     # StateFlow state, retry, auto-load, prefs save
├── util/
│   ├── LocationHelper.kt           # FusedLocation → suspend functions
│   ├── ImageCacheHelper.kt         # Disk cache with atomic temp-file-rename writes
│   ├── NetworkMonitor.kt           # ConnectivityManager — fast offline check
│   └── PrefsHelper.kt              # SharedPreferences — last searched city
└── MainActivity.kt                 # Activity + WeatherApp root composable
```

---

## Architecture

```
MainActivity
    │
    ├── WeatherViewModel  (survives rotation)
    │       ├── WeatherRepository
    │       │       ├── WeatherApiService   (OkHttp)
    │       │       └── NetworkMonitor
    │       └── PrefsHelper
    │
    ├── LocationHelper    (GPS → suspend fun)
    └── ImageCacheHelper  (disk icon cache)
```

**Data flow:**
1. User types a city → `ViewModel.searchByCity()` → `Repository.getWeatherByCity()`
2. Repository checks connectivity first, then calls `WeatherApiService`
3. Raw JSON is parsed by `WeatherResponse.fromJson()` into model classes
4. `NetworkResult.Success` or `NetworkResult.Error` flows back to the ViewModel
5. ViewModel updates `uiState: StateFlow<WeatherUiState>`
6. Compose collects it and renders `SearchScreen` or `WeatherDetailScreen`

---

## Auto-load priority on launch

```
Has GPS permission?
├── Yes → try FusedLocation
│         ├── Got fix  → searchByCoordinates()
│         └── No fix   → autoLoadLastCity()
└── No  → autoLoadLastCity()
            └── No saved city → empty search screen
```

---

## Error surface

| Situation | Message shown |
|---|---|
| Device offline (before call) | "No internet connection. Check your network and try again." |
| Network drops mid-call | "Could not reach the weather service. Check your connection and try again." |
| City not found (HTTP 404) | "City not found. Try a different city name." |
| Invalid API key (HTTP 401) | "Invalid API key. Check your OpenWeatherMap credentials." |
| Rate limited (HTTP 429) | "Too many requests. Please wait a moment and try again." |
| Server error (HTTP 5xx) | "Weather service is unavailable. Try again later." |
| Malformed JSON | "Received unexpected data from the weather service. Please try again." |

All errors show an inline card with **Dismiss** and **Retry** buttons. Retry replays the exact last search (city name or coordinates) without requiring the user to retype anything.

---

## Icon caching

Icons are fetched from the OpenWeatherMap CDN (`https://openweathermap.org/img/wn/{code}@2x.png`) and written to `Context.cacheDir/weather_icons/{code}.png`.

Writes use a **temp-file-then-rename** pattern so a partially-written file is never left on disk if the process is killed mid-write. Because icon codes are deterministic (same code → same image forever), cached entries never expire. The OS will evict them under storage pressure as normal cache files.

---

## Known limitations / TODOs

- No Hilt DI — helpers are constructed manually in `Factory` / `lazy {}`. All `TODO:` comments in the code call this out.
- No unit tests (skipped in this version).
- Launcher icons are XML vector drawables. Density-specific PNGs would look sharper on older devices.
- `SavedStateHandle` is not used — if the process is killed while the detail screen is showing, the app restores to the search screen rather than the last result. This is acceptable for a current-conditions app since the data would be stale anyway.
- Wind direction is shown as an 8-point cardinal string. A compass rose would be a natural next step.
