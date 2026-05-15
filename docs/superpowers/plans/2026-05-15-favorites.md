# Favorites Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let users save pools as favorites, accessible from the map card and detail screen, with a dedicated Favorites tab and a golden map marker for favorite pools.

**Architecture:** Follows the HiddenPoolsManager pattern exactly — a new `FavoritesManager` singleton for SharedPreferences persistence, a second listener set in `PoolStateManager` for cross-screen sync, `isFavorite: Boolean = false` added to the `Pool` model. A new `features/favorites/` feature hosts `FavoritesViewModel` + `FavoritesScreen` with a `FavoritesRouteNav` destination.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Navigation 3, SharedPreferences, MockK (unit tests), Compose Test Rule (instrumented tests).

---

## File Map

| File | Action |
|------|--------|
| `core/presentation/util/FavoritesManager.kt` | **Create** |
| `core/presentation/util/PoolStateManager.kt` | **Modify** — add favorite listener set |
| `core/domain/model/Pool.kt` | **Modify** — add `isFavorite: Boolean = false` |
| `features/home/data/repository/PoolRepositoryImpl.kt` | **Modify** — populate `isFavorite` in `searchNearbyPools` |
| `features/explore/data/repository/ExploreRepositoryImpl.kt` | **Modify** — populate `isFavorite` in `searchNearbyPools` |
| `res/values/strings.xml` | **Modify** — add 3 new strings |
| `res/values-es/strings.xml` | **Modify** — add 3 new strings |
| `core/presentation/components/PoolDetailContent.kt` | **Modify** — add heart icon to top bar |
| `features/detail/presentation/DetailScreen.kt` | **Modify** — wire `isFavorite` state and callbacks |
| `core/presentation/components/PoolDetailCard.kt` | **Modify** — add heart icon button |
| `features/home/presentation/HomeScreen.kt` | **Modify** — pass favorite callbacks to `PoolDetailCard`, add `poolIconFavorite` |
| `features/home/presentation/HomeViewmodel.kt` | **Modify** — subscribe to favorite events, add `updatePoolFavoriteState` |
| `navigation/NavDestinations.kt` | **Modify** — add `FavoritesRouteNav` |
| `navigation/PiscinaMapNavGraph.kt` | **Modify** — add destination, update bottom bar to 3 tabs |
| `features/favorites/presentation/FavoritesViewModel.kt` | **Create** |
| `features/favorites/presentation/FavoritesScreen.kt` | **Create** |
| `test/.../FavoritesManagerTest.kt` | **Create** |
| `test/.../PoolStateManagerTest.kt` | **Modify** — add favorite listener tests |
| `test/.../FavoritesViewModelTest.kt` | **Create** |
| `androidTest/.../FavoritesContentTest.kt` | **Create** |
| `androidTest/.../DetailContentTest.kt` | **Modify** — add favorite toggle tests |
| `androidTest/.../util/TestData.kt` | **Modify** — add `isFavorite = false` to pool fixture |

---

## Task 1: FavoritesManager

**Files:**
- Create: `app/src/main/java/com/burixer85/piscinamap/core/presentation/util/FavoritesManager.kt`
- Create: `app/src/test/java/com/burixer85/piscinamap/core/presentation/util/FavoritesManagerTest.kt`

- [ ] **Step 1: Create FavoritesManager.kt**

```kotlin
package com.burixer85.piscinamap.core.presentation.util

import android.content.Context
import android.content.SharedPreferences

object FavoritesManager {
    private const val PREFS_NAME = "favorite_pools"
    private const val KEY_FAVORITE_IDS = "favorite_pool_ids"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isFavorite(context: Context, poolId: String): Boolean {
        val ids = getPrefs(context).getStringSet(KEY_FAVORITE_IDS, emptySet()) ?: emptySet()
        return poolId in ids
    }

    fun addFavorite(context: Context, poolId: String) {
        val prefs = getPrefs(context)
        val ids = prefs.getStringSet(KEY_FAVORITE_IDS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        ids.add(poolId)
        prefs.edit().putStringSet(KEY_FAVORITE_IDS, ids).apply()
    }

    fun removeFavorite(context: Context, poolId: String) {
        val prefs = getPrefs(context)
        val ids = prefs.getStringSet(KEY_FAVORITE_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        ids.remove(poolId)
        prefs.edit().putStringSet(KEY_FAVORITE_IDS, ids).apply()
    }

    fun getFavoriteIds(context: Context): Set<String> =
        getPrefs(context).getStringSet(KEY_FAVORITE_IDS, emptySet()) ?: emptySet()
}
```

- [ ] **Step 2: Create FavoritesManagerTest.kt**

```kotlin
package com.burixer85.piscinamap.core.presentation.util

import android.content.Context
import android.content.SharedPreferences
import io.mockk.CapturingSlot
import io.mockk.answers
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FavoritesManagerTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val backingSet = mutableSetOf<String>()
    private val capturedSet: CapturingSlot<Set<String>> = slot()

    @Before
    fun setUp() {
        backingSet.clear()
        editor = mockk(relaxed = true)
        prefs = mockk()
        context = mockk()

        every { context.getSharedPreferences("favorite_pools", Context.MODE_PRIVATE) } returns prefs
        every { prefs.getStringSet("favorite_pool_ids", any()) } answers { backingSet.toSet() }
        every { prefs.edit() } returns editor
        every { editor.putStringSet("favorite_pool_ids", capture(capturedSet)) } answers {
            backingSet.clear()
            backingSet.addAll(capturedSet.captured)
            editor
        }
    }

    @Test
    fun `isFavorite returns false when set is empty`() {
        assertFalse(FavoritesManager.isFavorite(context, "pool1"))
    }

    @Test
    fun `addFavorite makes isFavorite return true`() {
        FavoritesManager.addFavorite(context, "pool1")
        assertTrue(FavoritesManager.isFavorite(context, "pool1"))
    }

    @Test
    fun `removeFavorite makes isFavorite return false`() {
        backingSet.add("pool1")
        FavoritesManager.removeFavorite(context, "pool1")
        assertFalse(FavoritesManager.isFavorite(context, "pool1"))
    }

    @Test
    fun `getFavoriteIds returns all added ids`() {
        backingSet.addAll(listOf("pool1", "pool2"))
        val ids = FavoritesManager.getFavoriteIds(context)
        assertEquals(setOf("pool1", "pool2"), ids)
    }

    @Test
    fun `addFavorite does not duplicate existing id`() {
        FavoritesManager.addFavorite(context, "pool1")
        FavoritesManager.addFavorite(context, "pool1")
        assertEquals(1, FavoritesManager.getFavoriteIds(context).size)
    }

    @Test
    fun `removeFavorite on absent id leaves set unchanged`() {
        backingSet.add("pool2")
        FavoritesManager.removeFavorite(context, "pool99")
        assertEquals(setOf("pool2"), FavoritesManager.getFavoriteIds(context))
    }
}
```

- [ ] **Step 3: Run unit tests**

```
./gradlew testDebugUnitTest --tests "*.FavoritesManagerTest"
```

Expected: 6 tests PASS.

- [ ] **Step 4: Commit**

```
git add app/src/main/java/com/burixer85/piscinamap/core/presentation/util/FavoritesManager.kt
git add app/src/test/java/com/burixer85/piscinamap/core/presentation/util/FavoritesManagerTest.kt
git commit -m "feat: add FavoritesManager for pool favorites persistence"
```

---

## Task 2: Extend PoolStateManager with Favorite Listeners

**Files:**
- Modify: `app/src/main/java/com/burixer85/piscinamap/core/presentation/util/PoolStateManager.kt`
- Modify: `app/src/test/java/com/burixer85/piscinamap/core/presentation/util/PoolStateManagerTest.kt`

- [ ] **Step 1: Extend PoolStateManager.kt**

Replace the entire file content:

```kotlin
package com.burixer85.piscinamap.core.presentation.util

object PoolStateManager {
    private val listeners = mutableListOf<(String, Boolean) -> Unit>()
    private val favoriteListeners = mutableListOf<(String, Boolean) -> Unit>()

    fun subscribe(listener: (String, Boolean) -> Unit) {
        listeners.add(listener)
    }

    fun unsubscribe(listener: (String, Boolean) -> Unit) {
        listeners.remove(listener)
    }

    fun emitHiddenStateChange(poolId: String, isHidden: Boolean) {
        listeners.forEach { it(poolId, isHidden) }
    }

    fun subscribeFavorite(listener: (String, Boolean) -> Unit) {
        favoriteListeners.add(listener)
    }

    fun unsubscribeFavorite(listener: (String, Boolean) -> Unit) {
        favoriteListeners.remove(listener)
    }

    fun emitFavoriteStateChange(poolId: String, isFavorite: Boolean) {
        favoriteListeners.forEach { it(poolId, isFavorite) }
    }
}
```

- [ ] **Step 2: Add favorite listener tests to PoolStateManagerTest.kt**

Append these tests inside the `PoolStateManagerTest` class (before the closing brace). Also add `unsubscribeFavorite` cleanup to `tearDown`. Replace tearDown and add a `registeredFavoriteListeners` field:

At the top of the class add:
```kotlin
private val registeredFavoriteListeners = mutableListOf<(String, Boolean) -> Unit>()
```

Replace `tearDown`:
```kotlin
@After
fun tearDown() {
    registeredListeners.forEach { PoolStateManager.unsubscribe(it) }
    registeredListeners.clear()
    registeredFavoriteListeners.forEach { PoolStateManager.unsubscribeFavorite(it) }
    registeredFavoriteListeners.clear()
}
```

Add helper and new tests:
```kotlin
private fun subscribeFavorite(listener: (String, Boolean) -> Unit) {
    PoolStateManager.subscribeFavorite(listener)
    registeredFavoriteListeners.add(listener)
}

@Test
fun `subscribed favorite listener receives emitted favorite state change`() {
    val received = mutableListOf<Pair<String, Boolean>>()
    subscribeFavorite { poolId, isFav -> received.add(poolId to isFav) }

    PoolStateManager.emitFavoriteStateChange("pool1", true)

    assertEquals(1, received.size)
    assertEquals("pool1" to true, received[0])
}

@Test
fun `unsubscribed favorite listener does not receive events`() {
    val received = mutableListOf<String>()
    val listener: (String, Boolean) -> Unit = { poolId, _ -> received.add(poolId) }

    PoolStateManager.subscribeFavorite(listener)
    PoolStateManager.unsubscribeFavorite(listener)
    PoolStateManager.emitFavoriteStateChange("pool1", true)

    assertTrue(received.isEmpty())
}

@Test
fun `favorite listeners do not receive hidden state events`() {
    val favReceived = mutableListOf<String>()
    subscribeFavorite { poolId, _ -> favReceived.add(poolId) }

    PoolStateManager.emitHiddenStateChange("pool1", true)

    assertTrue(favReceived.isEmpty())
}

@Test
fun `hidden listeners do not receive favorite state events`() {
    val hiddenReceived = mutableListOf<String>()
    subscribe { poolId, _ -> hiddenReceived.add(poolId) }

    PoolStateManager.emitFavoriteStateChange("pool1", true)

    assertTrue(hiddenReceived.isEmpty())
}
```

- [ ] **Step 3: Run tests**

```
./gradlew testDebugUnitTest --tests "*.PoolStateManagerTest"
```

Expected: all tests PASS (old 6 + new 4 = 10 tests).

- [ ] **Step 4: Commit**

```
git add app/src/main/java/com/burixer85/piscinamap/core/presentation/util/PoolStateManager.kt
git add app/src/test/java/com/burixer85/piscinamap/core/presentation/util/PoolStateManagerTest.kt
git commit -m "feat: extend PoolStateManager with favorite state listeners"
```

---

## Task 3: Add isFavorite to Pool Model and Repositories

**Files:**
- Modify: `app/src/main/java/com/burixer85/piscinamap/core/domain/model/Pool.kt`
- Modify: `app/src/main/java/com/burixer85/piscinamap/features/home/data/repository/PoolRepositoryImpl.kt`
- Modify: `app/src/main/java/com/burixer85/piscinamap/features/explore/data/repository/ExploreRepositoryImpl.kt`
- Modify: `app/src/androidTest/java/com/burixer85/piscinamap/util/TestData.kt`

- [ ] **Step 1: Add isFavorite to Pool.kt**

In `Pool.kt`, add `val isFavorite: Boolean = false` after `val isHidden: Boolean = false`:

```kotlin
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
```

- [ ] **Step 2: Update PoolRepositoryImpl.searchNearbyPools to populate isFavorite**

In `PoolRepositoryImpl.kt`, find the `.map { place ->` block inside `searchNearbyPools` and update it:

```kotlin
.map { place ->
    val isHidden = HiddenPoolsManager.isHidden(context, place.placeId)
    val isFavorite = FavoritesManager.isFavorite(context, place.placeId)
    place.toDomain().copy(isHidden = isHidden, isFavorite = isFavorite)
}
```

Add the import at the top of the file:
```kotlin
import com.burixer85.piscinamap.core.presentation.util.FavoritesManager
```

- [ ] **Step 3: Update ExploreRepositoryImpl.searchNearbyPools to populate isFavorite**

In `ExploreRepositoryImpl.kt`, find the `.map { place ->` block and update it:

```kotlin
.map { place ->
    val isHidden = HiddenPoolsManager.isHidden(context, place.placeId)
    val isFavorite = FavoritesManager.isFavorite(context, place.placeId)
    place.toDomain().copy(isHidden = isHidden, isFavorite = isFavorite)
}
```

Add the import:
```kotlin
import com.burixer85.piscinamap.core.presentation.util.FavoritesManager
```

- [ ] **Step 4: Update TestData.kt to include isFavorite field**

In `TestData.kt`, the `pool` object uses named parameters so the new `isFavorite = false` field gets its default value automatically. No change required — verify it compiles.

- [ ] **Step 5: Run unit tests to confirm no regressions**

```
./gradlew testDebugUnitTest
```

Expected: all existing unit tests PASS.

- [ ] **Step 6: Commit**

```
git add app/src/main/java/com/burixer85/piscinamap/core/domain/model/Pool.kt
git add app/src/main/java/com/burixer85/piscinamap/features/home/data/repository/PoolRepositoryImpl.kt
git add app/src/main/java/com/burixer85/piscinamap/features/explore/data/repository/ExploreRepositoryImpl.kt
git commit -m "feat: add isFavorite field to Pool model and populate from FavoritesManager"
```

---

## Task 4: Add String Resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-es/strings.xml`

- [ ] **Step 1: Add strings to values/strings.xml**

Inside the `<!-- NAV / BOTTOM BAR -->` section, after `<string name="favorites">Favorites</string>`, add nothing (already exists). Add a new `<!-- FAVORITES SCREEN -->` section:

```xml
<!-- ========== FAVORITES SCREEN ========== -->
<string name="no_favorites">You don\'t have any favorite pools yet</string>
<string name="add_to_favorites">Add to favorites</string>
<string name="remove_from_favorites">Remove from favorites</string>
```

- [ ] **Step 2: Add strings to values-es/strings.xml**

Add a matching section in the Spanish file:

```xml
<!-- ========== FAVORITES SCREEN ========== -->
<string name="no_favorites">Aún no tienes piscinas favoritas</string>
<string name="add_to_favorites">Añadir a favoritos</string>
<string name="remove_from_favorites">Quitar de favoritos</string>
```

- [ ] **Step 3: Commit**

```
git add app/src/main/res/values/strings.xml
git add app/src/main/res/values-es/strings.xml
git commit -m "feat: add string resources for favorites feature"
```

---

## Task 5: Add Favorite Icon to PoolDetailContent

**Files:**
- Modify: `app/src/main/java/com/burixer85/piscinamap/core/presentation/components/PoolDetailContent.kt`

`PoolDetailContent` renders the top action bar with Back, Share, and MoreVert buttons. The heart icon goes into this same row, between Share and MoreVert.

- [ ] **Step 1: Add isFavorite and onFavoriteToggle parameters to PoolDetailContent**

Update the function signature from:
```kotlin
fun PoolDetailContent(
    pool: Pool,
    onCallClick: (String) -> Unit,
    onBack: () -> Unit,
    onMoreClick: () -> Unit,
    onHideClick: (() -> Unit)? = null
)
```
to:
```kotlin
fun PoolDetailContent(
    pool: Pool,
    onCallClick: (String) -> Unit,
    onBack: () -> Unit,
    onMoreClick: () -> Unit,
    onHideClick: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    onFavoriteToggle: () -> Unit = {}
)
```

- [ ] **Step 2: Add the heart icon in the top bar row**

In `PoolDetailContent`, locate the `Row(horizontalArrangement = Arrangement.spacedBy(8.dp))` that contains `IconBtn(icon = Icons.Default.Share, ...)` and `IconBtn(icon = Icons.Default.MoreVert, ...)`.

Insert the heart button between Share and MoreVert:

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    IconBtn(icon = Icons.Default.Share, size = 18, onClick = {
        val mapsUrl = "https://www.google.com/maps/search/?api=1&query=${pool.latitude},${pool.longitude}"
        val shareText = "${pool.name}\n${pool.address}\n$mapsUrl"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, getString(context, R.string.share_pool)))
    })
    IconBtn(
        icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        size = 18,
        onClick = onFavoriteToggle,
        tint = if (isFavorite) cs.error else cs.onSurfaceVariant
    )
    IconBtn(icon = Icons.Default.MoreVert, size = 18, onClick = onMoreClick)
}
```

Add the needed imports at the top of the file:
```kotlin
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
```

- [ ] **Step 3: Check that IconBtn supports a tint parameter**

Search for the `IconBtn` private composable in `PoolDetailContent.kt`. If it has a fixed tint, add an optional `tint` parameter:

```kotlin
@Composable
private fun IconBtn(
    icon: ImageVector,
    size: Int,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    // keep existing implementation, replace hardcoded tint with the parameter
}
```

- [ ] **Step 4: Build to verify compilation**

```
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```
git add app/src/main/java/com/burixer85/piscinamap/core/presentation/components/PoolDetailContent.kt
git commit -m "feat: add favorite heart icon to pool detail top bar"
```

---

## Task 6: Wire Favorite State in DetailScreen

**Files:**
- Modify: `app/src/main/java/com/burixer85/piscinamap/features/detail/presentation/DetailScreen.kt`
- Modify: `app/src/androidTest/java/com/burixer85/piscinamap/ui/screens/DetailContentTest.kt`

- [ ] **Step 1: Add isFavorite state and onFavoriteToggle to DetailScreen**

In `DetailScreen.kt`, after the existing `var isHidden by remember(poolId) { ... }`, add:

```kotlin
var isFavorite by remember(poolId) {
    mutableStateOf(FavoritesManager.isFavorite(context, poolId))
}
```

Add the `FavoritesManager` import:
```kotlin
import com.burixer85.piscinamap.core.presentation.util.FavoritesManager
```

- [ ] **Step 2: Add isFavorite and onFavoriteToggle to the DetailContent call in DetailScreen**

Update the `DetailContent(...)` call to pass:
```kotlin
DetailContent(
    uiState = uiState,
    isHidden = isHidden,
    isFavorite = isFavorite,
    showMenu = showMenu,
    onRetry = viewModel::retry,
    onCallClick = { phone ->
        val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
        context.startActivity(intent)
    },
    onBack = onBack,
    onMoreClick = { showMenu = true },
    onHidePool = {
        showMenu = false
        scope.launch {
            delay(100)
            HiddenPoolsManager.hidePool(context, poolId)
            isHidden = true
            PoolStateManager.emitHiddenStateChange(poolId, true)
        }
    },
    onUnhidePool = {
        showMenu = false
        scope.launch {
            delay(100)
            HiddenPoolsManager.showPool(context, poolId)
            isHidden = false
            PoolStateManager.emitHiddenStateChange(poolId, false)
        }
    },
    onDismissMenu = { showMenu = false },
    onFavoriteToggle = {
        val newState = !isFavorite
        if (newState) FavoritesManager.addFavorite(context, poolId)
        else FavoritesManager.removeFavorite(context, poolId)
        isFavorite = newState
        PoolStateManager.emitFavoriteStateChange(poolId, newState)
    }
)
```

- [ ] **Step 3: Add isFavorite and onFavoriteToggle parameters to DetailContent**

Update `DetailContent` signature to add:
```kotlin
@Composable
internal fun DetailContent(
    uiState: DetailUiState,
    isHidden: Boolean = false,
    isFavorite: Boolean = false,
    showMenu: Boolean = false,
    onRetry: () -> Unit = {},
    onCallClick: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onHidePool: () -> Unit = {},
    onUnhidePool: () -> Unit = {},
    onDismissMenu: () -> Unit = {},
    onFavoriteToggle: () -> Unit = {},
)
```

- [ ] **Step 4: Pass isFavorite and onFavoriteToggle down to PoolDetailContent**

In `DetailContent`, in the branch where `uiState.pool != null && !isHidden`, update the `PoolDetailContent(...)` call:

```kotlin
PoolDetailContent(
    pool = uiState.pool.copy(isHidden = isHidden),
    onCallClick = onCallClick,
    onBack = onBack,
    onMoreClick = onMoreClick,
    isFavorite = isFavorite,
    onFavoriteToggle = onFavoriteToggle,
)
```

- [ ] **Step 5: Build to confirm compilation**

```
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Add favorite toggle tests to DetailContentTest.kt**

Append these tests inside the `DetailContentTest` class:

```kotlin
@Test
fun showsFavoriteBorderIcon_whenNotFavorited() {
    rule.setContent {
        PiscinaMapTheme {
            DetailContent(
                uiState = DetailUiState(pool = TestData.pool),
                isFavorite = false,
            )
        }
    }
    rule.onNode(
        androidx.compose.ui.test.hasContentDescription(
            ctx.getString(R.string.add_to_favorites)
        )
    ).assertIsDisplayed()
}

@Test
fun showsFavoriteFilledIcon_whenFavorited() {
    rule.setContent {
        PiscinaMapTheme {
            DetailContent(
                uiState = DetailUiState(pool = TestData.pool),
                isFavorite = true,
            )
        }
    }
    rule.onNode(
        androidx.compose.ui.test.hasContentDescription(
            ctx.getString(R.string.remove_from_favorites)
        )
    ).assertIsDisplayed()
}

@Test
fun invokesFavoriteToggle_whenHeartIconClicked() {
    var toggled = false
    rule.setContent {
        PiscinaMapTheme {
            DetailContent(
                uiState = DetailUiState(pool = TestData.pool),
                isFavorite = false,
                onFavoriteToggle = { toggled = true },
            )
        }
    }
    rule.onNode(
        androidx.compose.ui.test.hasContentDescription(
            ctx.getString(R.string.add_to_favorites)
        )
    ).performClick()
    assert(toggled)
}
```

> Note: For these tests to work, the `IconBtn` composable in `PoolDetailContent.kt` must set `contentDescription` on the icon. Update the `IconBtn` that renders the heart to pass `contentDescription = stringResource(if (isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites)` to the `Icon` composable inside it. Check the `IconBtn` implementation and add the content description parameter.

- [ ] **Step 7: Run instrumented tests**

```
./gradlew connectedAndroidTest --tests "*.DetailContentTest"
```

Expected: all tests PASS (original 8 + new 3 = 11 tests).

- [ ] **Step 8: Commit**

```
git add app/src/main/java/com/burixer85/piscinamap/features/detail/presentation/DetailScreen.kt
git add app/src/androidTest/java/com/burixer85/piscinamap/ui/screens/DetailContentTest.kt
git commit -m "feat: add favorite toggle to DetailScreen"
```

---

## Task 7: Add Favorite Toggle to PoolDetailCard (Map Card)

**Files:**
- Modify: `app/src/main/java/com/burixer85/piscinamap/core/presentation/components/PoolDetailCard.kt`
- Modify: `app/src/main/java/com/burixer85/piscinamap/features/home/presentation/HomeScreen.kt`

- [ ] **Step 1: Add isFavorite and onFavoriteToggle to PoolDetailCard**

Update `PoolDetailCard` signature:
```kotlin
@Composable
fun PoolDetailCard(
    pool: Pool,
    onClose: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    isFavorite: Boolean = false,
    onFavoriteToggle: () -> Unit = {},
    modifier: Modifier = Modifier
)
```

- [ ] **Step 2: Add heart icon button next to the phone button**

In `PoolDetailCard`, find the `Row` that contains the Directions button and the Phone icon button. Add a heart button after the Phone button:

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    // existing Directions button (weight(1f))
    Box(
        modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cs.primary)
            .clickable { /* existing directions logic */ },
        contentAlignment = Alignment.Center
    ) { /* existing content */ }

    // existing Phone button — unchanged

    // NEW: Favorite button
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cs.surface)
            .border(1.dp, cs.outlineVariant, RoundedCornerShape(16.dp))
            .clickable { onFavoriteToggle() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isFavorite)
                stringResource(R.string.remove_from_favorites)
            else
                stringResource(R.string.add_to_favorites),
            tint = if (isFavorite) cs.error else cs.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}
```

Add imports:
```kotlin
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
```

- [ ] **Step 3: Update HomeScreen to pass isFavorite and onFavoriteToggle to PoolDetailCard**

In `HomeScreen.kt`, find the `lastSelectedPool?.let { lastPool ->` block and update the `PoolDetailCard` call:

```kotlin
lastSelectedPool?.let { lastPool ->
    val pool = uiState.pools.find { it.id == lastPool.id } ?: lastPool
    PoolDetailCard(
        pool = pool,
        onClose = { selectedPool = null },
        onNavigateToDetail = { id ->
            focusManager.clearFocus()
            CameraStateHolder.isNavigatingToDetail = true
            onNavigateToDetail(id)
        },
        isFavorite = pool.isFavorite,
        onFavoriteToggle = {
            val newState = !pool.isFavorite
            if (newState) FavoritesManager.addFavorite(context, pool.id)
            else FavoritesManager.removeFavorite(context, pool.id)
            PoolStateManager.emitFavoriteStateChange(pool.id, newState)
        }
    )
}
```

Add imports to `HomeScreen.kt`:
```kotlin
import com.burixer85.piscinamap.core.presentation.util.FavoritesManager
```

- [ ] **Step 4: Build to confirm compilation**

```
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```
git add app/src/main/java/com/burixer85/piscinamap/core/presentation/components/PoolDetailCard.kt
git add app/src/main/java/com/burixer85/piscinamap/features/home/presentation/HomeScreen.kt
git commit -m "feat: add favorite toggle to map pool card"
```

---

## Task 8: Favorite Map Marker and HomeViewModel Sync

**Files:**
- Modify: `app/src/main/java/com/burixer85/piscinamap/features/home/presentation/HomeViewmodel.kt`
- Modify: `app/src/main/java/com/burixer85/piscinamap/features/home/presentation/HomeScreen.kt`

- [ ] **Step 1: Add favorite state listener to HomeViewModel**

In `HomeViewmodel.kt`, add the favorite listener field and wire it in `init`/`onCleared`:

```kotlin
private val favoriteStateListener: (String, Boolean) -> Unit = { poolId, isFavorite ->
    updatePoolFavoriteState(poolId, isFavorite)
}

init {
    PoolStateManager.subscribe(hiddenStateListener)
    PoolStateManager.subscribeFavorite(favoriteStateListener)
}

override fun onCleared() {
    super.onCleared()
    PoolStateManager.unsubscribe(hiddenStateListener)
    PoolStateManager.unsubscribeFavorite(favoriteStateListener)
}
```

- [ ] **Step 2: Add updatePoolFavoriteState to HomeViewModel**

Add this method next to `updatePoolHiddenState`:

```kotlin
internal fun updatePoolFavoriteState(poolId: String, isFavorite: Boolean) {
    _uiState.update { currentState ->
        val updatedPools = currentState.pools.map { pool ->
            if (pool.id == poolId) pool.copy(isFavorite = isFavorite) else pool
        }
        currentState.copy(pools = updatedPools)
    }
}
```

- [ ] **Step 3: Add favorite marker icon in HomeScreen**

In `HomeScreen.kt`, inside the `LaunchedEffect(locationPermissionState.status.isGranted)` block, after loading the other icons, add:

```kotlin
var poolIconFavorite by remember { mutableStateOf<BitmapDescriptor?>(null) }
```

> Note: Declare this variable alongside the other icon state variables at the top of the composable (with `poolIconNormal`, `poolIconHighlighted`, `poolIconHidden`). Inside the LaunchedEffect, add:

```kotlin
// no new drawable needed — use a yellow default marker for favorites
poolIconFavorite = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
```

Actually, since `BitmapDescriptorFactory.defaultMarker` doesn't require a context, compute it inside the `GoogleMap { }` composable block just like `poolIconSelected`:

```kotlin
val poolIconFavorite = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
```

Remove the `var poolIconFavorite by remember` step above — just add this line inside the GoogleMap block alongside `poolIconSelected`.

- [ ] **Step 4: Add isFavorite case to the marker icon selector**

In `HomeScreen.kt`, find the `val icon = when {` block and add the `isFavorite` case:

```kotlin
val icon = when {
    isSelected -> poolIconSelected
    pool.isHidden -> poolIconHidden
    pool.isFavorite -> poolIconFavorite
    pool.isNew -> poolIconHighlighted
    else -> poolIconNormal
}
```

Also update the `remember` key for `isSelected` to also react to `pool.isFavorite`:
```kotlin
val isSelected by remember(selectedPool, pool.id, pool.isHidden, pool.isFavorite) {
    derivedStateOf { selectedPool?.id == pool.id }
}
```

- [ ] **Step 5: Build to confirm compilation**

```
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```
git add app/src/main/java/com/burixer85/piscinamap/features/home/presentation/HomeViewmodel.kt
git add app/src/main/java/com/burixer85/piscinamap/features/home/presentation/HomeScreen.kt
git commit -m "feat: sync favorite state to map markers via PoolStateManager"
```

---

## Task 9: FavoritesViewModel and FavoritesScreen

**Files:**
- Create: `app/src/main/java/com/burixer85/piscinamap/features/favorites/presentation/FavoritesViewModel.kt`
- Create: `app/src/main/java/com/burixer85/piscinamap/features/favorites/presentation/FavoritesScreen.kt`
- Create: `app/src/test/java/com/burixer85/piscinamap/features/favorites/presentation/FavoritesViewModelTest.kt`

- [ ] **Step 1: Create FavoritesViewModel.kt**

```kotlin
package com.burixer85.piscinamap.features.favorites.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.presentation.util.FavoritesManager
import com.burixer85.piscinamap.core.presentation.util.PoolStateManager
import com.burixer85.piscinamap.features.detail.domain.usecases.GetPoolDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getPoolDetailsUseCase: GetPoolDetailsUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val favoriteStateListener: (String, Boolean) -> Unit = { poolId, isFavorite ->
        updateFavoriteState(poolId, isFavorite)
    }

    init {
        PoolStateManager.subscribeFavorite(favoriteStateListener)
        loadFavorites()
    }

    override fun onCleared() {
        super.onCleared()
        PoolStateManager.unsubscribeFavorite(favoriteStateListener)
    }

    private fun loadFavorites() {
        val ids = FavoritesManager.getFavoriteIds(context)
        if (ids.isEmpty()) {
            _uiState.update { FavoritesUiState(pools = emptyList(), isEmpty = true) }
            return
        }
        viewModelScope.launch {
            val pools = ids.mapNotNull { id ->
                getPoolDetailsUseCase(id).getOrNull()?.copy(isFavorite = true)
            }
            _uiState.update { FavoritesUiState(pools = pools, isEmpty = pools.isEmpty()) }
        }
    }

    internal fun updateFavoriteState(poolId: String, isFavorite: Boolean) {
        if (isFavorite) {
            viewModelScope.launch {
                val pool = getPoolDetailsUseCase(poolId).getOrNull()?.copy(isFavorite = true)
                if (pool != null) {
                    _uiState.update { current ->
                        val exists = current.pools.any { it.id == poolId }
                        if (!exists) {
                            val newPools = current.pools + pool
                            current.copy(pools = newPools, isEmpty = false)
                        } else current
                    }
                }
            }
        } else {
            _uiState.update { current ->
                val newPools = current.pools.filter { it.id != poolId }
                current.copy(pools = newPools, isEmpty = newPools.isEmpty())
            }
        }
    }
}

data class FavoritesUiState(
    val pools: List<Pool> = emptyList(),
    val isEmpty: Boolean = true
)
```

- [ ] **Step 2: Create FavoritesScreen.kt**

```kotlin
package com.burixer85.piscinamap.features.favorites.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burixer85.piscinamap.R
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.presentation.components.PoolListCard

@Composable
fun FavoritesScreen(
    onNavigateToDetail: (String) -> Unit,
    bottomPadding: Int = 0,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FavoritesContent(
        uiState = uiState,
        onPoolClick = onNavigateToDetail,
        bottomPadding = bottomPadding
    )
}

@Composable
internal fun FavoritesContent(
    uiState: FavoritesUiState,
    onPoolClick: (String) -> Unit,
    bottomPadding: Int = 0
) {
    if (uiState.isEmpty) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = stringResource(R.string.no_favorites),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = (bottomPadding + 16).dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.pools, key = { it.id }) { pool ->
                PoolListCard(
                    pool = pool,
                    onNavigateToDetail = onPoolClick
                )
            }
        }
    }
}
```

- [ ] **Step 3: Create FavoritesViewModelTest.kt**

```kotlin
package com.burixer85.piscinamap.features.favorites.presentation

import android.content.Context
import android.content.SharedPreferences
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.detail.domain.usecases.GetPoolDetailsUseCase
import io.mockk.CapturingSlot
import io.mockk.answers
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var useCase: GetPoolDetailsUseCase
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val backingSet = mutableSetOf<String>()
    private val capturedSet: CapturingSlot<Set<String>> = slot()

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
        backingSet.clear()

        useCase = mockk()
        editor = mockk(relaxed = true)
        prefs = mockk()
        context = mockk()

        every { context.getSharedPreferences("favorite_pools", Context.MODE_PRIVATE) } returns prefs
        every { prefs.getStringSet("favorite_pool_ids", any()) } answers { backingSet.toSet() }
        every { prefs.edit() } returns editor
        every { editor.putStringSet("favorite_pool_ids", capture(capturedSet)) } answers {
            backingSet.clear()
            backingSet.addAll(capturedSet.captured)
            editor
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty when no favorites saved`() = runTest {
        val vm = FavoritesViewModel(useCase, context)

        assertTrue(vm.uiState.value.isEmpty)
        assertTrue(vm.uiState.value.pools.isEmpty())
    }

    @Test
    fun `loads pool details for each saved favorite`() = runTest {
        backingSet.add("pool1")
        coEvery { useCase("pool1") } returns Result.success(fakePool)

        val vm = FavoritesViewModel(useCase, context)

        assertFalse(vm.uiState.value.isEmpty)
        assertEquals(1, vm.uiState.value.pools.size)
        assertEquals("pool1", vm.uiState.value.pools[0].id)
        assertTrue(vm.uiState.value.pools[0].isFavorite)
    }

    @Test
    fun `failed use case call omits pool from list`() = runTest {
        backingSet.addAll(listOf("pool1", "pool2"))
        coEvery { useCase("pool1") } returns Result.success(fakePool)
        coEvery { useCase("pool2") } returns Result.failure(Exception("Network error"))

        val vm = FavoritesViewModel(useCase, context)

        assertEquals(1, vm.uiState.value.pools.size)
        assertEquals("pool1", vm.uiState.value.pools[0].id)
    }

    @Test
    fun `updateFavoriteState with isFavorite true adds pool to list`() = runTest {
        val pool2 = fakePool.copy(id = "pool2", name = "Piscina 2")
        coEvery { useCase("pool2") } returns Result.success(pool2)

        val vm = FavoritesViewModel(useCase, context)

        vm.updateFavoriteState("pool2", true)

        assertTrue(vm.uiState.value.pools.any { it.id == "pool2" })
        assertFalse(vm.uiState.value.isEmpty)
    }

    @Test
    fun `updateFavoriteState with isFavorite false removes pool from list`() = runTest {
        backingSet.add("pool1")
        coEvery { useCase("pool1") } returns Result.success(fakePool)

        val vm = FavoritesViewModel(useCase, context)

        vm.updateFavoriteState("pool1", false)

        assertTrue(vm.uiState.value.pools.none { it.id == "pool1" })
        assertTrue(vm.uiState.value.isEmpty)
    }

    @Test
    fun `updateFavoriteState does not duplicate pool already in list`() = runTest {
        backingSet.add("pool1")
        coEvery { useCase("pool1") } returns Result.success(fakePool)

        val vm = FavoritesViewModel(useCase, context)

        vm.updateFavoriteState("pool1", true)

        assertEquals(1, vm.uiState.value.pools.size)
    }
}
```

- [ ] **Step 4: Run unit tests**

```
./gradlew testDebugUnitTest --tests "*.FavoritesViewModelTest"
```

Expected: 6 tests PASS.

- [ ] **Step 5: Commit**

```
git add app/src/main/java/com/burixer85/piscinamap/features/favorites/
git add app/src/test/java/com/burixer85/piscinamap/features/favorites/
git commit -m "feat: add FavoritesViewModel and FavoritesScreen"
```

---

## Task 10: Navigation — Add Favorites Tab

**Files:**
- Modify: `app/src/main/java/com/burixer85/piscinamap/navigation/NavDestinations.kt`
- Modify: `app/src/main/java/com/burixer85/piscinamap/navigation/PiscinaMapNavGraph.kt`

- [ ] **Step 1: Add FavoritesRouteNav to NavDestinations.kt**

Append at the end of the file:

```kotlin
@Serializable
data object FavoritesRouteNav : PiscinaRoute()
```

- [ ] **Step 2: Add FavoritesScreen destination to the entryProvider in PiscinaMapNavGraph.kt**

Inside the `entryProvider { }` block, after the `entry<ExploreRouteNav>` block, add:

```kotlin
entry<FavoritesRouteNav> {
    FavoritesScreen(
        onNavigateToDetail = { poolId ->
            CameraStateHolder.isNavigatingToDetail = true
            navCounter++
            backStack.add(DetailRouteNav(poolId))
        },
        bottomPadding = 130
    )
}
```

Add the import at the top:
```kotlin
import com.burixer85.piscinamap.features.favorites.presentation.FavoritesScreen
import com.burixer85.piscinamap.navigation.FavoritesRouteNav
```

- [ ] **Step 3: Update showBottomBar condition to include FavoritesRouteNav**

Find:
```kotlin
val showBottomBar = currentRoute is HomeRouteNav || currentRoute is ExploreRouteNav
```

Replace with:
```kotlin
val showBottomBar = currentRoute is HomeRouteNav || currentRoute is ExploreRouteNav || currentRoute is FavoritesRouteNav
```

- [ ] **Step 4: Update the bottom nav bar to 3 tabs**

In `PiscinaMapNavGraph.kt`, replace the entire `Row { }` that contains the two `NavItem` composables and the divider with:

```kotlin
Row(
    modifier = Modifier
        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
        .fillMaxWidth()
        .height(64.dp)
        .clip(RoundedCornerShape(999.dp))
        .background(cs.surfaceVariant)
        .border(
            width = 1.dp,
            color = cs.outline,
            shape = RoundedCornerShape(999.dp)
        ),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically
) {
    val isMapActive = currentRoute is HomeRouteNav
    NavItem(
        icon = Icons.Default.Home,
        label = stringResource(R.string.nav_map),
        isActive = isMapActive,
        modifier = Modifier.weight(1f),
        onClick = {
            val existingHomeIndex = backStack.indexOfFirst { it is HomeRouteNav }
            if (existingHomeIndex != -1 && existingHomeIndex != backStack.lastIndex) {
                while (backStack.lastIndex > existingHomeIndex) {
                    backStack.removeLastOrNull()
                }
            }
        }
    )

    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(cs.outline)
    )

    val isListActive = currentRoute is ExploreRouteNav
    NavItem(
        icon = Icons.Default.Place,
        label = stringResource(R.string.nav_list),
        isActive = isListActive,
        modifier = Modifier.weight(1f),
        onClick = {
            CameraStateHolder.isNavigatingToDetail = true
            val existingExploreIndex = backStack.indexOfFirst { it is ExploreRouteNav }
            if (existingExploreIndex != -1 && existingExploreIndex != backStack.lastIndex) {
                while (backStack.lastIndex > existingExploreIndex) {
                    backStack.removeLastOrNull()
                }
            } else if (existingExploreIndex == -1) {
                backStack.add(ExploreRouteNav)
            }
        }
    )

    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(cs.outline)
    )

    val isFavoritesActive = currentRoute is FavoritesRouteNav
    NavItem(
        icon = Icons.Default.Favorite,
        label = stringResource(R.string.favorites),
        isActive = isFavoritesActive,
        modifier = Modifier.weight(1f),
        onClick = {
            val existingFavIndex = backStack.indexOfFirst { it is FavoritesRouteNav }
            if (existingFavIndex != -1 && existingFavIndex != backStack.lastIndex) {
                while (backStack.lastIndex > existingFavIndex) {
                    backStack.removeLastOrNull()
                }
            } else if (existingFavIndex == -1) {
                backStack.add(FavoritesRouteNav)
            }
        }
    )
}
```

Add import:
```kotlin
import androidx.compose.material.icons.filled.Favorite
```

- [ ] **Step 5: Build to confirm compilation**

```
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```
git add app/src/main/java/com/burixer85/piscinamap/navigation/NavDestinations.kt
git add app/src/main/java/com/burixer85/piscinamap/navigation/PiscinaMapNavGraph.kt
git commit -m "feat: add Favorites tab to navigation"
```

---

## Task 11: FavoritesContentTest (Instrumented)

**Files:**
- Create: `app/src/androidTest/java/com/burixer85/piscinamap/ui/screens/FavoritesContentTest.kt`

- [ ] **Step 1: Create FavoritesContentTest.kt**

```kotlin
package com.burixer85.piscinamap.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.burixer85.piscinamap.R
import com.burixer85.piscinamap.features.favorites.presentation.FavoritesContent
import com.burixer85.piscinamap.features.favorites.presentation.FavoritesUiState
import com.burixer85.piscinamap.ui.theme.PiscinaMapTheme
import com.burixer85.piscinamap.util.TestData
import org.junit.Rule
import org.junit.Test

class FavoritesContentTest {

    @get:Rule val rule = createComposeRule()
    private val ctx get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun showsEmptyStateMessage_whenNoFavorites() {
        rule.setContent {
            PiscinaMapTheme {
                FavoritesContent(
                    uiState = FavoritesUiState(isEmpty = true, pools = emptyList()),
                    onPoolClick = {}
                )
            }
        }
        rule.onNodeWithText(ctx.getString(R.string.no_favorites)).assertIsDisplayed()
    }

    @Test
    fun showsPoolName_whenFavoritesNotEmpty() {
        val pools = listOf(TestData.pool.copy(isFavorite = true))
        rule.setContent {
            PiscinaMapTheme {
                FavoritesContent(
                    uiState = FavoritesUiState(isEmpty = false, pools = pools),
                    onPoolClick = {}
                )
            }
        }
        rule.onNodeWithText("Piscina Municipal Centro").assertIsDisplayed()
    }

    @Test
    fun showsMultiplePoolNames_whenMultipleFavorites() {
        val pools = listOf(
            TestData.pool.copy(isFavorite = true),
            TestData.pool.copy(id = "id2", name = "Piscina Olímpica Norte", isFavorite = true)
        )
        rule.setContent {
            PiscinaMapTheme {
                FavoritesContent(
                    uiState = FavoritesUiState(isEmpty = false, pools = pools),
                    onPoolClick = {}
                )
            }
        }
        rule.onNodeWithText("Piscina Municipal Centro").assertIsDisplayed()
        rule.onNodeWithText("Piscina Olímpica Norte").assertIsDisplayed()
    }

    @Test
    fun invokesOnPoolClick_whenPoolCardTapped() {
        var clickedId = ""
        val pools = listOf(TestData.pool.copy(isFavorite = true))
        rule.setContent {
            PiscinaMapTheme {
                FavoritesContent(
                    uiState = FavoritesUiState(isEmpty = false, pools = pools),
                    onPoolClick = { id -> clickedId = id }
                )
            }
        }
        rule.onNodeWithText("Piscina Municipal Centro").performClick()
        assert(clickedId == TestData.pool.id)
    }

    @Test
    fun doesNotShowPoolNames_whenEmptyState() {
        rule.setContent {
            PiscinaMapTheme {
                FavoritesContent(
                    uiState = FavoritesUiState(isEmpty = true, pools = emptyList()),
                    onPoolClick = {}
                )
            }
        }
        rule.onNodeWithText("Piscina Municipal Centro").assertDoesNotExist()
    }
}
```

- [ ] **Step 2: Run instrumented tests**

```
./gradlew connectedAndroidTest --tests "*.FavoritesContentTest"
```

Expected: 5 tests PASS.

- [ ] **Step 3: Run all tests to confirm no regressions**

```
./gradlew testDebugUnitTest
./gradlew connectedAndroidTest
```

Expected: all tests PASS.

- [ ] **Step 4: Commit**

```
git add app/src/androidTest/java/com/burixer85/piscinamap/ui/screens/FavoritesContentTest.kt
git commit -m "test: add FavoritesContentTest instrumented UI tests"
```

---

## Self-Review

**Spec coverage check:**
- ✅ FavoritesManager (Task 1)
- ✅ PoolStateManager extension (Task 2)
- ✅ Pool.isFavorite + repositories (Task 3)
- ✅ FavoritesViewModel (Task 9)
- ✅ FavoritesScreen with empty state and PoolListCard (Task 9)
- ✅ Heart icon in DetailScreen via PoolDetailContent (Tasks 5–6)
- ✅ Heart icon in PoolDetailCard map card (Task 7)
- ✅ Special (yellow) favorite map marker (Task 8)
- ✅ HomeViewModel subscribes to favorite state changes (Task 8)
- ✅ FavoritesRouteNav + 3-tab bottom nav (Task 10)
- ✅ FavoritesManagerTest (Task 1)
- ✅ PoolStateManagerTest extended (Task 2)
- ✅ FavoritesViewModelTest (Task 9)
- ✅ DetailContentTest extended (Task 6)
- ✅ FavoritesContentTest (Task 11)
- ✅ String resources (Task 4)

**No placeholders found.**

**Type consistency:** `FavoritesUiState` defined in `FavoritesViewModel.kt` and used in `FavoritesScreen.kt`. `updateFavoriteState` is `internal` as used in tests. `FavoritesManager` method names used consistently across all tasks. `PoolStateManager.subscribeFavorite/unsubscribeFavorite/emitFavoriteStateChange` defined in Task 2 and used in Tasks 8, 9. `Pool.isFavorite` added in Task 3 and used in Tasks 7, 8, 9.

**Non-spec note:** The spec's non-goals section said "No API calls from FavoritesViewModel" but section 3 (approved during self-review) explicitly allows one API call per pool for richer data. Task 9 follows section 3.
