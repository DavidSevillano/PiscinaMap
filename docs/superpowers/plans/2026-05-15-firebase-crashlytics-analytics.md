# Firebase Crashlytics + Analytics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate Firebase Crashlytics and Analytics into PiscinaMap so that crashes, non-fatal errors, and key user interactions are observable in production.

**Architecture:** A thin `AnalyticsManager` interface decouples all Firebase calls from the rest of the app. Hilt provides `FirebaseAnalyticsManager` in release builds and `NoOpAnalyticsManager` in debug builds via separate Gradle source sets — no `BuildConfig.DEBUG` checks at runtime. ViewModels own the analytics calls; Screens call ViewModel methods for actions that currently happen inline in composables.

**Tech Stack:** Firebase BOM 33.7.0, firebase-analytics-ktx, firebase-crashlytics-ktx, google-services plugin 4.4.2, crashlytics plugin 3.0.2, Hilt, mockk 1.13.8, kotlinx-coroutines-test.

---

## File Map

| Action | Path |
|---|---|
| Modify | `gradle/libs.versions.toml` |
| Modify | `build.gradle.kts` (root) |
| Modify | `app/build.gradle.kts` |
| Add | `app/google-services.json` (manual step) |
| Create | `app/src/main/java/com/burixer85/piscinamap/core/analytics/AnalyticsManager.kt` |
| Create | `app/src/main/java/com/burixer85/piscinamap/core/analytics/NoOpAnalyticsManager.kt` |
| Create | `app/src/main/java/com/burixer85/piscinamap/core/analytics/FirebaseAnalyticsManager.kt` |
| Create | `app/src/release/java/com/burixer85/piscinamap/core/di/AnalyticsModule.kt` |
| Create | `app/src/debug/java/com/burixer85/piscinamap/core/di/AnalyticsModule.kt` |
| Create | `app/src/test/java/com/burixer85/piscinamap/core/analytics/NoOpAnalyticsManagerTest.kt` |
| Create | `app/src/test/java/com/burixer85/piscinamap/core/analytics/FirebaseAnalyticsManagerTest.kt` |
| Modify | `app/src/main/java/com/burixer85/piscinamap/features/detail/presentation/DetailViewModel.kt` |
| Modify | `app/src/test/java/com/burixer85/piscinamap/features/detail/presentation/DetailViewModelTest.kt` |
| Modify | `app/src/main/java/com/burixer85/piscinamap/features/detail/presentation/DetailScreen.kt` |
| Modify | `app/src/main/java/com/burixer85/piscinamap/features/home/presentation/HomeViewmodel.kt` |
| Modify | `app/src/main/java/com/burixer85/piscinamap/features/home/presentation/HomeScreen.kt` |
| Modify | `app/src/main/java/com/burixer85/piscinamap/features/explore/presentation/ExploreViewModel.kt` |
| Modify | `app/src/test/java/com/burixer85/piscinamap/features/explore/presentation/ExploreViewModelTest.kt` |

---

## Task 1: Firebase Gradle setup

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`
- Manual: `app/google-services.json`

- [ ] **Step 1: Add versions and libraries to `gradle/libs.versions.toml`**

In the `[versions]` section, add:
```toml
firebase-bom = "33.7.0"
google-services = "4.4.2"
firebase-crashlytics-plugin = "3.0.2"
```

In the `[libraries]` section, add:
```toml
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebase-bom" }
firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics-ktx" }
firebase-crashlytics = { group = "com.google.firebase", name = "firebase-crashlytics-ktx" }
```

In the `[plugins]` section, add:
```toml
google-services = { id = "com.google.gms.google-services", version.ref = "google-services" }
firebase-crashlytics-plugin = { id = "com.google.firebase.crashlytics", version.ref = "firebase-crashlytics-plugin" }
```

- [ ] **Step 2: Apply plugins in root `build.gradle.kts`**

Add two entries to the `plugins {}` block:
```kotlin
alias(libs.plugins.google.services) apply false
alias(libs.plugins.firebase.crashlytics.plugin) apply false
```

- [ ] **Step 3: Apply plugins and add dependencies in `app/build.gradle.kts`**

Add to the `plugins {}` block (after existing entries):
```kotlin
alias(libs.plugins.google.services)
alias(libs.plugins.firebase.crashlytics.plugin)
```

Add to the `dependencies {}` block:
```kotlin
// Firebase
implementation(platform(libs.firebase.bom))
implementation(libs.firebase.analytics)
implementation(libs.firebase.crashlytics)
```

- [ ] **Step 4: Place `google-services.json`**

Copy the `google-services.json` file downloaded from the Firebase Console into `app/google-services.json`.

- [ ] **Step 5: Verify the build compiles**

Run:
```
.\gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

---

## Task 2: `AnalyticsManager` interface + `NoOpAnalyticsManager` (TDD)

**Files:**
- Create: `app/src/test/java/com/burixer85/piscinamap/core/analytics/NoOpAnalyticsManagerTest.kt`
- Create: `app/src/main/java/com/burixer85/piscinamap/core/analytics/AnalyticsManager.kt`
- Create: `app/src/main/java/com/burixer85/piscinamap/core/analytics/NoOpAnalyticsManager.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/burixer85/piscinamap/core/analytics/NoOpAnalyticsManagerTest.kt`:
```kotlin
package com.burixer85.piscinamap.core.analytics

import org.junit.Test

class NoOpAnalyticsManagerTest {

    private val sut = NoOpAnalyticsManager()

    @Test
    fun `trackScreen does not throw`() {
        sut.trackScreen("HomeScreen")
    }

    @Test
    fun `trackEvent does not throw`() {
        sut.trackEvent("pool_detail_viewed", mapOf("pool_id" to "abc123"))
    }

    @Test
    fun `trackEvent with empty params does not throw`() {
        sut.trackEvent("map_area_searched")
    }

    @Test
    fun `logNonFatalError does not throw`() {
        sut.logNonFatalError(RuntimeException("test error"))
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Run:
```
.\gradlew :app:testDebugUnitTest --tests "com.burixer85.piscinamap.core.analytics.NoOpAnalyticsManagerTest"
```
Expected: FAILED — `NoOpAnalyticsManager` does not exist yet.

- [ ] **Step 3: Create the `AnalyticsManager` interface**

Create `app/src/main/java/com/burixer85/piscinamap/core/analytics/AnalyticsManager.kt`:
```kotlin
package com.burixer85.piscinamap.core.analytics

interface AnalyticsManager {
    fun trackScreen(screenName: String)
    fun trackEvent(name: String, params: Map<String, String> = emptyMap())
    fun logNonFatalError(throwable: Throwable)
}
```

- [ ] **Step 4: Create `NoOpAnalyticsManager`**

Create `app/src/main/java/com/burixer85/piscinamap/core/analytics/NoOpAnalyticsManager.kt`:
```kotlin
package com.burixer85.piscinamap.core.analytics

import javax.inject.Inject

class NoOpAnalyticsManager @Inject constructor() : AnalyticsManager {
    override fun trackScreen(screenName: String) = Unit
    override fun trackEvent(name: String, params: Map<String, String>) = Unit
    override fun logNonFatalError(throwable: Throwable) = Unit
}
```

- [ ] **Step 5: Run the test to confirm it passes**

Run:
```
.\gradlew :app:testDebugUnitTest --tests "com.burixer85.piscinamap.core.analytics.NoOpAnalyticsManagerTest"
```
Expected: BUILD SUCCESSFUL, 4 tests passed.

- [ ] **Step 6: Commit**

```
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts app/google-services.json app/src/main/java/com/burixer85/piscinamap/core/analytics/ app/src/test/java/com/burixer85/piscinamap/core/analytics/NoOpAnalyticsManagerTest.kt
git commit -m "feat: add Firebase deps and AnalyticsManager interface with NoOp implementation"
```

---

## Task 3: `FirebaseAnalyticsManager` (TDD)

**Files:**
- Create: `app/src/test/java/com/burixer85/piscinamap/core/analytics/FirebaseAnalyticsManagerTest.kt`
- Create: `app/src/main/java/com/burixer85/piscinamap/core/analytics/FirebaseAnalyticsManager.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/burixer85/piscinamap/core/analytics/FirebaseAnalyticsManagerTest.kt`:
```kotlin
package com.burixer85.piscinamap.core.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FirebaseAnalyticsManagerTest {

    private val firebaseAnalytics: FirebaseAnalytics = mockk(relaxed = true)
    private val crashlytics: FirebaseCrashlytics = mockk(relaxed = true)
    private lateinit var sut: FirebaseAnalyticsManager

    @Before
    fun setUp() {
        sut = FirebaseAnalyticsManager(firebaseAnalytics, crashlytics)
    }

    @Test
    fun `trackScreen logs SCREEN_VIEW event with screen_name param`() {
        val bundleSlot = slot<Bundle>()

        sut.trackScreen("HomeScreen")

        verify { firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, capture(bundleSlot)) }
        assertEquals("HomeScreen", bundleSlot.captured.getString(FirebaseAnalytics.Param.SCREEN_NAME))
    }

    @Test
    fun `trackEvent logs custom event with all params in bundle`() {
        val bundleSlot = slot<Bundle>()

        sut.trackEvent("pool_detail_viewed", mapOf("pool_id" to "abc", "pool_name" to "Piscina Test"))

        verify { firebaseAnalytics.logEvent("pool_detail_viewed", capture(bundleSlot)) }
        assertEquals("abc", bundleSlot.captured.getString("pool_id"))
        assertEquals("Piscina Test", bundleSlot.captured.getString("pool_name"))
    }

    @Test
    fun `trackEvent with empty params logs event with empty bundle`() {
        sut.trackEvent("map_area_searched")

        verify { firebaseAnalytics.logEvent("map_area_searched", any()) }
    }

    @Test
    fun `logNonFatalError records exception to Crashlytics`() {
        val ex = RuntimeException("network failure")

        sut.logNonFatalError(ex)

        verify { crashlytics.recordException(ex) }
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Run:
```
.\gradlew :app:testDebugUnitTest --tests "com.burixer85.piscinamap.core.analytics.FirebaseAnalyticsManagerTest"
```
Expected: FAILED — `FirebaseAnalyticsManager` does not exist yet.

- [ ] **Step 3: Create `FirebaseAnalyticsManager`**

Create `app/src/main/java/com/burixer85/piscinamap/core/analytics/FirebaseAnalyticsManager.kt`:
```kotlin
package com.burixer85.piscinamap.core.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject

class FirebaseAnalyticsManager @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
    private val crashlytics: FirebaseCrashlytics
) : AnalyticsManager {

    override fun trackScreen(screenName: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    override fun trackEvent(name: String, params: Map<String, String>) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) -> putString(key, value) }
        }
        firebaseAnalytics.logEvent(name, bundle)
    }

    override fun logNonFatalError(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }
}
```

- [ ] **Step 4: Run the test to confirm it passes**

Run:
```
.\gradlew :app:testDebugUnitTest --tests "com.burixer85.piscinamap.core.analytics.FirebaseAnalyticsManagerTest"
```
Expected: BUILD SUCCESSFUL, 4 tests passed.

- [ ] **Step 5: Commit**

```
git add app/src/main/java/com/burixer85/piscinamap/core/analytics/FirebaseAnalyticsManager.kt app/src/test/java/com/burixer85/piscinamap/core/analytics/FirebaseAnalyticsManagerTest.kt
git commit -m "feat: add FirebaseAnalyticsManager implementation"
```

---

## Task 4: Hilt modules (release + debug source sets)

**Files:**
- Create: `app/src/release/java/com/burixer85/piscinamap/core/di/AnalyticsModule.kt`
- Create: `app/src/debug/java/com/burixer85/piscinamap/core/di/AnalyticsModule.kt`

- [ ] **Step 1: Create the release source set directories**

Create the directory structure manually or run:
```
New-Item -ItemType Directory -Force -Path "app\src\release\java\com\burixer85\piscinamap\core\di"
New-Item -ItemType Directory -Force -Path "app\src\debug\java\com\burixer85\piscinamap\core\di"
```

- [ ] **Step 2: Create `AnalyticsModule` for release**

Create `app/src/release/java/com/burixer85/piscinamap/core/di/AnalyticsModule.kt`:
```kotlin
package com.burixer85.piscinamap.core.di

import android.content.Context
import com.burixer85.piscinamap.core.analytics.AnalyticsManager
import com.burixer85.piscinamap.core.analytics.FirebaseAnalyticsManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsManager(impl: FirebaseAnalyticsManager): AnalyticsManager

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics =
            FirebaseAnalytics.getInstance(context)

        @Provides
        @Singleton
        fun provideFirebaseCrashlytics(): FirebaseCrashlytics =
            FirebaseCrashlytics.getInstance()
    }
}
```

- [ ] **Step 3: Create `AnalyticsModule` for debug**

Create `app/src/debug/java/com/burixer85/piscinamap/core/di/AnalyticsModule.kt`:
```kotlin
package com.burixer85.piscinamap.core.di

import com.burixer85.piscinamap.core.analytics.AnalyticsManager
import com.burixer85.piscinamap.core.analytics.NoOpAnalyticsManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsManager(impl: NoOpAnalyticsManager): AnalyticsManager
}
```

- [ ] **Step 4: Verify both variants compile**

Run:
```
.\gradlew assembleDebug assembleRelease
```
Expected: BUILD SUCCESSFUL for both variants.

- [ ] **Step 5: Commit**

```
git add app/src/release/ app/src/debug/
git commit -m "feat: add Hilt AnalyticsModule for debug and release source sets"
```

---

## Task 5: Wire `DetailViewModel` with analytics (TDD)

**Files:**
- Modify: `app/src/test/java/com/burixer85/piscinamap/features/detail/presentation/DetailViewModelTest.kt`
- Modify: `app/src/main/java/com/burixer85/piscinamap/features/detail/presentation/DetailViewModel.kt`

The `DetailViewModel` needs to:
1. Track `DetailScreen` on every `loadPoolDetails` call.
2. Log `pool_detail_viewed` with `pool_id` and `pool_name` on success.
3. Log non-fatal errors via `logNonFatalError` on failure.
4. Expose `onFavoriteToggled(poolId, isFavorite)`, `onPoolHidden(poolId)`, `onPoolUnhidden(poolId)` methods so `DetailScreen` can call them (the screen still handles the persistence + PoolStateManager calls; the ViewModel only logs analytics).

- [ ] **Step 1: Update `DetailViewModelTest` with analytics assertions**

Replace the full content of `DetailViewModelTest.kt` with:
```kotlin
package com.burixer85.piscinamap.features.detail.presentation

import com.burixer85.piscinamap.core.analytics.AnalyticsManager
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.detail.domain.usecases.GetPoolDetailsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var useCase: GetPoolDetailsUseCase
    private lateinit var analytics: AnalyticsManager
    private lateinit var viewModel: DetailViewModel

    private val fakePool = Pool(
        id = "pool1",
        name = "Piscina Test",
        latitude = 40.0,
        longitude = -3.0,
        address = "Calle Test 1",
        rating = 4.5f,
        isOpenNow = true
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        useCase = mockk()
        analytics = mockk(relaxed = true)
        viewModel = DetailViewModel(useCase, analytics)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has isLoading true`() {
        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.pool)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `setPoolId triggers load and emits success state`() = runTest {
        coEvery { useCase("pool1") } returns Result.success(fakePool)

        viewModel.setPoolId("pool1")

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(fakePool, state.pool)
        assertNull(state.error)
    }

    @Test
    fun `setPoolId triggers load and emits error state on failure`() = runTest {
        coEvery { useCase("pool1") } returns Result.failure(Exception("Network error"))

        viewModel.setPoolId("pool1")

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.pool)
        assertEquals("Network error", state.error)
    }

    @Test
    fun `setPoolId with same id does not reload`() = runTest {
        coEvery { useCase("pool1") } returns Result.success(fakePool)

        viewModel.setPoolId("pool1")
        viewModel.setPoolId("pool1")

        coVerify(exactly = 1) { useCase("pool1") }
    }

    @Test
    fun `setPoolId with different id triggers new load`() = runTest {
        val fakePool2 = fakePool.copy(id = "pool2", name = "Piscina 2")
        coEvery { useCase("pool1") } returns Result.success(fakePool)
        coEvery { useCase("pool2") } returns Result.success(fakePool2)

        viewModel.setPoolId("pool1")
        viewModel.setPoolId("pool2")

        assertEquals(fakePool2, viewModel.uiState.value.pool)
    }

    @Test
    fun `retry reloads pool details`() = runTest {
        coEvery { useCase("pool1") } returns Result.success(fakePool)
        viewModel.setPoolId("pool1")

        val updatedPool = fakePool.copy(name = "Piscina Actualizada")
        coEvery { useCase("pool1") } returns Result.success(updatedPool)
        viewModel.retry()

        assertEquals("Piscina Actualizada", viewModel.uiState.value.pool?.name)
    }

    @Test
    fun `retry after error clears error and shows pool on success`() = runTest {
        coEvery { useCase("pool1") } returns Result.failure(Exception("Error"))
        viewModel.setPoolId("pool1")

        coEvery { useCase("pool1") } returns Result.success(fakePool)
        viewModel.retry()

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertEquals(fakePool, state.pool)
    }

    @Test
    fun `loadPoolDetails tracks DetailScreen and pool_detail_viewed on success`() = runTest {
        coEvery { useCase("pool1") } returns Result.success(fakePool)

        viewModel.setPoolId("pool1")

        verify { analytics.trackScreen("DetailScreen") }
        verify {
            analytics.trackEvent(
                "pool_detail_viewed",
                mapOf("pool_id" to "pool1", "pool_name" to "Piscina Test")
            )
        }
    }

    @Test
    fun `loadPoolDetails logs non-fatal error on failure`() = runTest {
        val exception = Exception("Network error")
        coEvery { useCase("pool1") } returns Result.failure(exception)

        viewModel.setPoolId("pool1")

        verify { analytics.logNonFatalError(exception) }
    }

    @Test
    fun `onFavoriteToggled tracks favorite_added when isFavorite is true`() {
        viewModel.onFavoriteToggled("pool1", true)

        verify { analytics.trackEvent("favorite_added", mapOf("pool_id" to "pool1")) }
    }

    @Test
    fun `onFavoriteToggled tracks favorite_removed when isFavorite is false`() {
        viewModel.onFavoriteToggled("pool1", false)

        verify { analytics.trackEvent("favorite_removed", mapOf("pool_id" to "pool1")) }
    }

    @Test
    fun `onPoolHidden tracks pool_hidden event`() {
        viewModel.onPoolHidden("pool1")

        verify { analytics.trackEvent("pool_hidden", mapOf("pool_id" to "pool1")) }
    }

    @Test
    fun `onPoolUnhidden tracks pool_unhidden event`() {
        viewModel.onPoolUnhidden("pool1")

        verify { analytics.trackEvent("pool_unhidden", mapOf("pool_id" to "pool1")) }
    }
}
```

- [ ] **Step 2: Run the tests to confirm they fail**

Run:
```
.\gradlew :app:testDebugUnitTest --tests "com.burixer85.piscinamap.features.detail.presentation.DetailViewModelTest"
```
Expected: FAILED — `DetailViewModel` constructor does not accept `AnalyticsManager` yet.

- [ ] **Step 3: Update `DetailViewModel`**

Replace the full content of `DetailViewModel.kt` with:
```kotlin
package com.burixer85.piscinamap.features.detail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burixer85.piscinamap.core.analytics.AnalyticsManager
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.detail.domain.usecases.GetPoolDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val getPoolDetailsUseCase: GetPoolDetailsUseCase,
    private val analytics: AnalyticsManager
) : ViewModel() {

    private var poolId: String = ""

    private val _uiState = MutableStateFlow(DetailUiState(isLoading = true))
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun setPoolId(id: String) {
        if (id != poolId) {
            poolId = id
            loadPoolDetails()
        }
    }

    private fun loadPoolDetails() {
        if (poolId.isBlank()) return
        analytics.trackScreen("DetailScreen")
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = getPoolDetailsUseCase(poolId)
            result.fold(
                onSuccess = { pool ->
                    analytics.trackEvent(
                        "pool_detail_viewed",
                        mapOf("pool_id" to pool.id, "pool_name" to pool.name)
                    )
                    _uiState.update { it.copy(pool = pool, isLoading = false, error = null) }
                },
                onFailure = { error ->
                    analytics.logNonFatalError(error)
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
            )
        }
    }

    fun onFavoriteToggled(poolId: String, isFavorite: Boolean) {
        val eventName = if (isFavorite) "favorite_added" else "favorite_removed"
        analytics.trackEvent(eventName, mapOf("pool_id" to poolId))
    }

    fun onPoolHidden(poolId: String) {
        analytics.trackEvent("pool_hidden", mapOf("pool_id" to poolId))
    }

    fun onPoolUnhidden(poolId: String) {
        analytics.trackEvent("pool_unhidden", mapOf("pool_id" to poolId))
    }

    fun retry() {
        loadPoolDetails()
    }
}

data class DetailUiState(
    val isLoading: Boolean = false,
    val pool: Pool? = null,
    val error: String? = null
)
```

- [ ] **Step 4: Run the tests to confirm they pass**

Run:
```
.\gradlew :app:testDebugUnitTest --tests "com.burixer85.piscinamap.features.detail.presentation.DetailViewModelTest"
```
Expected: BUILD SUCCESSFUL, all tests passed.

- [ ] **Step 5: Commit**

```
git add app/src/main/java/com/burixer85/piscinamap/features/detail/presentation/DetailViewModel.kt app/src/test/java/com/burixer85/piscinamap/features/detail/presentation/DetailViewModelTest.kt
git commit -m "feat: add analytics tracking to DetailViewModel"
```

---

## Task 6: Update `DetailScreen` to call analytics methods

**Files:**
- Modify: `app/src/main/java/com/burixer85/piscinamap/features/detail/presentation/DetailScreen.kt`

The screen currently calls `PoolStateManager.emit*` and manages persistence inline. We add calls to the ViewModel's analytics methods alongside the existing logic — no logic is removed.

- [ ] **Step 1: Update `onHidePool`, `onUnhidePool`, and `onFavoriteToggle` lambdas in `DetailScreen`**

In `DetailScreen.kt`, locate the block starting around `onHidePool = {` and update it. The existing logic for persistence + PoolStateManager stays; add a ViewModel call after each:

```kotlin
onHidePool = {
    showMenu = false
    scope.launch {
        delay(100)
        HiddenPoolsManager.hidePool(context, poolId)
        isHidden = true
        PoolStateManager.emitHiddenStateChange(poolId, true)
        viewModel.onPoolHidden(poolId)
    }
},
onUnhidePool = {
    showMenu = false
    scope.launch {
        delay(100)
        HiddenPoolsManager.showPool(context, poolId)
        isHidden = false
        PoolStateManager.emitHiddenStateChange(poolId, false)
        viewModel.onPoolUnhidden(poolId)
    }
},
onFavoriteToggle = {
    val newState = !isFavorite
    if (newState) {
        FavoritesManager.addFavorite(context, poolId)
    } else {
        FavoritesManager.removeFavorite(context, poolId)
    }
    isFavorite = newState
    PoolStateManager.emitFavoriteStateChange(poolId, newState)
    viewModel.onFavoriteToggled(poolId, newState)
},
```

- [ ] **Step 2: Verify the app compiles**

Run:
```
.\gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```
git add app/src/main/java/com/burixer85/piscinamap/features/detail/presentation/DetailScreen.kt
git commit -m "feat: wire DetailScreen to call analytics methods on hide/unhide/favorite"
```

---

## Task 7: Wire `HomeViewModel` with analytics (TDD)

**Files:**
- Modify: `app/src/main/java/com/burixer85/piscinamap/features/home/presentation/HomeViewmodel.kt`

`HomeViewModel` needs:
1. Track `search_performed` event in `onPredictionSelected` (when the user selects a place from autocomplete).
2. Expose `onFavoriteToggled(poolId, isFavorite)` so `HomeScreen` can call it when toggling favorites in the map bottom sheet.

- [ ] **Step 1: Add `AnalyticsManager` to `HomeViewModel` constructor**

In `HomeViewmodel.kt`, update the constructor and add the analytics calls. Replace the constructor and add the new method. The full updated file:

```kotlin
package com.burixer85.piscinamap.features.home.presentation

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.burixer85.piscinamap.core.analytics.AnalyticsManager
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.presentation.util.PoolStateManager
import com.burixer85.piscinamap.core.presentation.util.ViewModelHolder
import com.burixer85.piscinamap.core.presentation.util.PoolUtils.getGooglePhotoUrl
import com.burixer85.piscinamap.features.home.domain.usecases.GetNearbyPoolsUseCase
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getNearbyPoolsUseCase: GetNearbyPoolsUseCase,
    private val analytics: AnalyticsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState(isLoading = true))
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _events = Channel<HomeEvent>()
    val events = _events.receiveAsFlow()

    private var lastSearchLocation: LatLng? = null

    private var sessionToken: AutocompleteSessionToken? = null

    private var lastMoveTime = 0L

    private val hiddenStateListener: (String, Boolean) -> Unit = { poolId, isHidden ->
        updatePoolHiddenState(poolId, isHidden)
    }

    private val favoriteStateListener: (String, Boolean) -> Unit = { poolId, isFavorite ->
        updatePoolFavoriteState(poolId, isFavorite)
    }

    init {
        analytics.trackScreen("HomeScreen")
        PoolStateManager.subscribe(hiddenStateListener)
        PoolStateManager.subscribeFavorite(favoriteStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        PoolStateManager.unsubscribe(hiddenStateListener)
        PoolStateManager.unsubscribeFavorite(favoriteStateListener)
    }

    fun onMapMoved(currentCenter: LatLng, isCameraMoving: Boolean) {
        val currentTime = System.currentTimeMillis()
        if (isCameraMoving && (currentTime - lastMoveTime < 150)) return
        lastMoveTime = currentTime

        val lastLocation = lastSearchLocation

        if (isCameraMoving || lastLocation == null) {
            if (_uiState.value.showSearchButton) {
                _uiState.update { it.copy(showSearchButton = false) }
            }
            return
        }

        val results = FloatArray(1)
        Location.distanceBetween(
            lastLocation.latitude, lastLocation.longitude,
            currentCenter.latitude, currentCenter.longitude,
            results
        )

        val shouldShow = results[0] > 1500
        if (_uiState.value.showSearchButton != shouldShow) {
            _uiState.update { it.copy(showSearchButton = shouldShow) }
        }
    }

    private fun preloadPoolImages(context: Context, pools: List<Pool>) {
        val imageLoader = Coil.imageLoader(context)
        pools.forEach { pool ->
            val fullUrl = getGooglePhotoUrl(pool.photoUrl)
            fullUrl?.let { url ->
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(Size.ORIGINAL)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }

    fun fetchPools(
        latitude: Double,
        longitude: Double,
        context: Context,
        isManual: Boolean = false
    ) {
        val newLocation = LatLng(latitude, longitude)

        _uiState.update { it.copy(isLoading = true, errorMessage = null, showSearchButton = false) }

        viewModelScope.launch {
            try {
                val result = withTimeout(30_000) {
                    getNearbyPoolsUseCase(latitude, longitude)
                }

                result.fold(
                    onSuccess = { incomingPools ->
                        preloadPoolImages(context, incomingPools)

                        val currentPoolIds = _uiState.value.pools.map { it.id }.toSet()
                        val realNewPools = incomingPools.filter { it.id !in currentPoolIds }

                        if (isManual) {
                            val centerLatLng = if (realNewPools.isNotEmpty()) {
                                val avgLat = realNewPools.map { it.latitude }.average()
                                val avgLng = realNewPools.map { it.longitude }.average()
                                LatLng(avgLat, avgLng)
                            } else {
                                newLocation
                            }
                            _events.send(HomeEvent.ShowToast(realNewPools.size, centerLatLng))
                        }

                        lastSearchLocation = newLocation

                        _uiState.update { currentState ->
                            val finalPools = if (isManual) {
                                val oldPools = currentState.pools.map { it.copy(isNew = false) }
                                val newPools = incomingPools.map { pool ->
                                    pool.copy(isNew = pool.id !in currentPoolIds)
                                }
                                (oldPools + newPools).distinctBy { it.id }
                            } else {
                                incomingPools.map { it.copy(isNew = false) }
                            }
                            currentState.copy(pools = finalPools, isLoading = false)
                        }
                    },
                    onFailure = { error ->
                        analytics.logNonFatalError(error)
                        _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                    }
                )
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is kotlinx.coroutines.TimeoutCancellationException -> "Tiempo de espera agotado"
                    else -> e.message ?: "Error desconocido"
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMessage) }
            }
        }
    }

    fun onPredictionSelected(prediction: AutocompletePrediction, context: Context) {
        _uiState.update { it.copy(isLoading = true, predictions = emptyList()) }

        analytics.trackEvent(
            "search_performed",
            mapOf("query" to prediction.getPrimaryText(null).toString())
        )

        val placesClient = Places.createClient(context.applicationContext)
        val placeFields = listOf(Place.Field.NAME, Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.builder(prediction.placeId, placeFields).build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val latLng = response.place.latLng
                if (latLng != null) {
                    sessionToken = null

                    viewModelScope.launch {
                        _events.send(HomeEvent.AnimateToLocation(latLng))
                    }

                    viewModelScope.launch {
                        try {
                            val result = getNearbyPoolsUseCase(latLng.latitude, latLng.longitude)
                            result.fold(
                                onSuccess = { pools ->
                                    val centerLatLng = LatLng(latLng.latitude, latLng.longitude)
                                    _events.send(HomeEvent.ShowToast(pools.size, centerLatLng))
                                    _uiState.update {
                                        it.copy(
                                            searchText = response.place.name ?: "",
                                            pools = pools,
                                            isLoading = false
                                        )
                                    }
                                },
                                onFailure = { error ->
                                    analytics.logNonFatalError(error)
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            errorMessage = error.message
                                        )
                                    }
                                }
                            )
                        } catch (e: Exception) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = e.message
                                )
                            }
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Ubicación no disponible"
                        )
                    }
                }
            }
            .addOnFailureListener { exception ->
                analytics.logNonFatalError(exception)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error: ${exception.message}"
                    )
                }
            }
    }

    fun onSearchTextChange(newText: String, context: Context) {
        _uiState.update { it.copy(searchText = newText) }

        if (newText.isBlank() || newText.length < 3) {
            _uiState.update { it.copy(predictions = emptyList()) }
            return
        }

        if (sessionToken == null) sessionToken = AutocompleteSessionToken.newInstance()

        val applicationContext = context.applicationContext
        if (applicationContext == null) {
            return
        }

        try {
            val placesClient = Places.createClient(applicationContext)
            val request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(sessionToken)
                .setQuery(newText)
                .build()

            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    _uiState.update { it.copy(predictions = response.autocompletePredictions) }
                }
        } catch (e: Exception) {
        }
    }

    fun onMarkerClicked(poolId: String) {
        val selectedPool = _uiState.value.pools.find { it.id == poolId }

        selectedPool?.let { pool ->
            viewModelScope.launch {
                _events.send(HomeEvent.AnimateToLocation(LatLng(pool.latitude, pool.longitude)))
            }
        }

        _uiState.update { currentState ->
            val updatedPools = currentState.pools.map { pool ->
                if (pool.id == poolId) pool.copy(isNew = false) else pool
            }
            currentState.copy(pools = updatedPools)
        }
    }

    fun clearPredictions() {
        _uiState.update { it.copy(predictions = emptyList()) }
    }

    fun updatePoolHiddenState(poolId: String, isHidden: Boolean) {
        _uiState.update { currentState ->
            val updatedPools = currentState.pools.map { pool ->
                if (pool.id == poolId) pool.copy(isHidden = isHidden) else pool
            }
            currentState.copy(pools = updatedPools)
        }
    }

    private fun updatePoolFavoriteState(poolId: String, isFavorite: Boolean) {
        _uiState.update { currentState ->
            val updatedPools = currentState.pools.map { pool ->
                if (pool.id == poolId) pool.copy(isFavorite = isFavorite) else pool
            }
            currentState.copy(pools = updatedPools)
        }
    }

    fun onFavoriteToggled(poolId: String, isFavorite: Boolean) {
        val eventName = if (isFavorite) "favorite_added" else "favorite_removed"
        analytics.trackEvent(eventName, mapOf("pool_id" to poolId))
    }
}

data class MapUiState(
    val isLoading: Boolean = false,
    val pools: List<Pool> = emptyList(),
    val userLocation: LatLng? = null,
    val searchText: String = "",
    val searchLocationResult: LatLng? = null,
    val predictions: List<AutocompletePrediction> = emptyList(),
    val showSearchButton: Boolean = false,
    val errorMessage: String? = null
)

sealed class HomeEvent {
    data class AnimateToLocation(val latLng: LatLng) : HomeEvent()
    data class ShowToast(val newPoolsCount: Int, val centerLatLng: LatLng? = null) : HomeEvent()
}
```

- [ ] **Step 2: Verify the build compiles**

Run:
```
.\gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Update `HomeScreen` to call `onFavoriteToggled`**

In `HomeScreen.kt`, locate the `onFavoriteToggle` lambda inside the `PoolDetailCard` call (around line 364). Add a call to `viewModel.onFavoriteToggled` after the existing logic:

```kotlin
onFavoriteToggle = {
    val newState = !currentSelectedPool.isFavorite
    if (newState) FavoritesManager.addFavorite(context, currentSelectedPool.id)
    else FavoritesManager.removeFavorite(context, currentSelectedPool.id)
    PoolStateManager.emitFavoriteStateChange(currentSelectedPool.id, newState)
    viewModel.onFavoriteToggled(currentSelectedPool.id, newState)
}
```

- [ ] **Step 4: Verify the build compiles**

Run:
```
.\gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```
git add app/src/main/java/com/burixer85/piscinamap/features/home/presentation/HomeViewmodel.kt app/src/main/java/com/burixer85/piscinamap/features/home/presentation/HomeScreen.kt
git commit -m "feat: add analytics tracking to HomeViewModel and HomeScreen"
```

---

## Task 8: Wire `ExploreViewModel` with analytics (TDD)

**Files:**
- Modify: `app/src/test/java/com/burixer85/piscinamap/features/explore/presentation/ExploreViewModelTest.kt`
- Modify: `app/src/main/java/com/burixer85/piscinamap/features/explore/presentation/ExploreViewModel.kt`

`ExploreViewModel` needs to track `ExploreScreen` and log `map_area_searched` when pools are fetched.

- [ ] **Step 1: Read existing `ExploreViewModelTest.kt` and add analytics tests**

Open `app/src/test/java/com/burixer85/piscinamap/features/explore/presentation/ExploreViewModelTest.kt`. Add `analytics: AnalyticsManager = mockk(relaxed = true)` to the test setup, pass it to the `ExploreViewModel` constructor, and add these new tests at the end of the class:

```kotlin
// In setUp(), change:
// viewModel = ExploreViewModel(getExploreNearbyPoolsUseCase)
// to:
analytics = mockk(relaxed = true)
viewModel = ExploreViewModel(getExploreNearbyPoolsUseCase, analytics)

// Add these new test methods:
@Test
fun `fetchPools tracks ExploreScreen`() = runTest {
    coEvery { getExploreNearbyPoolsUseCase(any(), any(), any()) } returns Result.success(emptyList())

    viewModel.fetchPools(40.0, -3.0)

    verify { analytics.trackScreen("ExploreScreen") }
}

@Test
fun `fetchPools tracks map_area_searched with coordinates`() = runTest {
    coEvery { getExploreNearbyPoolsUseCase(any(), any(), any()) } returns Result.success(emptyList())

    viewModel.fetchPools(40.4168, -3.7038)

    verify {
        analytics.trackEvent(
            "map_area_searched",
            mapOf("latitude" to "40.4168", "longitude" to "-3.7038")
        )
    }
}

@Test
fun `fetchPools logs non-fatal error on failure`() = runTest {
    val exception = Exception("timeout")
    coEvery { getExploreNearbyPoolsUseCase(any(), any(), any()) } returns Result.failure(exception)

    viewModel.fetchPools(40.0, -3.0)

    verify { analytics.logNonFatalError(exception) }
}
```

Also add `private lateinit var analytics: AnalyticsManager` as a field in the test class, and import `com.burixer85.piscinamap.core.analytics.AnalyticsManager`.

- [ ] **Step 2: Run the tests to confirm they fail**

Run:
```
.\gradlew :app:testDebugUnitTest --tests "com.burixer85.piscinamap.features.explore.presentation.ExploreViewModelTest"
```
Expected: FAILED — `ExploreViewModel` constructor doesn't accept `AnalyticsManager` yet.

- [ ] **Step 3: Update `ExploreViewModel`**

Replace the full content of `ExploreViewModel.kt` with:
```kotlin
package com.burixer85.piscinamap.features.explore.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burixer85.piscinamap.core.analytics.AnalyticsManager
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.explore.domain.usecases.GetExploreNearbyPoolsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val getExploreNearbyPoolsUseCase: GetExploreNearbyPoolsUseCase,
    private val analytics: AnalyticsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState(isLoading = true))
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private var currentLat: Double = 0.0
    private var currentLng: Double = 0.0

    fun fetchPools(lat: Double, lng: Double) {
        currentLat = lat
        currentLng = lng

        analytics.trackScreen("ExploreScreen")
        analytics.trackEvent(
            "map_area_searched",
            mapOf("latitude" to lat.toString(), "longitude" to lng.toString())
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    warning = null,
                    hasSearchedMore = false
                )
            }
            getExploreNearbyPoolsUseCase(lat, lng, 50000).fold(
                onSuccess = { pools ->
                    _uiState.update { it.copy(pools = pools, isLoading = false, error = null) }
                },
                onFailure = { error ->
                    analytics.logNonFatalError(error)
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
            )
        }
    }

    fun fetchMorePools() {
        if (_uiState.value.isLoadingMore || _uiState.value.hasSearchedMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, error = null, hasSearchedMore = true) }
            getExploreNearbyPoolsUseCase(currentLat, currentLng, 50000).fold(
                onSuccess = { newPools ->
                    val existingIds = _uiState.value.pools.map { it.id }.toSet()
                    val uniqueNewPools = newPools.filter { it.id !in existingIds }

                    _uiState.update {
                        it.copy(
                            pools = it.pools + uniqueNewPools,
                            isLoadingMore = false,
                            warning = if (uniqueNewPools.isEmpty()) {
                                "No se encontraron más piscinas en esta área."
                            } else null
                        )
                    }
                },
                onFailure = { error ->
                    analytics.logNonFatalError(error)
                    _uiState.update { it.copy(isLoadingMore = false, error = error.message) }
                }
            )
        }
    }
}

data class ExploreUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val pools: List<Pool> = emptyList(),
    val error: String? = null,
    val warning: String? = null,
    val hasSearchedMore: Boolean = false
)
```

- [ ] **Step 4: Run the tests to confirm they pass**

Run:
```
.\gradlew :app:testDebugUnitTest --tests "com.burixer85.piscinamap.features.explore.presentation.ExploreViewModelTest"
```
Expected: BUILD SUCCESSFUL, all tests passed.

- [ ] **Step 5: Run the full test suite to confirm no regressions**

Run:
```
.\gradlew :app:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL, all tests passed.

- [ ] **Step 6: Commit**

```
git add app/src/main/java/com/burixer85/piscinamap/features/explore/presentation/ExploreViewModel.kt app/src/test/java/com/burixer85/piscinamap/features/explore/presentation/ExploreViewModelTest.kt
git commit -m "feat: add analytics tracking to ExploreViewModel"
```

---

## Task 9: Wire `FavoritesViewModel` with screen tracking

**Files:**
- Modify: `app/src/test/java/com/burixer85/piscinamap/features/favorites/presentation/FavoritesViewModelTest.kt`
- Modify: `app/src/main/java/com/burixer85/piscinamap/features/favorites/presentation/FavoritesViewModel.kt`

`FavoritesViewModel` needs to track `FavoritesScreen` when it is initialized.

- [ ] **Step 1: Read `FavoritesViewModelTest.kt` and add a screen-tracking test**

Open `app/src/test/java/com/burixer85/piscinamap/features/favorites/presentation/FavoritesViewModelTest.kt`. Add `analytics: AnalyticsManager = mockk(relaxed = true)` as a field, pass it to the `FavoritesViewModel` constructor in `setUp()`, and add:

```kotlin
@Test
fun `init tracks FavoritesScreen`() {
    verify { analytics.trackScreen("FavoritesScreen") }
}
```

Also add `import com.burixer85.piscinamap.core.analytics.AnalyticsManager`.

- [ ] **Step 2: Run the test to confirm it fails**

Run:
```
.\gradlew :app:testDebugUnitTest --tests "com.burixer85.piscinamap.features.favorites.presentation.FavoritesViewModelTest"
```
Expected: FAILED — constructor doesn't accept `AnalyticsManager` yet.

- [ ] **Step 3: Update `FavoritesViewModel` constructor and add screen tracking**

In `FavoritesViewModel.kt`, add `private val analytics: AnalyticsManager` to the constructor and call `analytics.trackScreen("FavoritesScreen")` at the top of `init {}`:

```kotlin
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val detailRepository: DetailRepository,
    private val analytics: AnalyticsManager,
) : ViewModel() {
    // ...
    init {
        analytics.trackScreen("FavoritesScreen")
        PoolStateManager.subscribeFavorite(favoriteStateListener)
        loadFavorites()
    }
    // rest of the class unchanged
```

Add `import com.burixer85.piscinamap.core.analytics.AnalyticsManager` to the imports.

- [ ] **Step 4: Run the tests to confirm they pass**

Run:
```
.\gradlew :app:testDebugUnitTest --tests "com.burixer85.piscinamap.features.favorites.presentation.FavoritesViewModelTest"
```
Expected: BUILD SUCCESSFUL, all tests passed.

- [ ] **Step 5: Commit**

```
git add app/src/main/java/com/burixer85/piscinamap/features/favorites/presentation/FavoritesViewModel.kt app/src/test/java/com/burixer85/piscinamap/features/favorites/presentation/FavoritesViewModelTest.kt
git commit -m "feat: add screen tracking to FavoritesViewModel"
```

---

## Task 10: Final build verification

- [ ] **Step 1: Build both variants**

Run:
```
.\gradlew assembleDebug assembleRelease
```
Expected: BUILD SUCCESSFUL for both. If release build fails with missing `google-services.json` or signing config, verify those files are in place.

- [ ] **Step 2: Run full unit test suite one last time**

Run:
```
.\gradlew :app:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL, no failures.

- [ ] **Step 3: Final commit**

```
git add .
git commit -m "feat: Firebase Crashlytics + Analytics integration complete"
```
