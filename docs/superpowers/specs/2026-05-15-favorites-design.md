# Favorites Feature Design

**Date:** 2026-05-15  
**Status:** Approved  
**Approach:** Option A — follow existing HiddenPoolsManager pattern

---

## Overview

Users can mark pools as favorites. The feature includes a persistent toggle accessible from the map card and the detail screen, a special map marker for favorite pools, a new "Favoritos" tab in the bottom nav bar, and real-time cross-screen synchronization.

---

## 1. Persistence & Synchronization

### FavoritesManager

New singleton object at `core/presentation/util/FavoritesManager.kt`, mirroring `HiddenPoolsManager` exactly:

- **SharedPreferences name:** `"favorite_pools"` (private mode)
- **Key:** `"favorite_pool_ids"` (StringSet)
- **Methods:**
  - `isFavorite(context, poolId): Boolean`
  - `addFavorite(context, poolId)`
  - `removeFavorite(context, poolId)`
  - `getFavoriteIds(context): Set<String>`

### PoolStateManager extension

A second set of listeners is added to `PoolStateManager` for favorite state changes:

- New listener type: `(poolId: String, isFavorite: Boolean) -> Unit`
- New public list: `favoriteListeners`
- New methods: `subscribeFavorite(listener)`, `unsubscribeFavorite(listener)`, `emitFavoriteStateChange(poolId, isFavorite)`

### Pool domain model

`isFavorite: Boolean = false` is added to the `Pool` data class. Repository implementations (PoolRepositoryImpl, ExploreRepositoryImpl, DetailRepositoryImpl) populate this field using `FavoritesManager.isFavorite(context, pool.id)` when mapping DTOs to domain models — the same pattern used for `isHidden`.

---

## 2. UI — Favorite Toggle

### DetailScreen

- An icon button with `Icons.Default.Favorite` (filled, colored) / `Icons.Default.FavoriteBorder` (outline) is added to the top app bar or action row, always visible.
- State is held locally: `var isFavorite by remember { mutableStateOf(FavoritesManager.isFavorite(context, poolId)) }`.
- On tap: calls `FavoritesManager.addFavorite/removeFavorite`, then `PoolStateManager.emitFavoriteStateChange(poolId, newState)`, then updates local state.

### PoolDetailCard (Home map card)

- The same heart icon is added to the card shown at the bottom of the map when the user taps a marker.
- Same toggle logic as DetailScreen.
- `PoolDetailCard` receives an `isFavorite: Boolean` and an `onFavoriteToggle: () -> Unit` callback.

### Map markers (HomeScreen)

- `HomeViewModel` subscribes a new listener to `PoolStateManager.subscribeFavorite(...)` in `init`, unsubscribes in `onCleared()`.
- On receiving a favorite change event, the ViewModel updates the matching `Pool.isFavorite` in `MapUiState.pools`.
- The marker bitmap selector adds a new case: if `pool.isFavorite == true` → a new golden/starred vector drawable is used as the marker icon.
- A new vector drawable `ic_marker_favorite.xml` is added to `res/drawable/`.

---

## 3. Favorites Screen & Navigation

### FavoritesViewModel

Located at `features/favorites/presentation/FavoritesViewModel.kt`:

- On `init`: reads `FavoritesManager.getFavoriteIds(context)` and builds a `List<Pool>` using data from the local repository cache, or constructs minimal `Pool` objects (id, name, address, rating, photoUrl, isFavorite=true) from persisted IDs.
- `FavoritesUiState(pools: List<Pool>, isEmpty: Boolean)`
- Subscribes to `PoolStateManager.subscribeFavorite(...)` to update the list in real time when the user adds or removes a favorite from another screen.
- Unsubscribes in `onCleared()`.

### FavoritesScreen

Located at `features/favorites/presentation/FavoritesScreen.kt`:

- Stateful wrapper that injects `FavoritesViewModel` via Hilt.
- Renders `FavoritesContent(uiState, onPoolClick)` — a stateless composable for testability.
- `FavoritesContent`:
  - Empty state: centered text "Aún no tienes piscinas favoritas" with a heart icon.
  - Loaded state: `LazyColumn` of `PoolListCard` items (same component used in ExploreScreen), no native ads.
  - Tapping a card calls `onPoolClick(poolId)` → navigates to `DetailRouteNav(poolId)`.

### Navigation changes

- New sealed class `FavoritesRouteNav` added to `NavDestinations`.
- `PiscinaMapNavGraph` adds a `FavoritesScreen` destination.
- The glass-pill bottom nav bar is updated from 2 to 3 items: Mapa (`Icons.Default.Map`), Explorar (`Icons.Default.Search`), Favoritos (`Icons.Default.Favorite`).
- Transition from/to Favorites: same horizontal slide animation used for Home ↔ Explore.
- The bottom nav bar remains hidden on `DetailRouteNav`.

---

## 4. Testing

### Unit tests (`app/src/test/`)

- **`FavoritesManagerTest`** — tests `addFavorite`, `removeFavorite`, `isFavorite`, `getFavoriteIds` using a real `ApplicationProvider` context (Robolectric). Verifies that adding persists correctly, removing clears the entry, and the set is empty initially.
- **`FavoritesViewModelTest`** — verifies `UiState.isEmpty == true` when no favorites, `UiState.pools` is populated correctly from `FavoritesManager`, and that the `PoolStateManager` listener correctly updates the list when a favorite is added/removed externally.

### UI / Instrumented tests (`app/src/androidTest/`)

- **`FavoritesContentTest`** — uses `createComposeRule()`, no Hilt:
  - Empty state shows the "Aún no tienes piscinas favoritas" message.
  - Non-empty state renders the correct pool names.
  - Tapping a card triggers `onPoolClick` with the correct pool ID.
- **`DetailContentTest` (extended)** — adds tests for the favorite toggle:
  - Heart icon renders in unfavorited state initially.
  - Tapping the icon triggers `onFavoriteToggle` callback.
  - After toggle, icon changes to filled/outlined state accordingly.
- **`FakeRepositoryModule`** — no new fake repository required; `FavoritesManager` is tested directly with real SharedPreferences in Hilt test context.

---

## File Inventory

| File | Action |
|------|--------|
| `core/presentation/util/FavoritesManager.kt` | Create |
| `core/presentation/util/PoolStateManager.kt` | Extend (favorite listeners) |
| `core/domain/model/Pool.kt` | Add `isFavorite: Boolean = false` |
| `core/data/mapper/*.kt` (pool mappers) | Populate `isFavorite` from `FavoritesManager` |
| `features/favorites/presentation/FavoritesViewModel.kt` | Create |
| `features/favorites/presentation/FavoritesScreen.kt` | Create |
| `navigation/NavDestinations.kt` | Add `FavoritesRouteNav` |
| `navigation/PiscinaMapNavGraph.kt` | Add favorites destination + update nav bar |
| `features/detail/presentation/DetailScreen.kt` | Add favorite toggle icon |
| `features/home/presentation/components/PoolDetailCard.kt` | Add favorite toggle icon |
| `features/home/presentation/HomeViewModel.kt` | Subscribe to favorite state changes |
| `res/drawable/ic_marker_favorite.xml` | Create (golden star/heart marker) |
| `app/src/test/.../FavoritesManagerTest.kt` | Create |
| `app/src/test/.../FavoritesViewModelTest.kt` | Create |
| `app/src/androidTest/.../FavoritesContentTest.kt` | Create |
| `app/src/androidTest/.../DetailContentTest.kt` | Extend |

---

## Non-Goals

- No cloud sync — favorites are local to the device.
- No ordering/sorting in the Favorites screen (insertion order, FIFO).
- No "favorites" filter in Explore — only the dedicated tab.
- No API calls from FavoritesViewModel — uses data already available or minimal Pool objects from persisted IDs.
