package com.burixer85.piscinamap.features.explore.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.burixer85.piscinamap.core.domain.model.FilterState

@Composable
fun FilterChipsRow(
    filters: FilterState,
    onFiltersChange: (FilterState) -> Unit,
    modifier: Modifier = Modifier
) {
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
