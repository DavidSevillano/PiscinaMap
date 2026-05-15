# UI Tests Design — PiscinaMap

**Date:** 2026-05-15  
**Status:** Approved

---

## Scope

Implement Compose UI tests covering the three main features (Home, Explore, Detail) plus shared components. Google Maps is excluded from direct testing; Home coverage focuses on non-map UI elements.

---

## Architecture

### File structure

```
app/src/androidTest/java/com/burixer85/piscinamap/
├── HiltTestRunner.kt              ← custom runner required for @HiltAndroidTest
├── ui/
│   ├── components/
│   │   ├── PoolListCardTest.kt
│   │   ├── PoolDetailCardTest.kt
│   │   └── ReviewCardTest.kt
│   ├── screens/
│   │   ├── ExploreScreenTest.kt
│   │   └── DetailScreenTest.kt
│   └── navigation/
│       └── NavigationTest.kt
├── di/
│   └── FakeRepositoryModule.kt
└── util/
    └── TestData.kt
```

`HiltTestRunner` extends `AndroidJUnitRunner` and replaces the application with `HiltTestApplication`. Must be set as `testInstrumentationRunner` in `build.gradle.kts`:

```kotlin
// app/build.gradle.kts — defaultConfig block
testInstrumentationRunner = "com.burixer85.piscinamap.HiltTestRunner"
```

### New dependency to add

```kotlin
// app/build.gradle.kts
androidTestImplementation("com.google.dagger:hilt-android-testing:2.51.1")
kspAndroidTest("com.google.dagger:hilt-android-compiler:2.51.1")
```

---

## Components: Test approach

Use `createComposeRule()`. No Hilt. Pass `Pool`/`Review` objects directly from `TestData`.

### `PoolListCardTest`
- Shows pool name
- Shows formatted rating
- Shows address when present

### `PoolDetailCardTest`
- Shows name, rating, address
- Shows service chips
- Shows opening hours section
- Shows reviews section
- "Llamar" and "Cómo llegar" buttons are displayed and clickable

### `ReviewCardTest`
- Shows author name
- Shows review text
- Shows star rating

---

## Screens: Test approach

Use `createAndroidComposeRule<ComponentActivity>()` with `@HiltAndroidTest`. Rules ordered: `HiltAndroidRule(order=0)`, compose rule `(order=1)`.

Location permissions must be granted before each test via `GrantPermissionRule(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)` as a JUnit `@Rule`.

### `ExploreScreenTest` — 5 scenarios
1. **Loading state** — shimmer placeholders visible while `isLoading=true`
2. **Loaded list** — pool cards with names from `TestData` visible
3. **Error state** — error message visible
4. **Empty state** — empty list message visible
5. **Exit dialog** — back press shows `ExitConfirmationDialog`

### `DetailScreenTest` — 5 scenarios
1. **Loading state** — loading indicator visible
2. **Loaded detail** — pool name and address visible
3. **Error + retry** — error message and "Reintentar" button visible and clickable
4. **Share button** — present and clickable
5. **Hide pool** — menu icon visible and "Ocultar piscina" option accessible

---

## Navigation: Test approach

Use `createAndroidComposeRule<MainActivity>()` with `@HiltAndroidTest` and fake repos returning controlled data.

### `NavigationTest` — 2 scenarios
1. **Explore → Detail** — tapping a pool card in the Explore list navigates to Detail screen showing the pool name (Home→Detail excluded: requires Maps interaction)
2. **Bottom bar Explore** — tapping "Explorar" tab from Home shows the Explore screen

---

## Fake data layer

### `TestData.kt`

Singleton object with:
- `pool`: single `Pool` with all fields populated (id, name, lat/lng, rating, address, photoUrls, reviews, openingHours, services)
- `poolList`: list of 3 `Pool` objects
- `review`: single `Review`

### `FakeRepositoryModule.kt`

`@TestInstallIn(components = [SingletonComponent::class], replaces = [NetworkModule::class])` — replaces the single real DI module. Binds fake implementations for:
- `PoolRepository` (home feature)
- `ExploreRepository`
- `DetailRepository`

Each fake exposes a `var result` that tests override via `@Inject` to control success/error/empty states.

---

## Constraints

- Google Maps composable is **not** tested — it requires a real API key and GPS, making it unsuitable for CI.
- Home screen's `PoolSearchBar` and `SearchAreaButton` are candidates for future component-level tests once this baseline is established.
- AdMob native ads (`NativeAdCard`) are excluded from UI tests — they depend on the AdMob SDK loading real ads.
- Tests run on API 26+ (minSdk from build config).
