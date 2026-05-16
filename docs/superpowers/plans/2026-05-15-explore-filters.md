# Explore Filters Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Añadir filtros client-side (valoración, distancia, abierto ahora, tipo) a ExploreScreen mediante una fila de chips horizontal, sin tocar repositorio, use cases, DI ni navegación.

**Architecture:** Filtrado pure client-side en `ExploreViewModel` a través de un `StateFlow<List<Pool>>` derivado (`filteredPools`). El `PoolType` se mapea desde `types: List<String>` de Google Places en el mapper existente. La UI usa `FilterChipsRow` — un nuevo composable con `LazyRow` de `FilterChip` Material 3.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt, Kotlin Coroutines/Flow, JUnit4, MockK, Compose UI Testing.

---

## File Map

| Fichero | Acción |
|---|---|
| `app/src/main/java/com/burixer85/piscinamap/core/domain/model/PoolType.kt` | Crear |
| `app/src/main/java/com/burixer85/piscinamap/core/domain/model/FilterState.kt` | Crear |
| `app/src/main/java/com/burixer85/piscinamap/core/domain/model/Pool.kt` | Modificar — añade `poolType: PoolType` |
| `app/src/main/java/com/burixer85/piscinamap/core/data/dto/PlacesResponse.kt` | Modificar — añade `toPoolType()` + mapea campo |
| `app/src/main/java/com/burixer85/piscinamap/features/explore/presentation/ExploreViewModel.kt` | Modificar — añade `filteredPools`, `updateFilter`, `setUserLocation`, `applyFilters` |
| `app/src/main/java/com/burixer85/piscinamap/features/explore/presentation/FilterChipsRow.kt` | Crear |
| `app/src/main/java/com/burixer85/piscinamap/features/explore/presentation/ExploreScreen.kt` | Modificar — integra `FilterChipsRow`, consume `filteredPools` |
| `app/src/test/java/com/burixer85/piscinamap/core/data/dto/PoolTypeMappingTest.kt` | Crear |
| `app/src/test/java/com/burixer85/piscinamap/features/explore/presentation/ExploreViewModelFilterTest.kt` | Crear |
| `app/src/androidTest/java/com/burixer85/piscinamap/features/explore/presentation/FilterChipsRowTest.kt` | Crear |

---

## Task 1: Domain model — PoolType y FilterState

**Files:**
- Create: `app/src/main/java/com/burixer85/piscinamap/core/domain/model/PoolType.kt`
- Create: `app/src/main/java/com/burixer85/piscinamap/core/domain/model/FilterState.kt`

- [ ] **Step 1: Crear PoolType.kt**

```kotlin
package com.burixer85.piscinamap.core.domain.model

enum class PoolType { PUBLIC, MUNICIPAL, HOTEL, UNKNOWN }
```

- [ ] **Step 2: Crear FilterState.kt**

```kotlin
package com.burixer85.piscinamap.core.domain.model

data class FilterState(
    val minRating: Float? = null,
    val openNow: Boolean = false,
    val maxDistanceKm: Int? = null,
    val selectedTypes: Set<PoolType> = emptySet()
)
```

- [ ] **Step 3: Verificar que compila**

```
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/burixer85/piscinamap/core/domain/model/PoolType.kt
git add app/src/main/java/com/burixer85/piscinamap/core/domain/model/FilterState.kt
git commit -m "feat: add PoolType enum and FilterState domain model"
```

---

## Task 2: Añadir poolType a Pool

**Files:**
- Modify: `app/src/main/java/com/burixer85/piscinamap/core/domain/model/Pool.kt`

El fichero actual termina en la línea 19 con `val reviews`. Añadir `poolType` como último campo con valor por defecto para no romper constructores existentes.

- [ ] **Step 1: Modificar Pool.kt**

Reemplazar el contenido del data class Pool (líneas 3-23):

```kotlin
package com.burixer85.piscinamap.core.domain.model

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
    val reviews: List<Review> = emptyList(),
    val poolType: PoolType = PoolType.UNKNOWN
) {
    val photoUrl: String?
        get() = photoUrls.firstOrNull()
}
```

- [ ] **Step 2: Compilar**

```
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL (el valor por defecto evita romper código existente)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/burixer85/piscinamap/core/domain/model/Pool.kt
git commit -m "feat: add poolType field to Pool domain model"
```

---

## Task 3: Mapper toPoolType() en PlacesResponse

**Files:**
- Modify: `app/src/main/java/com/burixer85/piscinamap/core/data/dto/PlacesResponse.kt`
- Create: `app/src/test/java/com/burixer85/piscinamap/core/data/dto/PoolTypeMappingTest.kt`

- [ ] **Step 1: Escribir el test que falla**

```kotlin
package com.burixer85.piscinamap.core.data.dto

import com.burixer85.piscinamap.core.domain.model.PoolType
import org.junit.Assert.assertEquals
import org.junit.Test

class PoolTypeMappingTest {

    private fun makePlaceDto(types: List<String>) = PlaceDto(
        placeId = "id",
        name = "Test Pool",
        geometry = GeometryDto(LocationDto(0.0, 0.0)),
        types = types
    )

    private fun makePlaceDetailsDto(types: List<String>) = PlaceDetailsDto(
        name = "Test Pool",
        geometry = GeometryDto(LocationDto(0.0, 0.0)),
        types = types
    )

    @Test
    fun `PlaceDto with lodging type maps to HOTEL`() {
        val pool = makePlaceDto(listOf("lodging", "point_of_interest")).toDomain()
        assertEquals(PoolType.HOTEL, pool.poolType)
    }

    @Test
    fun `PlaceDto with hotel type maps to HOTEL`() {
        val pool = makePlaceDto(listOf("hotel")).toDomain()
        assertEquals(PoolType.HOTEL, pool.poolType)
    }

    @Test
    fun `PlaceDto with local_government_office maps to MUNICIPAL`() {
        val pool = makePlaceDto(listOf("local_government_office", "swimming_pool")).toDomain()
        assertEquals(PoolType.MUNICIPAL, pool.poolType)
    }

    @Test
    fun `PlaceDto with swimming_pool only maps to PUBLIC`() {
        val pool = makePlaceDto(listOf("swimming_pool", "establishment")).toDomain()
        assertEquals(PoolType.PUBLIC, pool.poolType)
    }

    @Test
    fun `PlaceDto with empty types maps to UNKNOWN`() {
        val pool = makePlaceDto(emptyList()).toDomain()
        assertEquals(PoolType.UNKNOWN, pool.poolType)
    }

    @Test
    fun `PlaceDetailsDto with motel type maps to HOTEL`() {
        val pool = makePlaceDetailsDto(listOf("motel")).toDomain()
        assertEquals(PoolType.HOTEL, pool.poolType)
    }

    @Test
    fun `PlaceDetailsDto with park type maps to PUBLIC`() {
        val pool = makePlaceDetailsDto(listOf("park", "establishment")).toDomain()
        assertEquals(PoolType.PUBLIC, pool.poolType)
    }

    @Test
    fun `hotel takes priority over swimming_pool in mixed types`() {
        val pool = makePlaceDto(listOf("swimming_pool", "lodging")).toDomain()
        assertEquals(PoolType.HOTEL, pool.poolType)
    }
}
```

- [ ] **Step 2: Ejecutar para confirmar que falla**

```
./gradlew :app:testDebugUnitTest --tests "*.PoolTypeMappingTest"
```

Expected: compilation error — `pool.poolType` no existe aún en el mapper

- [ ] **Step 3: Añadir toPoolType() y mapear en PlacesResponse.kt**

Añadir al final del fichero (después de la línea 148, después de `fun PlaceDetailsDto.toDomain()`), la función privada:

```kotlin
private fun List<String>.toPoolType(): PoolType = when {
    any { it in listOf("lodging", "hotel", "resort_hotel", "motel") } -> PoolType.HOTEL
    any { it in listOf("local_government_office", "city_hall", "municipal_government") } -> PoolType.MUNICIPAL
    any { it in listOf("park", "sports_complex", "recreation_center", "swimming_pool") } -> PoolType.PUBLIC
    else -> PoolType.UNKNOWN
}
```

Añadir el import al principio del fichero:
```kotlin
import com.burixer85.piscinamap.core.domain.model.PoolType
```

Actualizar `PlaceDto.toDomain()` (líneas 50-62) para incluir poolType:

```kotlin
fun PlaceDto.toDomain(): Pool {
    return Pool(
        id = this.placeId,
        name = this.name,
        latitude = this.geometry.location.lat,
        longitude = this.geometry.location.lng,
        address = this.address ?: "",
        rating = this.rating,
        isOpenNow = this.openingHours?.openNow,
        photoUrls = this.photos?.map { it.photoReference } ?: emptyList(),
        isNew = false,
        poolType = types.toPoolType()
    )
}
```

Actualizar `PlaceDetailsDto.toDomain()` (línea 123, dentro del `return Pool(`), añadir antes del paréntesis de cierre:

```kotlin
        poolType = types.toPoolType()
```

- [ ] **Step 4: Ejecutar tests**

```
./gradlew :app:testDebugUnitTest --tests "*.PoolTypeMappingTest"
```

Expected: 8 tests PASSED

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/burixer85/piscinamap/core/data/dto/PlacesResponse.kt
git add app/src/test/java/com/burixer85/piscinamap/core/data/dto/PoolTypeMappingTest.kt
git commit -m "feat: map Google Places types to PoolType in PlacesResponse mapper"
```

---

## Task 4: ExploreViewModel — filteredPools y lógica de filtrado

**Files:**
- Modify: `app/src/main/java/com/burixer85/piscinamap/features/explore/presentation/ExploreViewModel.kt`
- Create: `app/src/test/java/com/burixer85/piscinamap/features/explore/presentation/ExploreViewModelFilterTest.kt`

- [ ] **Step 1: Escribir los tests que fallan**

```kotlin
package com.burixer85.piscinamap.features.explore.presentation

import com.burixer85.piscinamap.core.domain.model.FilterState
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.domain.model.PoolType
import com.burixer85.piscinamap.features.explore.domain.usecases.GetExploreNearbyPoolsUseCase
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelFilterTest {

    private val mockUseCase = mockk<GetExploreNearbyPoolsUseCase>(relaxed = true)
    private lateinit var viewModel: ExploreViewModel

    private fun makePool(
        id: String,
        rating: Float? = null,
        isOpenNow: Boolean? = null,
        lat: Double = 40.4168,
        lng: Double = -3.7038,
        poolType: PoolType = PoolType.UNKNOWN
    ) = Pool(
        id = id,
        name = "Pool $id",
        latitude = lat,
        longitude = lng,
        address = "",
        rating = rating,
        isOpenNow = isOpenNow,
        poolType = poolType
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = ExploreViewModel(mockUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `applyFilters returns all pools when FilterState is default`() {
        val pools = listOf(makePool("1"), makePool("2"), makePool("3"))
        val result = viewModel.applyFilters(pools, FilterState(), null)
        assertEquals(pools, result)
    }

    @Test
    fun `applyFilters filters by minRating`() {
        val pools = listOf(
            makePool("1", rating = 4.5f),
            makePool("2", rating = 3.5f),
            makePool("3", rating = null)
        )
        val result = viewModel.applyFilters(pools, FilterState(minRating = 4f), null)
        assertEquals(listOf(pools[0]), result)
    }

    @Test
    fun `applyFilters openNow excludes pools with isOpenNow null or false`() {
        val pools = listOf(
            makePool("1", isOpenNow = true),
            makePool("2", isOpenNow = false),
            makePool("3", isOpenNow = null)
        )
        val result = viewModel.applyFilters(pools, FilterState(openNow = true), null)
        assertEquals(listOf(pools[0]), result)
    }

    @Test
    fun `applyFilters openNow false does not filter`() {
        val pools = listOf(makePool("1", isOpenNow = true), makePool("2", isOpenNow = false))
        val result = viewModel.applyFilters(pools, FilterState(openNow = false), null)
        assertEquals(pools, result)
    }

    @Test
    fun `applyFilters filters by maxDistanceKm`() {
        // Madrid center ~40.4168, -3.7038
        // Pool at ~5km away: ~40.46, -3.70 (roughly 4.8km)
        // Pool at ~30km away: ~40.68, -3.70
        val nearPool = makePool("near", lat = 40.46, lng = -3.70)
        val farPool = makePool("far", lat = 40.68, lng = -3.70)
        val userLatLng = 40.4168 to -3.7038

        val result = viewModel.applyFilters(
            listOf(nearPool, farPool),
            FilterState(maxDistanceKm = 10),
            userLatLng
        )
        assertEquals(listOf(nearPool), result)
    }

    @Test
    fun `applyFilters distance returns all when userLatLng is null`() {
        val pools = listOf(makePool("1", lat = 60.0, lng = 20.0), makePool("2", lat = 0.0, lng = 0.0))
        val result = viewModel.applyFilters(pools, FilterState(maxDistanceKm = 1), null)
        assertEquals(pools, result)
    }

    @Test
    fun `applyFilters filters by single type`() {
        val pools = listOf(
            makePool("1", poolType = PoolType.HOTEL),
            makePool("2", poolType = PoolType.PUBLIC),
            makePool("3", poolType = PoolType.MUNICIPAL)
        )
        val result = viewModel.applyFilters(
            pools,
            FilterState(selectedTypes = setOf(PoolType.HOTEL)),
            null
        )
        assertEquals(listOf(pools[0]), result)
    }

    @Test
    fun `applyFilters filters by multiple types`() {
        val pools = listOf(
            makePool("1", poolType = PoolType.HOTEL),
            makePool("2", poolType = PoolType.PUBLIC),
            makePool("3", poolType = PoolType.MUNICIPAL)
        )
        val result = viewModel.applyFilters(
            pools,
            FilterState(selectedTypes = setOf(PoolType.HOTEL, PoolType.PUBLIC)),
            null
        )
        assertEquals(listOf(pools[0], pools[1]), result)
    }

    @Test
    fun `applyFilters empty selectedTypes does not filter by type`() {
        val pools = listOf(makePool("1", poolType = PoolType.HOTEL), makePool("2", poolType = PoolType.UNKNOWN))
        val result = viewModel.applyFilters(pools, FilterState(selectedTypes = emptySet()), null)
        assertEquals(pools, result)
    }

    @Test
    fun `applyFilters combines multiple active filters`() {
        val pools = listOf(
            makePool("1", rating = 4.5f, isOpenNow = true, poolType = PoolType.HOTEL),
            makePool("2", rating = 4.5f, isOpenNow = false, poolType = PoolType.HOTEL),
            makePool("3", rating = 3.0f, isOpenNow = true, poolType = PoolType.HOTEL)
        )
        val result = viewModel.applyFilters(
            pools,
            FilterState(minRating = 4f, openNow = true),
            null
        )
        assertEquals(listOf(pools[0]), result)
    }

    @Test
    fun `haversineKm returns approximately correct distance`() {
        // Madrid to Barcelona: ~504km
        val distance = viewModel.haversineKm(40.4168, -3.7038, 41.3851, 2.1734)
        assertTrue("Expected ~504km, got $distance", distance in 500.0..510.0)
    }
}
```

- [ ] **Step 2: Ejecutar para confirmar que falla**

```
./gradlew :app:testDebugUnitTest --tests "*.ExploreViewModelFilterTest"
```

Expected: compilation error — `applyFilters` y `haversineKm` no existen

- [ ] **Step 3: Reemplazar ExploreViewModel.kt completo**

```kotlin
package com.burixer85.piscinamap.features.explore.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burixer85.piscinamap.core.domain.model.FilterState
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.explore.domain.usecases.GetExploreNearbyPoolsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val getExploreNearbyPoolsUseCase: GetExploreNearbyPoolsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState(isLoading = true))
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    val filteredPools: StateFlow<List<Pool>> = _uiState
        .map { state -> applyFilters(state.pools, state.filters, state.userLatLng) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var currentLat: Double = 0.0
    private var currentLng: Double = 0.0

    fun setUserLocation(lat: Double, lng: Double) {
        _uiState.update { it.copy(userLatLng = lat to lng) }
    }

    fun updateFilter(filters: FilterState) {
        _uiState.update { it.copy(filters = filters) }
    }

    fun fetchPools(lat: Double, lng: Double) {
        currentLat = lat
        currentLng = lng

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
                    _uiState.update { it.copy(isLoadingMore = false, error = error.message) }
                }
            )
        }
    }

    internal fun applyFilters(
        pools: List<Pool>,
        filters: FilterState,
        userLatLng: Pair<Double, Double>?
    ): List<Pool> = pools.filter { pool ->
        val ratingOk = filters.minRating?.let { (pool.rating ?: 0f) >= it } ?: true
        val openOk = !filters.openNow || pool.isOpenNow == true
        val distanceOk = filters.maxDistanceKm?.let { maxKm ->
            userLatLng?.let { (uLat, uLng) ->
                haversineKm(uLat, uLng, pool.latitude, pool.longitude) <= maxKm
            } ?: true
        } ?: true
        val typeOk = filters.selectedTypes.isEmpty() || pool.poolType in filters.selectedTypes
        ratingOk && openOk && distanceOk && typeOk
    }

    internal fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return r * 2 * asin(sqrt(a))
    }
}

data class ExploreUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val pools: List<Pool> = emptyList(),
    val error: String? = null,
    val warning: String? = null,
    val hasSearchedMore: Boolean = false,
    val filters: FilterState = FilterState(),
    val userLatLng: Pair<Double, Double>? = null
)
```

- [ ] **Step 4: Ejecutar tests**

```
./gradlew :app:testDebugUnitTest --tests "*.ExploreViewModelFilterTest"
```

Expected: 11 tests PASSED

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/burixer85/piscinamap/features/explore/presentation/ExploreViewModel.kt
git add app/src/test/java/com/burixer85/piscinamap/features/explore/presentation/ExploreViewModelFilterTest.kt
git commit -m "feat: add filteredPools StateFlow and filter logic to ExploreViewModel"
```

---

## Task 5: FilterChipsRow composable

**Files:**
- Create: `app/src/main/java/com/burixer85/piscinamap/features/explore/presentation/FilterChipsRow.kt`
- Create: `app/src/androidTest/java/com/burixer85/piscinamap/features/explore/presentation/FilterChipsRowTest.kt`

- [ ] **Step 1: Escribir el test (androidTest — requiere emulador)**

```kotlin
package com.burixer85.piscinamap.features.explore.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.burixer85.piscinamap.core.domain.model.FilterState
import com.burixer85.piscinamap.core.domain.model.PoolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FilterChipsRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun chipOpenNow_togglesOnOff() {
        var emitted = FilterState()
        composeRule.setContent {
            FilterChipsRow(filters = FilterState(), onFiltersChange = { emitted = it })
        }
        composeRule.onNodeWithText("Abierto ahora").performClick()
        assertTrue(emitted.openNow)
    }

    @Test
    fun chipRating_cyclesThrough3f_4f_45f_null() {
        val states = mutableListOf<FilterState>()
        composeRule.setContent {
            FilterChipsRow(filters = FilterState(), onFiltersChange = { states.add(it) })
        }
        composeRule.onNodeWithText("Valoración").performClick()
        assertEquals(3f, states.last().minRating)
    }

    @Test
    fun chipRating_activeChip_showsLabel() {
        composeRule.setContent {
            FilterChipsRow(filters = FilterState(minRating = 4f), onFiltersChange = {})
        }
        composeRule.onNodeWithText("4+").assertIsDisplayed()
    }

    @Test
    fun chipDistance_cyclesThrough5_10_25_null() {
        val states = mutableListOf<FilterState>()
        composeRule.setContent {
            FilterChipsRow(filters = FilterState(), onFiltersChange = { states.add(it) })
        }
        composeRule.onNodeWithText("Distancia").performClick()
        assertEquals(5, states.last().maxDistanceKm)
    }

    @Test
    fun chipDistance_activeChip_showsKmLabel() {
        composeRule.setContent {
            FilterChipsRow(filters = FilterState(maxDistanceKm = 10), onFiltersChange = {})
        }
        composeRule.onNodeWithText("< 10km").assertIsDisplayed()
    }

    @Test
    fun chipTipo_opensDropdown() {
        composeRule.setContent {
            FilterChipsRow(filters = FilterState(), onFiltersChange = {})
        }
        composeRule.onNodeWithText("Tipo").performClick()
        composeRule.onNodeWithText("Pública").assertIsDisplayed()
        composeRule.onNodeWithText("Municipal").assertIsDisplayed()
        composeRule.onNodeWithText("Hotel").assertIsDisplayed()
    }

    @Test
    fun chipTipo_selectingHotel_emitsCorrectState() {
        var emitted = FilterState()
        composeRule.setContent {
            FilterChipsRow(filters = FilterState(), onFiltersChange = { emitted = it })
        }
        composeRule.onNodeWithText("Tipo").performClick()
        composeRule.onNodeWithText("Hotel").performClick()
        assertTrue(PoolType.HOTEL in emitted.selectedTypes)
    }

    @Test
    fun chipOpenNow_whenActive_showsCloseIcon_andClearsOnClick() {
        var emitted = FilterState(openNow = true)
        composeRule.setContent {
            FilterChipsRow(filters = FilterState(openNow = true), onFiltersChange = { emitted = it })
        }
        // Chip label changes to "Abierto" when active
        composeRule.onNodeWithText("Abierto").assertIsDisplayed()
        composeRule.onNodeWithText("Abierto").performClick()
        assertTrue(!emitted.openNow)
    }
}
```

- [ ] **Step 2: Crear FilterChipsRow.kt**

```kotlin
package com.burixer85.piscinamap.features.explore.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.burixer85.piscinamap.core.domain.model.FilterState
import com.burixer85.piscinamap.core.domain.model.PoolType

@Composable
fun FilterChipsRow(
    filters: FilterState,
    onFiltersChange: (FilterState) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTypeDropdown by remember { mutableStateOf(false) }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Abierto ahora
        item {
            val isActive = filters.openNow
            FilterChip(
                selected = isActive,
                onClick = { onFiltersChange(filters.copy(openNow = !isActive)) },
                label = { Text(if (isActive) "Abierto" else "Abierto ahora") },
                trailingIcon = if (isActive) {
                    { Icon(Icons.Default.Close, contentDescription = null) }
                } else null
            )
        }

        // Valoración
        item {
            val isActive = filters.minRating != null
            FilterChip(
                selected = isActive,
                onClick = {
                    onFiltersChange(filters.copy(minRating = filters.minRating.nextRating()))
                },
                label = { Text(filters.minRating.toRatingLabel()) },
                trailingIcon = if (isActive) {
                    { Icon(Icons.Default.Close, contentDescription = null) }
                } else null
            )
        }

        // Distancia
        item {
            val isActive = filters.maxDistanceKm != null
            FilterChip(
                selected = isActive,
                onClick = {
                    onFiltersChange(filters.copy(maxDistanceKm = filters.maxDistanceKm.nextDistance()))
                },
                label = { Text(filters.maxDistanceKm.toDistanceLabel()) },
                trailingIcon = if (isActive) {
                    { Icon(Icons.Default.Close, contentDescription = null) }
                } else null
            )
        }

        // Tipo
        item {
            val isActive = filters.selectedTypes.isNotEmpty()
            Box {
                FilterChip(
                    selected = isActive,
                    onClick = {
                        if (isActive) {
                            onFiltersChange(filters.copy(selectedTypes = emptySet()))
                        } else {
                            showTypeDropdown = true
                        }
                    },
                    label = {
                        Text(
                            if (isActive) filters.selectedTypes.joinToString { it.toLabel() }
                            else "Tipo"
                        )
                    },
                    trailingIcon = if (isActive) {
                        { Icon(Icons.Default.Close, contentDescription = null) }
                    } else null
                )
                DropdownMenu(
                    expanded = showTypeDropdown,
                    onDismissRequest = { showTypeDropdown = false }
                ) {
                    listOf(PoolType.PUBLIC, PoolType.MUNICIPAL, PoolType.HOTEL).forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = type in filters.selectedTypes,
                                        onCheckedChange = null
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(type.toLabel())
                                }
                            },
                            onClick = {
                                val newTypes = if (type in filters.selectedTypes) {
                                    filters.selectedTypes - type
                                } else {
                                    filters.selectedTypes + type
                                }
                                onFiltersChange(filters.copy(selectedTypes = newTypes))
                                if (newTypes.isEmpty()) showTypeDropdown = false
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun Float?.nextRating(): Float? = when (this) {
    null -> 3f
    3f -> 4f
    4f -> 4.5f
    else -> null
}

private fun Float?.toRatingLabel(): String = when (this) {
    3f -> "3+"
    4f -> "4+"
    4.5f -> "4.5+"
    else -> "Valoración"
}

private fun Int?.nextDistance(): Int? = when (this) {
    null -> 5
    5 -> 10
    10 -> 25
    else -> null
}

private fun Int?.toDistanceLabel(): String = when (this) {
    5 -> "< 5km"
    10 -> "< 10km"
    25 -> "< 25km"
    else -> "Distancia"
}

private fun PoolType.toLabel(): String = when (this) {
    PoolType.PUBLIC -> "Pública"
    PoolType.MUNICIPAL -> "Municipal"
    PoolType.HOTEL -> "Hotel"
    PoolType.UNKNOWN -> ""
}
```

- [ ] **Step 3: Compilar**

```
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/burixer85/piscinamap/features/explore/presentation/FilterChipsRow.kt
git add app/src/androidTest/java/com/burixer85/piscinamap/features/explore/presentation/FilterChipsRowTest.kt
git commit -m "feat: add FilterChipsRow composable with rating, distance, openNow and type chips"
```

---

## Task 6: Integrar FilterChipsRow en ExploreScreen

**Files:**
- Modify: `app/src/main/java/com/burixer85/piscinamap/features/explore/presentation/ExploreScreen.kt`

- [ ] **Step 1: Reemplazar ExploreScreen.kt completo**

```kotlin
package com.burixer85.piscinamap.features.explore.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burixer85.piscinamap.BuildConfig
import com.burixer85.piscinamap.R
import com.burixer85.piscinamap.core.domain.model.FilterState
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.presentation.components.ExitConfirmationDialog
import com.burixer85.piscinamap.core.presentation.components.NativeAdCard
import com.burixer85.piscinamap.core.presentation.components.PoolListCard
import com.burixer85.piscinamap.core.presentation.util.HiddenPoolsManager
import com.burixer85.piscinamap.core.presentation.util.LocaleHelper.getString
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ExploreScreen(
    onNavigateToDetail: (String) -> Unit = {},
    bottomPadding: Int = 0,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredPools by viewModel.filteredPools.collectAsStateWithLifecycle()

    var nativeAdList by remember { mutableStateOf<List<NativeAd>>(emptyList()) }
    var locationPermissionDecided by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        showExitConfirmation = true
    }

    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    LaunchedEffect(locationPermissionState.status) {
        delay(500)
        locationPermissionDecided = true
    }

    val nativeId = if (BuildConfig.USE_TEST_ADS) {
        "ca-app-pub-3940256099942544/2247696110"
    } else {
        BuildConfig.ADMOB_NATIVE_ID
    }

    LaunchedEffect(uiState.pools, locationPermissionDecided) {
        if (uiState.pools.isEmpty()) return@LaunchedEffect
        if (nativeAdList.isNotEmpty()) return@LaunchedEffect
        if (!locationPermissionDecided) return@LaunchedEffect

        try {
            val adLoader = AdLoader.Builder(context, nativeId)
                .forNativeAd { ad ->
                    nativeAdList = nativeAdList + ad
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {}
                })
                .build()
            adLoader.loadAds(AdRequest.Builder().build(), 5)
        } catch (e: Exception) {}
    }

    var initialLocationObtained by remember { mutableStateOf(false) }

    @SuppressLint("MissingPermission")
    LaunchedEffect(locationPermissionState.status.isGranted, uiState.pools.isEmpty()) {
        if (locationPermissionState.status.isGranted && !initialLocationObtained && uiState.pools.isEmpty()) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    viewModel.setUserLocation(it.latitude, it.longitude)
                    viewModel.fetchPools(it.latitude, it.longitude)
                }
            }
        }
    }

    ExploreContent(
        uiState = uiState,
        filteredPools = filteredPools,
        onFiltersChange = viewModel::updateFilter,
        onFetchMore = viewModel::fetchMorePools,
        onNavigateToDetail = onNavigateToDetail,
        nativeAdList = nativeAdList,
        showExitConfirmation = showExitConfirmation,
        onConfirmExit = {
            showExitConfirmation = false
            (context as? Activity)?.finish()
        },
        onDismissExit = { showExitConfirmation = false },
        bottomPadding = bottomPadding,
    )
}

@Composable
internal fun ExploreContent(
    uiState: ExploreUiState,
    filteredPools: List<Pool>,
    onFiltersChange: (FilterState) -> Unit,
    onFetchMore: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    nativeAdList: List<NativeAd> = emptyList(),
    showExitConfirmation: Boolean = false,
    onConfirmExit: () -> Unit = {},
    onDismissExit: () -> Unit = {},
    bottomPadding: Int = 0,
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            uiState.error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            uiState.pools.isEmpty() -> {
                Text(
                    text = stringResource(R.string.no_pools_in_this_area),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                val visiblePools = filteredPools.filter { pool ->
                    !HiddenPoolsManager.isHidden(context, pool.id)
                }
                val hasActiveFilters = uiState.filters != FilterState()

                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 18.dp, end = 18.dp, top = 32.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.poolmark),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = getString(context, R.string.app_name),
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.01f).sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Text(
                        text = "${visiblePools.size} piscinas encontradas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    FilterChipsRow(
                        filters = uiState.filters,
                        onFiltersChange = onFiltersChange,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (visiblePools.isEmpty() && hasActiveFilters) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Sin resultados para estos filtros",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { onFiltersChange(FilterState()) }) {
                                Text("Limpiar filtros")
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 16.dp,
                                bottom = (16 + bottomPadding).dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            var totalPoolsAdShown = 0

                            val itemList = buildList {
                                var totalPools = 0
                                visiblePools.forEachIndexed { _, pool ->
                                    add(pool)
                                    totalPools++

                                    val adPosition = 2 + (totalPoolsAdShown * 4)
                                    if (totalPools == adPosition && totalPools >= 2) {
                                        add(totalPoolsAdShown)
                                        totalPoolsAdShown++
                                    }
                                }
                            }

                            items(itemList.size) { index ->
                                val item = itemList[index]
                                when (item) {
                                    is Pool -> {
                                        PoolListCard(
                                            pool = item,
                                            onNavigateToDetail = onNavigateToDetail
                                        )
                                    }
                                    is Int -> {
                                        val adIndex = if (nativeAdList.isNotEmpty()) item % nativeAdList.size else 0
                                        val adForPosition = nativeAdList.getOrNull(adIndex) ?: nativeAdList.firstOrNull()
                                        NativeAdCard(
                                            nativeAd = adForPosition,
                                            ctaText = stringResource(R.string.see_more)
                                        )
                                    }
                                }
                            }

                            item {
                                if (!uiState.hasSearchedMore) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp, bottom = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (uiState.isLoadingMore) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        } else {
                                            Button(onClick = onFetchMore) {
                                                Text(stringResource(R.string.search_more))
                                            }
                                        }
                                    }
                                } else if (uiState.warning != null) {
                                    Text(
                                        text = uiState.warning!!,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExitConfirmation) {
        ExitConfirmationDialog(
            onConfirm = onConfirmExit,
            onDismiss = onDismissExit,
        )
    }
}
```

- [ ] **Step 2: Compilar**

```
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Ejecutar todos los unit tests**

```
./gradlew :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL, todos los tests PASSED

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/burixer85/piscinamap/features/explore/presentation/ExploreScreen.kt
git commit -m "feat: integrate FilterChipsRow into ExploreScreen with filteredPools"
```

---

## Verificación final

- [ ] **Compilación limpia completa**

```
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Todos los unit tests**

```
./gradlew :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL

- [ ] **Commit final si hay cambios pendientes**

```bash
git status
# Si hay cambios sin commitear:
git add -A
git commit -m "chore: finalize explore filters implementation"
```
