package com.burixer85.piscinamap.features.explore.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
                                    if (type in filters.selectedTypes) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    } else {
                                        Spacer(Modifier.width(24.dp))
                                    }
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
