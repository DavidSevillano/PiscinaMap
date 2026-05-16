# Filtros en ExploreScreen

**Fecha:** 2026-05-15  
**Estado:** Aprobado

## Objetivo

Añadir filtros de usuario (valoración, distancia, abierto ahora, tipo) a ExploreScreen sin modificar la arquitectura existente (repositorio, use cases, DI, navegación).

## Decisiones clave

- **Estrategia de filtrado:** client-side en el ViewModel — los pools ya están cargados en memoria, no requiere llamadas de red adicionales.
- **Tipo de piscina:** mapeado desde el campo `types: List<String>` de la Google Places API en el mapper existente.
- **UI:** fila de chips scrollable horizontal (`LazyRow`) encima de la lista de piscinas.

---

## Modelo de dominio

### `PoolType` (nuevo fichero)

```
app/src/main/java/com/burixer85/piscinamap/core/domain/model/PoolType.kt
```

```kotlin
enum class PoolType { PUBLIC, MUNICIPAL, HOTEL, UNKNOWN }
```

Reglas de clasificación desde `types: List<String>`:

| Tipo resultante | Google Places types que lo activan |
|---|---|
| `HOTEL` | `lodging`, `hotel`, `resort_hotel`, `motel` |
| `MUNICIPAL` | `local_government_office`, `city_hall`, `municipal_government` |
| `PUBLIC` | `park`, `sports_complex`, `recreation_center`, `swimming_pool` |
| `UNKNOWN` | cualquier otra combinación |

### `FilterState` (nuevo fichero)

```
app/src/main/java/com/burixer85/piscinamap/core/domain/model/FilterState.kt
```

```kotlin
data class FilterState(
    val minRating: Float? = null,          // null = sin filtro; presets: 3f, 4f, 4.5f
    val openNow: Boolean = false,
    val maxDistanceKm: Int? = null,        // null = sin filtro; presets: 5, 10, 25
    val selectedTypes: Set<PoolType> = emptySet()  // vacío = sin filtro
)
```

### `Pool` (modificado)

Añadir campo:

```kotlin
val poolType: PoolType = PoolType.UNKNOWN
```

---

## Capa de datos

**Fichero modificado:** `PlacesResponse.kt`

Añadir función privada de clasificación:

```kotlin
private fun List<String>.toPoolType(): PoolType = when {
    any { it in listOf("lodging", "hotel", "resort_hotel", "motel") } -> PoolType.HOTEL
    any { it in listOf("local_government_office", "city_hall", "municipal_government") } -> PoolType.MUNICIPAL
    any { it in listOf("park", "sports_complex", "recreation_center", "swimming_pool") } -> PoolType.PUBLIC
    else -> PoolType.UNKNOWN
}
```

Aplicar en `PlaceDto.toDomain()` y `PlaceDetailsDto.toDomain()`:

```kotlin
poolType = types?.toPoolType() ?: PoolType.UNKNOWN
```

---

## ViewModel

**Fichero modificado:** `ExploreViewModel.kt`

### Cambios en `ExploreUiState`

```kotlin
data class ExploreUiState(
    // campos existentes sin cambio...
    val filters: FilterState = FilterState(),
    val userLatLng: Pair<Double, Double>? = null
)
```

### Nuevas funciones

```kotlin
fun setUserLocation(lat: Double, lng: Double) {
    _uiState.update { it.copy(userLatLng = lat to lng) }
}

fun updateFilter(filters: FilterState) {
    _uiState.update { it.copy(filters = filters) }
}
```

### `filteredPools` (StateFlow derivado)

```kotlin
val filteredPools: StateFlow<List<Pool>> = _uiState
    .map { state -> applyFilters(state.pools, state.filters, state.userLatLng) }
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
```

### `applyFilters` (lógica de filtrado)

```kotlin
private fun applyFilters(
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

private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val R = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    return R * 2 * asin(sqrt(a))
}
```

El filtro de pools ocultos que existe actualmente en `ExploreContent` se mueve dentro de `applyFilters`.

---

## UI

### `FilterChipsRow` (nuevo fichero)

```
app/src/main/java/com/burixer85/piscinamap/features/explore/presentation/FilterChipsRow.kt
```

**Firma:**
```kotlin
@Composable
fun FilterChipsRow(
    filters: FilterState,
    onFiltersChange: (FilterState) -> Unit,
    modifier: Modifier = Modifier
)
```

**Comportamiento de cada chip:**

| Chip | Comportamiento al pulsar |
|---|---|
| Abierto ahora | Toggle `openNow` on/off |
| ⭐ Valoración | Cicla: `null → 3f → 4f → 4.5f → null` |
| 📍 Distancia | Cicla: `null → 5 → 10 → 25 → null` |
| 🏊 Tipo | Abre `DropdownMenu` con multiselect (Pública / Municipal / Hotel) |

**Estados visuales:**
- Chip inactivo: borde sutil, texto gris
- Chip activo: color primario, texto blanco, ✕ al final para limpiar ese filtro

**Implementación interna:**
- `LazyRow` con `FilterChip` de Material 3
- Chip de Tipo usa `DropdownMenu` + `DropdownMenuItem` con `Checkbox`

### Cambios en `ExploreScreen.kt`

1. Llamar `viewModel.setUserLocation(lat, lng)` cuando se obtiene la ubicación (antes de `fetchPools`)
2. Insertar `FilterChipsRow` entre la cabecera y el `LazyColumn`
3. Usar `viewModel.filteredPools.collectAsStateWithLifecycle()` en lugar de `uiState.pools`
4. Añadir contador "X piscinas encontradas" encima de `FilterChipsRow`
5. Empty state específico cuando `filteredPools` es vacío por filtros activos: mostrar mensaje + botón "Limpiar filtros" que llama `viewModel.updateFilter(FilterState())`

---

## Tests

### Unit tests

**`ExploreViewModelTest`** — cubrir `applyFilters`:
- Filtro por rating mínimo
- Filtro openNow con pools sin dato (`isOpenNow = null`)
- Filtro distancia (haversine correcto)
- Filtro tipo — single y multiselect
- Combinación de filtros activos
- `FilterState` vacío devuelve todos los pools

**`PoolTypeMapperTest`** — cubrir `toPoolType()`:
- Cada categoría de tipos conocidos
- Lista vacía → `UNKNOWN`
- Tipos mixtos → prioridad de hotel sobre otros

### Compose tests

**`FilterChipsRowTest`**:
- Chip Abierto cicla correctamente
- Chip Rating cicla por presets
- Chip Distancia cicla por presets
- Chip Tipo abre menú y emite `selectedTypes` correcto
- Chip activo muestra ✕ y al pulsarlo limpia ese filtro

---

## Ficheros afectados

| Fichero | Acción |
|---|---|
| `core/domain/model/PoolType.kt` | Nuevo |
| `core/domain/model/FilterState.kt` | Nuevo |
| `features/explore/presentation/FilterChipsRow.kt` | Nuevo |
| `core/domain/model/Pool.kt` | Modificado — añade `poolType` |
| `core/data/dto/PlacesResponse.kt` | Modificado — añade `toPoolType()` y mapeo |
| `features/explore/presentation/ExploreViewModel.kt` | Modificado — `filteredPools`, `updateFilter`, `setUserLocation`, `applyFilters` |
| `features/explore/presentation/ExploreScreen.kt` | Modificado — integra `FilterChipsRow`, consume `filteredPools` |

Sin cambios en: repositorio, use cases, DI, navegación, ni ninguna otra feature.
