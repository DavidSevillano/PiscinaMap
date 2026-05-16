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
