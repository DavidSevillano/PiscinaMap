# Offline Pool Cache with Room

**Date:** 2026-05-15
**Status:** Approved

## Goal

Cache viewed pool data locally using Room so the app remains useful under low/no coverage and reduces Google Places API call costs.

## Decisions

- **What to cache:** Basic data (map/list) + full detail data (Detail screen) — two levels
- **TTL:** 7 days for basic pool data, 24 hours for full detail data
- **Approach:** Cache added directly to existing repositories (transparent to ViewModels and UseCases)

## Architecture

### New files in `core/data/local/`

```
core/data/local/
  db/
    PoolDatabase.kt             @Database singleton (Room)
    PoolCacheDao.kt             CRUD for pool_cache table
    PoolDetailCacheDao.kt       CRUD for pool_detail_cache table
  entity/
    PoolCacheEntity.kt          @Entity — basic pool data
    PoolDetailCacheEntity.kt    @Entity — full pool detail
  converter/
    RoomConverters.kt           TypeConverters (List<String>, List<Review> ↔ JSON)
core/di/
  DatabaseModule.kt             Hilt @Module providing DB + DAOs
```

### Database tables

**`pool_cache`** (TTL: 7 days)

| Column | Type | Notes |
|--------|------|-------|
| place_id | TEXT PK | Google Place ID |
| name | TEXT | |
| latitude | REAL | |
| longitude | REAL | |
| address | TEXT | |
| rating | REAL nullable | |
| is_open_now | INTEGER nullable | Boolean stored as 0/1 |
| is_favorite | INTEGER | Boolean stored as 0/1 |
| is_hidden | INTEGER | Boolean stored as 0/1 |
| cached_at | INTEGER | Unix timestamp (ms) |

**`pool_detail_cache`** (TTL: 24 hours)

| Column | Type | Notes |
|--------|------|-------|
| place_id | TEXT PK | |
| photo_urls | TEXT | JSON array of strings |
| opening_hours | TEXT | JSON array of strings |
| current_opening_hours | TEXT nullable | |
| services | TEXT | JSON array of strings |
| reviews | TEXT | JSON array of Review objects |
| formatted_phone | TEXT nullable | |
| cached_at | INTEGER | Unix timestamp (ms) |

`List<String>` and `List<Review>` are serialized to/from JSON using Kotlinx Serialization TypeConverters (already in the project).

## Data Flow by Feature

### Home / Explore — network-first with cache fallback

```
searchNearbyPools(lat, lng, radius)
  │
  ├─ try API call
  │   ├─ success → save results to pool_cache → return pools
  │   └─ failure (no network / timeout)
  │       ├─ query pool_cache for pools within radius, cached_at > now - 7d
  │       ├─ non-empty → return cached pools
  │       └─ empty → return Result.failure(e)
```

Offline radius filtering uses Euclidean distance approximation on stored coordinates — sufficient for km-scale radii without PostGIS.

### Detail — cache-first with network fallback

```
getPoolDetails(placeId)
  │
  ├─ query pool_detail_cache where cached_at > now - 24h
  │   └─ hit → return immediately (no API call)
  │
  └─ miss or stale
      ├─ try API call
      │   ├─ success → upsert pool_detail_cache + pool_cache → return pool
      │   └─ failure
      │       ├─ stale cache exists → return stale data (better than error)
      │       └─ no cache → return Result.failure(e)
```

Detail uses cache-first because it is the most expensive API call (photos, reviews, hours) and data changes infrequently within 24 hours.

## Hilt DI

New `DatabaseModule.kt` in `core/di/` — separate from existing `NetworkModule`:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun providePoolDatabase(@ApplicationContext ctx: Context): PoolDatabase =
        Room.databaseBuilder(ctx, PoolDatabase::class.java, "pool_cache.db").build()

    @Provides
    fun providePoolCacheDao(db: PoolDatabase): PoolCacheDao = db.poolCacheDao()

    @Provides
    fun providePoolDetailCacheDao(db: PoolDatabase): PoolDetailCacheDao = db.poolDetailCacheDao()
}
```

DAOs injected into repositories via constructor injection. No changes to ViewModels or UseCases.

## Modified Repositories

| Repository | Change |
|------------|--------|
| `PoolRepositoryImpl` | Inject `PoolCacheDao`; add cache save on success + fallback on failure |
| `ExploreRepositoryImpl` | Same as above |
| `DetailRepositoryImpl` | Inject both DAOs; implement cache-first logic |

## What Does NOT Change

- `HomeViewModel`, `ExploreViewModel`, `DetailViewModel`, `FavoritesViewModel`
- `GetNearbyPoolsUseCase`, `GetPoolDetailsUseCase`, `GetExploreNearbyPoolsUseCase`
- `Pool` domain model
- `FavoritesManager`, `HiddenPoolsManager`, `PoolStateManager`
- All existing Compose UI and navigation

## Testing

### Unit tests (extend existing)

**`PoolRepositoryImplTest`:**
- `searchNearbyPools_whenApiFails_returnsCachedPools`
- `searchNearbyPools_whenApiFailsAndCacheEmpty_returnsFailure`

**`ExploreRepositoryImplTest`:**
- Same two cases mirrored

**`DetailRepositoryImplTest`:**
- `getPoolDetails_whenCacheFresh_doesNotCallApi`
- `getPoolDetails_whenCacheStale_callsApiAndUpdatesCache`
- `getPoolDetails_whenApiFailsWithStaleCache_returnsStaleData`
- `getPoolDetails_whenApiFailsAndNoCache_returnsFailure`

### New instrumented test

**`PoolCacheDaoTest`** (Room in-memory database via `androidx.room:room-testing`):
- Insert and retrieve pools
- TTL expiry filters out old entries
- Radius query returns only pools within bounds
- Upsert updates existing entry without duplication

### UI tests

No changes needed — ViewModel behavior and Compose screens are unaffected.

## File Change Summary

| File | Action |
|------|--------|
| `core/data/local/db/PoolDatabase.kt` | New |
| `core/data/local/db/PoolCacheDao.kt` | New |
| `core/data/local/db/PoolDetailCacheDao.kt` | New |
| `core/data/local/entity/PoolCacheEntity.kt` | New |
| `core/data/local/entity/PoolDetailCacheEntity.kt` | New |
| `core/data/local/converter/RoomConverters.kt` | New |
| `core/di/DatabaseModule.kt` | New |
| `features/home/data/repository/PoolRepositoryImpl.kt` | Modified |
| `features/explore/data/repository/ExploreRepositoryImpl.kt` | Modified |
| `features/detail/data/repository/DetailRepositoryImpl.kt` | Modified |
| `features/home/data/repository/PoolRepositoryImplTest.kt` | Extended |
| `features/explore/data/repository/ExploreRepositoryImplTest.kt` | Extended |
| `features/detail/data/repository/DetailRepositoryImplTest.kt` | Extended |
| `core/data/local/db/PoolCacheDaoTest.kt` | New (instrumented) |
