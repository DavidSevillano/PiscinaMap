# Firebase Crashlytics + Analytics — Design Spec
**Date:** 2026-05-15
**App:** PiscinaMap (`com.burixer85.piscinamap`)

## Goal

Integrate Firebase Crashlytics and Firebase Analytics to observe production behavior: crashes, ANRs, non-fatal errors, and key user interactions.

## Approach: `AnalyticsManager` wrapper with Hilt (Option B)

A thin interface isolates all Firebase calls. Hilt provides the real implementation in release builds and a no-op in debug builds via Gradle source sets — no runtime `BuildConfig.DEBUG` checks.

## Architecture

### New files

```
app/src/main/java/com/burixer85/piscinamap/core/analytics/
    AnalyticsManager.kt              ← interface
    FirebaseAnalyticsManager.kt      ← real implementation
    NoOpAnalyticsManager.kt          ← silent implementation for debug

app/src/main/java/com/burixer85/piscinamap/core/di/
    AnalyticsModule.kt               ← abstract Hilt module (declares @Binds)

app/src/release/java/com/burixer85/piscinamap/core/di/
    AnalyticsModuleImpl.kt           ← binds FirebaseAnalyticsManager

app/src/debug/java/com/burixer85/piscinamap/core/di/
    AnalyticsModuleImpl.kt           ← binds NoOpAnalyticsManager
```

### `google-services.json`
Placed at `app/google-services.json` (already obtained from Firebase Console).

## Interface

```kotlin
interface AnalyticsManager {
    fun trackScreen(screenName: String)
    fun trackEvent(name: String, params: Map<String, String> = emptyMap())
    fun logNonFatalError(throwable: Throwable)
}
```

## Events catalog

| Event name | Triggered in | Parameters |
|---|---|---|
| `screen_view` (via `trackScreen`) | Each ViewModel on load | `screen_name` |
| `pool_detail_viewed` | `DetailViewModel` | `pool_id`, `pool_name` |
| `favorite_added` | `FavoritesViewModel` | `pool_id` |
| `favorite_removed` | `FavoritesViewModel` | `pool_id` |
| `pool_hidden` | `HomeViewModel` | `pool_id` |
| `pool_unhidden` | `HomeViewModel` | `pool_id` |
| `search_performed` | `HomeViewModel` | `query` |
| `map_area_searched` | `ExploreViewModel` | `latitude`, `longitude` |

## Crashlytics

- **Automatic:** unhandled crashes, ANRs.
- **Non-fatal errors:** logged via `analytics.logNonFatalError(e)` in repository `catch` blocks (`PoolRepositoryImpl`, `DetailRepositoryImpl`, `ExploreRepositoryImpl`).

## Dependencies

Added to `gradle/libs.versions.toml`:

```toml
[versions]
firebase-bom = "33.7.0"

[libraries]
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebase-bom" }
firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics-ktx" }
firebase-crashlytics = { group = "com.google.firebase", name = "firebase-crashlytics-ktx" }

[plugins]
google-services = { id = "com.google.gms.google-services", version = "4.4.2" }
firebase-crashlytics-plugin = { id = "com.google.firebase.crashlytics", version = "3.0.2" }
```

Applied in `build.gradle.kts` (root) and `app/build.gradle.kts`.

## Build variant wiring

Gradle source sets `release/` and `debug/` contain separate `AnalyticsModuleImpl.kt` files. Both implement the same `@Module @InstallIn(SingletonComponent::class)` but bind different classes. This is the idiomatic way to swap implementations per build type without runtime checks.

## ViewModel integration pattern

```kotlin
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val analytics: AnalyticsManager,
    // existing deps...
) : ViewModel() {
    fun onPoolLoaded(pool: Pool) {
        analytics.trackScreen("DetailScreen")
        analytics.trackEvent("pool_detail_viewed", mapOf(
            "pool_id" to pool.id,
            "pool_name" to pool.name
        ))
    }
}
```

## Out of scope

- Firebase Remote Config
- Firebase Performance Monitoring
- Push notifications
