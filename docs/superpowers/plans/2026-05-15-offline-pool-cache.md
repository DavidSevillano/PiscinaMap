# Offline Pool Cache Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cache pool data in Room so Home, Explore, and Detail screens work under no/low coverage and reduce Google Places API calls.

**Architecture:** Two Room tables — `pool_cache` (basic data, TTL 7 days) populated on every search result, and `pool_detail_cache` (full data, TTL 24h) populated on every Detail screen open. Home/Explore use network-first with cache fallback; Detail uses cache-first. All changes are inside the three existing repository implementations — ViewModels, UseCases, and UI are untouched.

**Tech Stack:** Room 2.6.1 (already in build.gradle), Kotlinx Serialization (already used), Hilt (already configured), KSP (already configured), MockK + coroutines-test (already in test deps), Room Testing (new androidTest dep needed).

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `app/src/main/java/com/burixer85/piscinamap/core/domain/model/Pool.kt` | Modify | Add `@Serializable` to `Review` |
| `core/data/local/entity/PoolCacheEntity.kt` | Create | Room entity for `pool_cache` table + mappers |
| `core/data/local/entity/PoolDetailCacheEntity.kt` | Create | Room entity for `pool_detail_cache` table + mappers |
| `core/data/local/converter/RoomConverters.kt` | Create | TypeConverters for `List<String>` and `List<Review>` |
| `core/data/local/db/PoolCacheDao.kt` | Create | Queries for `pool_cache` |
| `core/data/local/db/PoolDetailCacheDao.kt` | Create | Queries for `pool_detail_cache` |
| `core/data/local/db/PoolDatabase.kt` | Create | `@Database` singleton wiring entities + DAOs |
| `core/di/DatabaseModule.kt` | Create | Hilt `@Module` providing DB + DAOs |
| `core/di/NetworkModule.kt` | Modify | Inject DAOs into repository providers |
| `features/detail/data/repository/DetailRepositoryImpl.kt` | Modify | Cache-first logic |
| `features/home/data/repository/PoolRepositoryImpl.kt` | Modify | Network-first + cache fallback |
| `features/explore/data/repository/ExploreRepositoryImpl.kt` | Modify | Network-first + cache fallback |
| `app/build.gradle.kts` | Modify | Add `room-testing` androidTest dep |
| `androidTest/.../PoolCacheDaoTest.kt` | Create | In-memory Room DAO tests |
| `test/.../DetailRepositoryImplIntegrationTest.kt` | Modify | Add 4 cache-related test cases |
| `test/.../PoolRepositoryImplIntegrationTest.kt` | Modify | Add 2 cache-related test cases |
| `test/.../ExploreRepositoryImplIntegrationTest.kt` | Create | Mirror of PoolRepository cache tests |

All paths under `app/src/main/java/com/burixer85/piscinamap/` unless noted. Test paths under `app/src/test/java/com/burixer85/piscinamap/` or `app/src/androidTest/java/com/burixer85/piscinamap/`.

---

## Task 1 — Add `@Serializable` to `Review`

**Files:**
- Modify: `app/src/main/java/com/burixer85/piscinamap/core/domain/model/Pool.kt`

- [ ] **Step 1: Add the annotation**

Open `core/domain/model/Pool.kt` and add `@Serializable` to `Review`. The file header and `Pool` class stay untouched — only `Review` changes:

```kotlin
package com.burixer85.piscinamap.core.domain.model

import kotlinx.serialization.Serializable

data class Pool(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val rating: Float?,
    val isOpenNow: Boolean?,
    val photoUrls: List<String> = emptyList(),
    val isNew: Boolean = false,
    val isHidden: Boolean = false,
    val isFavorite: Boolean = false,
    val formattedPhone: String? = null,
    val openingHours: List<String> = emptyList(),
    val currentOpeningHours: String? = null,
    val services: List<String> = emptyList(),
    val reviews: List<Review> = emptyList()
) {
    val photoUrl: String?
        get() = photoUrls.firstOrNull()
}

@Serializable
data class Review(
    val authorName: String,
    val rating: Float,
    val text: String,
    val relativeTimeDescription: String,
    val publishedAt: Long = 0L,
    val language: String? = null,
    val profilePhotoUrl: String? = null
)
```

- [ ] **Step 2: Verify it compiles**

```
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL (no errors about missing serialization).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/burixer85/piscinamap/core/domain/model/Pool.kt
git commit -m "feat: make Review @Serializable for Room TypeConverter"
```

---

## Task 2 — Room entities and TypeConverters

**Files:**
- Create: `app/src/main/java/com/burixer85/piscinamap/core/data/local/entity/PoolCacheEntity.kt`
- Create: `app/src/main/java/com/burixer85/piscinamap/core/data/local/entity/PoolDetailCacheEntity.kt`
- Create: `app/src/main/java/com/burixer85/piscinamap/core/data/local/converter/RoomConverters.kt`

- [ ] **Step 1: Create `PoolCacheEntity.kt`**

```kotlin
package com.burixer85.piscinamap.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.burixer85.piscinamap.core.domain.model.Pool

@Entity(tableName = "pool_cache")
data class PoolCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "place_id")
    val placeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val rating: Float?,
    @ColumnInfo(name = "is_open_now") val isOpenNow: Boolean?,
    @ColumnInfo(name = "cached_at") val cachedAt: Long
)

fun PoolCacheEntity.toDomain(): Pool = Pool(
    id = placeId,
    name = name,
    latitude = latitude,
    longitude = longitude,
    address = address,
    rating = rating,
    isOpenNow = isOpenNow
)

fun Pool.toCacheEntity(): PoolCacheEntity = PoolCacheEntity(
    placeId = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    address = address,
    rating = rating,
    isOpenNow = isOpenNow,
    cachedAt = System.currentTimeMillis()
)
```

- [ ] **Step 2: Create `PoolDetailCacheEntity.kt`**

This entity is self-contained — it stores ALL `Pool` fields so the Detail screen can reconstruct the full object from cache alone.

```kotlin
package com.burixer85.piscinamap.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.domain.model.Review

@Entity(tableName = "pool_detail_cache")
data class PoolDetailCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "place_id")
    val placeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val rating: Float?,
    @ColumnInfo(name = "is_open_now") val isOpenNow: Boolean?,
    @ColumnInfo(name = "photo_urls") val photoUrls: List<String>,
    @ColumnInfo(name = "opening_hours") val openingHours: List<String>,
    @ColumnInfo(name = "current_opening_hours") val currentOpeningHours: String?,
    val services: List<String>,
    val reviews: List<Review>,
    @ColumnInfo(name = "formatted_phone") val formattedPhone: String?,
    @ColumnInfo(name = "cached_at") val cachedAt: Long
)

fun PoolDetailCacheEntity.toDomain(): Pool = Pool(
    id = placeId,
    name = name,
    latitude = latitude,
    longitude = longitude,
    address = address,
    rating = rating,
    isOpenNow = isOpenNow,
    photoUrls = photoUrls,
    openingHours = openingHours,
    currentOpeningHours = currentOpeningHours,
    services = services,
    reviews = reviews,
    formattedPhone = formattedPhone
)

fun Pool.toDetailCacheEntity(): PoolDetailCacheEntity = PoolDetailCacheEntity(
    placeId = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    address = address,
    rating = rating,
    isOpenNow = isOpenNow,
    photoUrls = photoUrls,
    openingHours = openingHours,
    currentOpeningHours = currentOpeningHours,
    services = services,
    reviews = reviews,
    formattedPhone = formattedPhone,
    cachedAt = System.currentTimeMillis()
)
```

- [ ] **Step 3: Create `RoomConverters.kt`**

```kotlin
package com.burixer85.piscinamap.core.data.local.converter

import androidx.room.TypeConverter
import com.burixer85.piscinamap.core.domain.model.Review
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = json.decodeFromString(value)

    @TypeConverter
    fun fromReviewList(value: List<Review>): String = json.encodeToString(value)

    @TypeConverter
    fun toReviewList(value: String): List<Review> = json.decodeFromString(value)
}
```

- [ ] **Step 4: Verify it compiles**

```
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/burixer85/piscinamap/core/data/local/
git commit -m "feat: add Room entities and TypeConverters for pool cache"
```

---

## Task 3 — DAOs and Database

**Files:**
- Create: `app/src/main/java/com/burixer85/piscinamap/core/data/local/db/PoolCacheDao.kt`
- Create: `app/src/main/java/com/burixer85/piscinamap/core/data/local/db/PoolDetailCacheDao.kt`
- Create: `app/src/main/java/com/burixer85/piscinamap/core/data/local/db/PoolDatabase.kt`

- [ ] **Step 1: Create `PoolCacheDao.kt`**

```kotlin
package com.burixer85.piscinamap.core.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.burixer85.piscinamap.core.data.local.entity.PoolCacheEntity

@Dao
interface PoolCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPools(pools: List<PoolCacheEntity>)

    @Query("""
        SELECT * FROM pool_cache
        WHERE latitude BETWEEN :minLat AND :maxLat
          AND longitude BETWEEN :minLng AND :maxLng
          AND cached_at >= :minCachedAt
    """)
    suspend fun getPoolsInBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
        minCachedAt: Long
    ): List<PoolCacheEntity>
}
```

- [ ] **Step 2: Create `PoolDetailCacheDao.kt`**

```kotlin
package com.burixer85.piscinamap.core.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.burixer85.piscinamap.core.data.local.entity.PoolDetailCacheEntity

@Dao
interface PoolDetailCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetail(detail: PoolDetailCacheEntity)

    @Query("SELECT * FROM pool_detail_cache WHERE place_id = :placeId AND cached_at >= :minCachedAt LIMIT 1")
    suspend fun getDetail(placeId: String, minCachedAt: Long): PoolDetailCacheEntity?

    @Query("SELECT * FROM pool_detail_cache WHERE place_id = :placeId LIMIT 1")
    suspend fun getDetailIgnoringTtl(placeId: String): PoolDetailCacheEntity?
}
```

- [ ] **Step 3: Create `PoolDatabase.kt`**

```kotlin
package com.burixer85.piscinamap.core.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.burixer85.piscinamap.core.data.local.converter.RoomConverters
import com.burixer85.piscinamap.core.data.local.entity.PoolCacheEntity
import com.burixer85.piscinamap.core.data.local.entity.PoolDetailCacheEntity

@Database(
    entities = [PoolCacheEntity::class, PoolDetailCacheEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class PoolDatabase : RoomDatabase() {
    abstract fun poolCacheDao(): PoolCacheDao
    abstract fun poolDetailCacheDao(): PoolDetailCacheDao
}
```

- [ ] **Step 4: Verify it compiles**

```
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL (Room annotation processor generates the implementation via KSP).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/burixer85/piscinamap/core/data/local/db/
git commit -m "feat: add Room DAOs and PoolDatabase"
```

---

## Task 4 — Hilt DatabaseModule + update NetworkModule

**Files:**
- Create: `app/src/main/java/com/burixer85/piscinamap/core/di/DatabaseModule.kt`
- Modify: `app/src/main/java/com/burixer85/piscinamap/core/di/NetworkModule.kt`

- [ ] **Step 1: Create `DatabaseModule.kt`**

```kotlin
package com.burixer85.piscinamap.core.di

import android.content.Context
import androidx.room.Room
import com.burixer85.piscinamap.core.data.local.db.PoolCacheDao
import com.burixer85.piscinamap.core.data.local.db.PoolDatabase
import com.burixer85.piscinamap.core.data.local.db.PoolDetailCacheDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePoolDatabase(@ApplicationContext context: Context): PoolDatabase =
        Room.databaseBuilder(context, PoolDatabase::class.java, "pool_cache.db").build()

    @Provides
    fun providePoolCacheDao(db: PoolDatabase): PoolCacheDao = db.poolCacheDao()

    @Provides
    fun providePoolDetailCacheDao(db: PoolDatabase): PoolDetailCacheDao = db.poolDetailCacheDao()
}
```

- [ ] **Step 2: Update the three repository providers in `NetworkModule.kt`**

Replace the three `@Provides` methods that create repositories. The rest of `NetworkModule` (the `provideGoogleApi` method) stays untouched.

```kotlin
package com.burixer85.piscinamap.core.di

import android.content.Context
import com.burixer85.piscinamap.core.data.GooglePlacesApi
import com.burixer85.piscinamap.core.data.local.db.PoolCacheDao
import com.burixer85.piscinamap.core.data.local.db.PoolDetailCacheDao
import com.burixer85.piscinamap.features.detail.data.repository.DetailRepositoryImpl
import com.burixer85.piscinamap.features.detail.domain.repository.DetailRepository
import com.burixer85.piscinamap.features.explore.data.repository.ExploreRepositoryImpl
import com.burixer85.piscinamap.features.explore.domain.repository.ExploreRepository
import com.burixer85.piscinamap.features.home.data.repository.PoolRepositoryImpl
import com.burixer85.piscinamap.features.home.domain.repository.PoolRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGoogleApi(): GooglePlacesApi {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }
        return Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GooglePlacesApi::class.java)
    }

    @Provides
    @Singleton
    fun providePoolRepository(
        api: GooglePlacesApi,
        @ApplicationContext context: Context,
        poolCacheDao: PoolCacheDao
    ): PoolRepository = PoolRepositoryImpl(api, context, poolCacheDao)

    @Provides
    @Singleton
    fun provideDetailRepository(
        api: GooglePlacesApi,
        poolDetailCacheDao: PoolDetailCacheDao
    ): DetailRepository = DetailRepositoryImpl(api, poolDetailCacheDao)

    @Provides
    @Singleton
    fun provideExploreRepository(
        api: GooglePlacesApi,
        @ApplicationContext context: Context,
        poolCacheDao: PoolCacheDao
    ): ExploreRepository = ExploreRepositoryImpl(api, context, poolCacheDao)
}
```

- [ ] **Step 3: Verify the app compiles (constructors don't match yet — this will fail)**

```
./gradlew :app:compileDebugKotlin
```

Expected: compilation errors on `PoolRepositoryImpl`, `DetailRepositoryImpl`, `ExploreRepositoryImpl` constructors — this is expected and will be fixed in Tasks 6-8.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/burixer85/piscinamap/core/di/
git commit -m "feat: add DatabaseModule and update NetworkModule for cache DAOs"
```

---

## Task 5 — DAO instrumented tests

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/androidTest/java/com/burixer85/piscinamap/core/data/local/db/PoolCacheDaoTest.kt`

- [ ] **Step 1: Add `room-testing` dependency to `build.gradle.kts`**

In `app/build.gradle.kts`, add after the existing `testImplementation` block:

```kotlin
androidTestImplementation("androidx.room:room-testing:2.6.1")
androidTestImplementation("androidx.test:runner:1.6.2")
androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
```

- [ ] **Step 2: Create `PoolCacheDaoTest.kt`**

```kotlin
package com.burixer85.piscinamap.core.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.burixer85.piscinamap.core.data.local.entity.PoolCacheEntity
import com.burixer85.piscinamap.core.data.local.entity.PoolDetailCacheEntity
import com.burixer85.piscinamap.core.domain.model.Review
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PoolCacheDaoTest {

    private lateinit var db: PoolDatabase
    private lateinit var poolCacheDao: PoolCacheDao
    private lateinit var poolDetailCacheDao: PoolDetailCacheDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PoolDatabase::class.java).build()
        poolCacheDao = db.poolCacheDao()
        poolDetailCacheDao = db.poolDetailCacheDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndRetrievePoolsInBoundingBox() = runTest {
        val now = System.currentTimeMillis()
        val pool = PoolCacheEntity("id1", "Piscina Test", 40.416, -3.703, "Calle Mayor", 4.2f, true, now)
        poolCacheDao.insertPools(listOf(pool))

        val minLat = 40.0; val maxLat = 41.0
        val minLng = -4.0; val maxLng = -3.0
        val minCachedAt = now - 1000L

        val result = poolCacheDao.getPoolsInBoundingBox(minLat, maxLat, minLng, maxLng, minCachedAt)

        assertEquals(1, result.size)
        assertEquals("Piscina Test", result[0].name)
    }

    @Test
    fun poolOutsideBoundingBoxIsNotReturned() = runTest {
        val now = System.currentTimeMillis()
        val pool = PoolCacheEntity("id2", "Piscina Lejana", 51.5, -0.1, "London Rd", 3.5f, null, now)
        poolCacheDao.insertPools(listOf(pool))

        val result = poolCacheDao.getPoolsInBoundingBox(40.0, 41.0, -4.0, -3.0, now - 1000L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun expiredPoolIsExcludedByTtlFilter() = runTest {
        val eightDaysAgo = System.currentTimeMillis() - 8 * 24 * 60 * 60 * 1000L
        val pool = PoolCacheEntity("id3", "Piscina Antigua", 40.416, -3.703, "Calle X", 4.0f, false, eightDaysAgo)
        poolCacheDao.insertPools(listOf(pool))

        val sevenDaysTtl = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val result = poolCacheDao.getPoolsInBoundingBox(40.0, 41.0, -4.0, -3.0, sevenDaysTtl)

        assertTrue(result.isEmpty())
    }

    @Test
    fun insertPoolsUpsertsDuplicatesWithLatestData() = runTest {
        val now = System.currentTimeMillis()
        val original = PoolCacheEntity("id4", "Piscina Original", 40.416, -3.703, "Calle A", 3.0f, true, now)
        val updated = PoolCacheEntity("id4", "Piscina Actualizada", 40.416, -3.703, "Calle A", 4.8f, true, now + 1000L)

        poolCacheDao.insertPools(listOf(original))
        poolCacheDao.insertPools(listOf(updated))

        val result = poolCacheDao.getPoolsInBoundingBox(40.0, 41.0, -4.0, -3.0, now - 1000L)

        assertEquals(1, result.size)
        assertEquals("Piscina Actualizada", result[0].name)
        assertEquals(4.8f, result[0].rating)
    }

    @Test
    fun insertDetailAndRetrieveWithFreshTtl() = runTest {
        val now = System.currentTimeMillis()
        val detail = PoolDetailCacheEntity(
            placeId = "id5", name = "Piscina Detalle", latitude = 40.416, longitude = -3.703,
            address = "Gran Vía 1", rating = 4.5f, isOpenNow = true,
            photoUrls = listOf("url1"), openingHours = listOf("Lunes: 9-21"),
            currentOpeningHours = "9:00 - 21:00", services = listOf("Piscina"),
            reviews = listOf(Review("Ana", 5f, "Genial", "hace 1 semana")),
            formattedPhone = "+34 900 000 000", cachedAt = now
        )
        poolDetailCacheDao.insertDetail(detail)

        val result = poolDetailCacheDao.getDetail("id5", now - 1000L)

        assertNotNull(result)
        assertEquals("Piscina Detalle", result!!.name)
        assertEquals(1, result.reviews.size)
        assertEquals("Ana", result.reviews[0].authorName)
        assertEquals(listOf("url1"), result.photoUrls)
    }

    @Test
    fun expiredDetailIsNotReturnedByFreshQuery() = runTest {
        val twentyFiveHoursAgo = System.currentTimeMillis() - 25 * 60 * 60 * 1000L
        val detail = PoolDetailCacheEntity(
            placeId = "id6", name = "Piscina Expirada", latitude = 40.416, longitude = -3.703,
            address = "Calle Y", rating = 3.0f, isOpenNow = false,
            photoUrls = emptyList(), openingHours = emptyList(),
            currentOpeningHours = null, services = emptyList(),
            reviews = emptyList(), formattedPhone = null, cachedAt = twentyFiveHoursAgo
        )
        poolDetailCacheDao.insertDetail(detail)

        val twentyFourHoursTtl = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        val result = poolDetailCacheDao.getDetail("id6", twentyFourHoursTtl)

        assertNull(result)
    }

    @Test
    fun expiredDetailIsReturnedByIgnoreTtlQuery() = runTest {
        val twentyFiveHoursAgo = System.currentTimeMillis() - 25 * 60 * 60 * 1000L
        val detail = PoolDetailCacheEntity(
            placeId = "id7", name = "Piscina Stale", latitude = 40.416, longitude = -3.703,
            address = "Calle Z", rating = 3.0f, isOpenNow = false,
            photoUrls = emptyList(), openingHours = emptyList(),
            currentOpeningHours = null, services = emptyList(),
            reviews = emptyList(), formattedPhone = null, cachedAt = twentyFiveHoursAgo
        )
        poolDetailCacheDao.insertDetail(detail)

        val result = poolDetailCacheDao.getDetailIgnoringTtl("id7")

        assertNotNull(result)
        assertEquals("Piscina Stale", result!!.name)
    }
}
```

- [ ] **Step 3: Run the DAO tests**

```
./gradlew :app:connectedAndroidTest --tests "*.PoolCacheDaoTest"
```

Expected: 7 tests PASS (requires a connected device or emulator).

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts
git add app/src/androidTest/java/com/burixer85/piscinamap/core/data/local/db/PoolCacheDaoTest.kt
git commit -m "test: add instrumented DAO tests for pool cache with in-memory Room"
```

---

## Task 6 — DetailRepositoryImpl: cache-first logic (TDD)

**Files:**
- Modify: `app/src/test/java/com/burixer85/piscinamap/features/detail/data/repository/DetailRepositoryImplIntegrationTest.kt`
- Modify: `app/src/main/java/com/burixer85/piscinamap/features/detail/data/repository/DetailRepositoryImpl.kt`

- [ ] **Step 1: Add failing tests to `DetailRepositoryImplIntegrationTest`**

Add the following imports and fields to the existing test class, then add 4 new test methods. The `setUp` method changes to inject mock DAOs:

```kotlin
// New imports to add at the top of the file:
import com.burixer85.piscinamap.core.data.local.db.PoolDetailCacheDao
import com.burixer85.piscinamap.core.data.local.entity.PoolDetailCacheEntity
import com.burixer85.piscinamap.core.domain.model.Review
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
```

Replace the `setUp` method and add the new field + 4 test cases:

```kotlin
    private lateinit var mockDetailCacheDao: PoolDetailCacheDao  // ADD this field

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        mockDetailCacheDao = mockk(relaxed = true)  // ADD: relaxed so insert calls don't need stubs

        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }
        val api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GooglePlacesApi::class.java)

        repository = DetailRepositoryImpl(api, mockDetailCacheDao)  // CHANGED: add dao
    }

    // --- ADD these 4 new test methods ---

    @Test
    fun `getPoolDetails returns cached detail when cache is fresh without calling API`() = runTest {
        val now = System.currentTimeMillis()
        val cachedDetail = PoolDetailCacheEntity(
            placeId = "ChIJcached", name = "Piscina Cacheada", latitude = 37.388, longitude = -5.982,
            address = "Calle Cache 1", rating = 4.1f, isOpenNow = true,
            photoUrls = listOf("url_cached"), openingHours = listOf("Lunes: 9-21"),
            currentOpeningHours = "9:00 - 21:00", services = listOf("Piscina"),
            reviews = listOf(Review("María", 4f, "Muy bien", "hace 3 días")),
            formattedPhone = "+34 900 111 222", cachedAt = now
        )
        coEvery { mockDetailCacheDao.getDetail(eq("ChIJcached"), any()) } returns cachedDetail

        val result = repository.getPoolDetails("ChIJcached")

        assertTrue(result.isSuccess)
        assertEquals("Piscina Cacheada", result.getOrThrow().name)
        assertEquals("ChIJcached", result.getOrThrow().id)
        assertEquals(1, result.getOrThrow().reviews.size)
        // No HTTP request was made
        assertEquals(0, mockWebServer.requestCount)
    }

    @Test
    fun `getPoolDetails calls API when cache is stale and saves result`() = runTest {
        coEvery { mockDetailCacheDao.getDetail(any(), any()) } returns null  // fresh query misses
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(DETAIL_OK_RESPONSE))

        val result = repository.getPoolDetails("ChIJtest123")

        assertTrue(result.isSuccess)
        assertEquals(1, mockWebServer.requestCount)
        coVerify { mockDetailCacheDao.insertDetail(any()) }
    }

    @Test
    fun `getPoolDetails returns stale cache when API fails`() = runTest {
        val staleDetail = PoolDetailCacheEntity(
            placeId = "ChIJstale", name = "Piscina Stale", latitude = 37.388, longitude = -5.982,
            address = "Calle Vieja 1", rating = 3.8f, isOpenNow = null,
            photoUrls = emptyList(), openingHours = emptyList(),
            currentOpeningHours = null, services = emptyList(),
            reviews = emptyList(), formattedPhone = null, cachedAt = 1L
        )
        coEvery { mockDetailCacheDao.getDetail(any(), any()) } returns null   // fresh miss
        coEvery { mockDetailCacheDao.getDetailIgnoringTtl("ChIJstale") } returns staleDetail
        mockWebServer.shutdown()

        val result = repository.getPoolDetails("ChIJstale")

        assertTrue(result.isSuccess)
        assertEquals("Piscina Stale", result.getOrThrow().name)
    }

    @Test
    fun `getPoolDetails returns failure when API fails and there is no cache at all`() = runTest {
        coEvery { mockDetailCacheDao.getDetail(any(), any()) } returns null
        coEvery { mockDetailCacheDao.getDetailIgnoringTtl(any()) } returns null
        mockWebServer.shutdown()

        val result = repository.getPoolDetails("ChIJnocache")

        assertTrue(result.isFailure)
    }
```

- [ ] **Step 2: Run tests — expect compilation failure**

```
./gradlew :app:testDebugUnitTest --tests "*.DetailRepositoryImplIntegrationTest"
```

Expected: compilation error — `DetailRepositoryImpl(api)` no longer matches once we update the constructor. The tests define what the new constructor must look like.

- [ ] **Step 3: Implement cache-first logic in `DetailRepositoryImpl.kt`**

Replace the entire file:

```kotlin
package com.burixer85.piscinamap.features.detail.data.repository

import com.burixer85.piscinamap.BuildConfig
import com.burixer85.piscinamap.core.data.GooglePlacesApi
import com.burixer85.piscinamap.core.data.dto.*
import com.burixer85.piscinamap.core.data.local.db.PoolDetailCacheDao
import com.burixer85.piscinamap.core.data.local.entity.*
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.detail.domain.repository.DetailRepository
import java.util.Locale
import javax.inject.Inject

class DetailRepositoryImpl @Inject constructor(
    private val api: GooglePlacesApi,
    private val poolDetailCacheDao: PoolDetailCacheDao
) : DetailRepository {

    override suspend fun getPoolDetails(placeId: String): Result<Pool> {
        val freshThreshold = System.currentTimeMillis() - DETAIL_TTL_MS
        val cached = poolDetailCacheDao.getDetail(placeId, freshThreshold)
        if (cached != null) return Result.success(cached.toDomain())

        return try {
            val language = Locale.getDefault().language
            val response = api.getPlaceDetails(
                placeId = placeId,
                apiKey = BuildConfig.GOOGLEMAPS_KEY,
                language = language
            )
            val pool = response.result?.toDomain()?.copy(id = placeId)
                ?: return Result.failure(Exception("Pool not found"))

            poolDetailCacheDao.insertDetail(pool.toDetailCacheEntity())
            Result.success(pool)
        } catch (e: Exception) {
            val stale = poolDetailCacheDao.getDetailIgnoringTtl(placeId)
            if (stale != null) Result.success(stale.toDomain())
            else Result.failure(e)
        }
    }

    companion object {
        private val DETAIL_TTL_MS = 24 * 60 * 60 * 1000L
    }
}
```

- [ ] **Step 4: Run tests — all should pass**

```
./gradlew :app:testDebugUnitTest --tests "*.DetailRepositoryImplIntegrationTest"
```

Expected: 10 tests PASS (6 original + 4 new).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/burixer85/piscinamap/features/detail/data/repository/DetailRepositoryImpl.kt
git add app/src/test/java/com/burixer85/piscinamap/features/detail/data/repository/DetailRepositoryImplIntegrationTest.kt
git commit -m "feat: add cache-first offline support to DetailRepositoryImpl"
```

---

## Task 7 — PoolRepositoryImpl: network-first + fallback (TDD)

**Files:**
- Modify: `app/src/test/java/com/burixer85/piscinamap/features/home/data/repository/PoolRepositoryImplIntegrationTest.kt`
- Modify: `app/src/main/java/com/burixer85/piscinamap/features/home/data/repository/PoolRepositoryImpl.kt`

- [ ] **Step 1: Add failing tests to `PoolRepositoryImplIntegrationTest`**

Add the following imports and field, update `setUp`, and add 2 new test methods:

```kotlin
// New imports to add:
import com.burixer85.piscinamap.core.data.local.db.PoolCacheDao
import com.burixer85.piscinamap.core.data.local.entity.PoolCacheEntity
import io.mockk.coEvery
import io.mockk.coVerify
```

Add field and update `setUp`:

```kotlin
    private lateinit var mockPoolCacheDao: PoolCacheDao  // ADD field

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        mockPoolCacheDao = mockk(relaxed = true)  // ADD

        mockSharedPrefs = mockk()
        every { mockSharedPrefs.getStringSet("hidden_pool_ids", emptySet()) } returns emptySet()
        every { mockSharedPrefs.getStringSet("favorite_pool_ids", emptySet()) } returns emptySet()

        mockContext = mockk()
        every { mockContext.getSharedPreferences("hidden_pools", Context.MODE_PRIVATE) } returns mockSharedPrefs
        every { mockContext.getSharedPreferences("favorite_pools", Context.MODE_PRIVATE) } returns mockSharedPrefs

        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }
        val api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GooglePlacesApi::class.java)

        repository = PoolRepositoryImpl(api, mockContext, mockPoolCacheDao)  // CHANGED
    }

    // --- ADD these 2 new test methods ---

    @Test
    fun `searchNearbyPools returns cached pools when API throws exception`() = runTest {
        val now = System.currentTimeMillis()
        val cachedEntity = PoolCacheEntity("ChIJcached99", "Piscina Cacheada Mapa", 37.388, -5.982, "Calle Cache", 4.0f, true, now)
        coEvery { mockPoolCacheDao.getPoolsInBoundingBox(any(), any(), any(), any(), any()) } returns listOf(cachedEntity)
        mockWebServer.shutdown()

        val result = repository.searchNearbyPools(37.388, -5.982, 2500)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("Piscina Cacheada Mapa", result.getOrNull()?.first()?.name)
        assertEquals("ChIJcached99", result.getOrNull()?.first()?.id)
    }

    @Test
    fun `searchNearbyPools returns failure when API fails and cache is empty`() = runTest {
        coEvery { mockPoolCacheDao.getPoolsInBoundingBox(any(), any(), any(), any(), any()) } returns emptyList()
        mockWebServer.shutdown()

        val result = repository.searchNearbyPools(37.388, -5.982, 2500)

        assertTrue(result.isFailure)
    }
```

- [ ] **Step 2: Run tests — expect compilation failure**

```
./gradlew :app:testDebugUnitTest --tests "*.PoolRepositoryImplIntegrationTest"
```

Expected: compilation error — constructor mismatch until we update `PoolRepositoryImpl`.

- [ ] **Step 3: Implement network-first + fallback in `PoolRepositoryImpl.kt`**

Replace the entire file:

```kotlin
package com.burixer85.piscinamap.features.home.data.repository

import android.content.Context
import com.burixer85.piscinamap.BuildConfig
import com.burixer85.piscinamap.core.data.GooglePlacesApi
import com.burixer85.piscinamap.core.data.dto.*
import com.burixer85.piscinamap.core.data.local.db.PoolCacheDao
import com.burixer85.piscinamap.core.data.local.entity.*
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.presentation.util.FavoritesManager
import com.burixer85.piscinamap.core.presentation.util.HiddenPoolsManager
import com.burixer85.piscinamap.features.home.domain.repository.PoolRepository
import java.util.Locale
import javax.inject.Inject
import kotlin.math.cos

class PoolRepositoryImpl @Inject constructor(
    private val api: GooglePlacesApi,
    private val context: Context,
    private val poolCacheDao: PoolCacheDao
) : PoolRepository {

    override suspend fun searchNearbyPools(lat: Double, lng: Double, radius: Int): Result<List<Pool>> {
        return try {
            val language = Locale.getDefault().language
            val response = api.getNearbyPools(
                location = "$lat,$lng",
                radius = radius,
                keyword = "swimming_pool",
                language = language,
                apiKey = BuildConfig.GOOGLEMAPS_KEY
            )

            when (response.status) {
                "REQUEST_DENIED" -> return Result.failure(Exception("Error de API: Solicitud denegada"))
                "OVER_QUERY_LIMIT" -> return Result.failure(Exception("Cuota de API excedida"))
                "INVALID_REQUEST" -> return Result.failure(Exception("Solicitud inválida"))
                "UNKNOWN_ERROR" -> return Result.failure(Exception("Error del servidor"))
                "ZERO_RESULTS", "OK" -> {}
                else -> {}
            }

            val pools = response.results
                .filter { place ->
                    val name = place.name.lowercase()
                    val excludePatterns = listOf("piscinas", "piscina s", "piscinas triana", "acuaeuropa", "piscinas sevilla", "piscina sevilla", "piscina madrid", "tienda", "tienda de", "ventas", "venta de", "s.l.", " s.l", "sl", "sa", "s.a.", "slne")
                    val genericNames = listOf("swimming pool", "piscina", "pool")
                    val isGenericName = genericNames.any { name.trim() == it }
                    !isGenericName && excludePatterns.none { pattern -> name.contains(pattern) }
                }
                .map { place ->
                    val isHidden = HiddenPoolsManager.isHidden(context, place.placeId)
                    val isFavorite = FavoritesManager.isFavorite(context, place.placeId)
                    place.toDomain().copy(isHidden = isHidden, isFavorite = isFavorite)
                }

            poolCacheDao.insertPools(pools.map { it.toCacheEntity() })
            Result.success(pools)
        } catch (e: Exception) {
            val ttlThreshold = System.currentTimeMillis() - BASIC_TTL_MS
            val latDelta = radius / 111_000.0
            val lngDelta = radius / (111_000.0 * cos(Math.toRadians(lat)))
            val cached = poolCacheDao.getPoolsInBoundingBox(
                minLat = lat - latDelta, maxLat = lat + latDelta,
                minLng = lng - lngDelta, maxLng = lng + lngDelta,
                minCachedAt = ttlThreshold
            )
            if (cached.isNotEmpty()) {
                Result.success(cached.map { entity ->
                    val isHidden = HiddenPoolsManager.isHidden(context, entity.placeId)
                    val isFavorite = FavoritesManager.isFavorite(context, entity.placeId)
                    entity.toDomain().copy(isHidden = isHidden, isFavorite = isFavorite)
                })
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getPoolDetails(placeId: String): Result<Pool> {
        return try {
            val language = Locale.getDefault().language
            val response = api.getPlaceDetails(
                placeId = placeId,
                apiKey = BuildConfig.GOOGLEMAPS_KEY,
                language = language
            )
            val pool = response.result?.toDomain()?.copy(id = placeId)
                ?: return Result.failure(Exception("Pool not found"))
            Result.success(pool)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private val BASIC_TTL_MS = 7 * 24 * 60 * 60 * 1000L
    }
}
```

- [ ] **Step 4: Run tests — all should pass**

```
./gradlew :app:testDebugUnitTest --tests "*.PoolRepositoryImplIntegrationTest"
```

Expected: 13 tests PASS (11 original + 2 new).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/burixer85/piscinamap/features/home/data/repository/PoolRepositoryImpl.kt
git add app/src/test/java/com/burixer85/piscinamap/features/home/data/repository/PoolRepositoryImplIntegrationTest.kt
git commit -m "feat: add network-first offline cache fallback to PoolRepositoryImpl"
```

---

## Task 8 — ExploreRepositoryImpl: network-first + fallback (TDD)

**Files:**
- Create: `app/src/test/java/com/burixer85/piscinamap/features/explore/data/repository/ExploreRepositoryImplIntegrationTest.kt`
- Modify: `app/src/main/java/com/burixer85/piscinamap/features/explore/data/repository/ExploreRepositoryImpl.kt`

- [ ] **Step 1: Create `ExploreRepositoryImplIntegrationTest.kt`**

```kotlin
package com.burixer85.piscinamap.features.explore.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.burixer85.piscinamap.core.data.GooglePlacesApi
import com.burixer85.piscinamap.core.data.local.db.PoolCacheDao
import com.burixer85.piscinamap.core.data.local.entity.PoolCacheEntity
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class ExploreRepositoryImplIntegrationTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: ExploreRepositoryImpl
    private lateinit var mockContext: Context
    private lateinit var mockSharedPrefs: SharedPreferences
    private lateinit var mockPoolCacheDao: PoolCacheDao

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        mockPoolCacheDao = mockk(relaxed = true)

        mockSharedPrefs = mockk()
        every { mockSharedPrefs.getStringSet("hidden_pool_ids", emptySet()) } returns emptySet()
        every { mockSharedPrefs.getStringSet("favorite_pool_ids", emptySet()) } returns emptySet()

        mockContext = mockk()
        every { mockContext.getSharedPreferences("hidden_pools", Context.MODE_PRIVATE) } returns mockSharedPrefs
        every { mockContext.getSharedPreferences("favorite_pools", Context.MODE_PRIVATE) } returns mockSharedPrefs

        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }
        val api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GooglePlacesApi::class.java)

        repository = ExploreRepositoryImpl(api, mockContext, mockPoolCacheDao)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `searchNearbyPools returns mapped pools on OK response`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(NEARBY_OK_RESPONSE))

        val result = repository.searchNearbyPools(37.388, -5.982, 50000)

        assertTrue(result.isSuccess)
        val pools = result.getOrThrow()
        assertEquals(1, pools.size)
        assertEquals("Piscina Olímpica Sur", pools[0].name)
    }

    @Test
    fun `searchNearbyPools returns cached pools when API throws exception`() = runTest {
        val now = System.currentTimeMillis()
        val cachedEntity = PoolCacheEntity("ChIJexplore1", "Piscina Explore Cache", 37.388, -5.982, "Calle Explorar", 4.3f, true, now)
        coEvery { mockPoolCacheDao.getPoolsInBoundingBox(any(), any(), any(), any(), any()) } returns listOf(cachedEntity)
        mockWebServer.shutdown()

        val result = repository.searchNearbyPools(37.388, -5.982, 50000)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("Piscina Explore Cache", result.getOrNull()?.first()?.name)
    }

    @Test
    fun `searchNearbyPools returns failure when API fails and cache is empty`() = runTest {
        coEvery { mockPoolCacheDao.getPoolsInBoundingBox(any(), any(), any(), any(), any()) } returns emptyList()
        mockWebServer.shutdown()

        val result = repository.searchNearbyPools(37.388, -5.982, 50000)

        assertTrue(result.isFailure)
    }

    companion object {
        private val NEARBY_OK_RESPONSE = """
            {
              "status": "OK",
              "results": [
                {
                  "place_id": "ChIJexplore1",
                  "name": "Piscina Olímpica Sur",
                  "vicinity": "Av. Sur 10, Sevilla",
                  "geometry": { "location": { "lat": 37.388, "lng": -5.982 } },
                  "rating": 4.5,
                  "opening_hours": { "open_now": true },
                  "types": ["swimming_pool"]
                }
              ]
            }
        """.trimIndent()
    }
}
```

- [ ] **Step 2: Run tests — expect compilation failure**

```
./gradlew :app:testDebugUnitTest --tests "*.ExploreRepositoryImplIntegrationTest"
```

Expected: compilation error — constructor mismatch until we update `ExploreRepositoryImpl`.

- [ ] **Step 3: Implement network-first + fallback in `ExploreRepositoryImpl.kt`**

Replace the entire file:

```kotlin
package com.burixer85.piscinamap.features.explore.data.repository

import android.content.Context
import com.burixer85.piscinamap.BuildConfig
import com.burixer85.piscinamap.core.data.GooglePlacesApi
import com.burixer85.piscinamap.core.data.dto.*
import com.burixer85.piscinamap.core.data.local.db.PoolCacheDao
import com.burixer85.piscinamap.core.data.local.entity.*
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.presentation.util.FavoritesManager
import com.burixer85.piscinamap.core.presentation.util.HiddenPoolsManager
import com.burixer85.piscinamap.features.explore.domain.repository.ExploreRepository
import java.util.Locale
import javax.inject.Inject
import kotlin.math.cos

class ExploreRepositoryImpl @Inject constructor(
    private val api: GooglePlacesApi,
    private val context: Context,
    private val poolCacheDao: PoolCacheDao
) : ExploreRepository {

    override suspend fun searchNearbyPools(lat: Double, lng: Double, radius: Int): Result<List<Pool>> {
        return try {
            val language = Locale.getDefault().language
            val response = api.getNearbyPools(
                location = "$lat,$lng",
                radius = radius,
                keyword = "swimming_pool",
                language = language,
                apiKey = BuildConfig.GOOGLEMAPS_KEY
            )

            val pools = response.results
                .filter { place ->
                    val name = place.name.lowercase()
                    val excludePatterns = listOf("piscinas", "piscina s", "piscinas triana", "acuaeuropa", "piscinas sevilla", "piscina sevilla", "piscina madrid", "tienda", "tienda de", "ventas", "venta de", "s.l.", " s.l", "sl", "sa", "s.a.", "slne")
                    val genericNames = listOf("swimming pool", "piscina", "pool")
                    val isGenericName = genericNames.any { name.trim() == it }
                    !isGenericName && excludePatterns.none { pattern -> name.contains(pattern) }
                }
                .map { place ->
                    val isHidden = HiddenPoolsManager.isHidden(context, place.placeId)
                    val isFavorite = FavoritesManager.isFavorite(context, place.placeId)
                    place.toDomain().copy(isHidden = isHidden, isFavorite = isFavorite)
                }

            poolCacheDao.insertPools(pools.map { it.toCacheEntity() })
            Result.success(pools)
        } catch (e: Exception) {
            val ttlThreshold = System.currentTimeMillis() - BASIC_TTL_MS
            val latDelta = radius / 111_000.0
            val lngDelta = radius / (111_000.0 * cos(Math.toRadians(lat)))
            val cached = poolCacheDao.getPoolsInBoundingBox(
                minLat = lat - latDelta, maxLat = lat + latDelta,
                minLng = lng - lngDelta, maxLng = lng + lngDelta,
                minCachedAt = ttlThreshold
            )
            if (cached.isNotEmpty()) {
                Result.success(cached.map { entity ->
                    val isHidden = HiddenPoolsManager.isHidden(context, entity.placeId)
                    val isFavorite = FavoritesManager.isFavorite(context, entity.placeId)
                    entity.toDomain().copy(isHidden = isHidden, isFavorite = isFavorite)
                })
            } else {
                Result.failure(e)
            }
        }
    }

    companion object {
        private val BASIC_TTL_MS = 7 * 24 * 60 * 60 * 1000L
    }
}
```

- [ ] **Step 4: Run all unit tests to verify nothing is broken**

```
./gradlew :app:testDebugUnitTest
```

Expected: all unit tests PASS (ExploreRepositoryImplIntegrationTest 3 tests + all existing tests).

- [ ] **Step 5: Verify the full app compiles**

```
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/burixer85/piscinamap/features/explore/data/repository/ExploreRepositoryImpl.kt
git add app/src/test/java/com/burixer85/piscinamap/features/explore/data/repository/ExploreRepositoryImplIntegrationTest.kt
git commit -m "feat: add network-first offline cache fallback to ExploreRepositoryImpl"
```
