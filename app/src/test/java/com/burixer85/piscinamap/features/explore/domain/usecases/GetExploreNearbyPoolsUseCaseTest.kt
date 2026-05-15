package com.burixer85.piscinamap.features.explore.domain.usecases

import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.explore.domain.repository.ExploreRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetExploreNearbyPoolsUseCaseTest {

    private val repository: ExploreRepository = mockk()
    private val useCase = GetExploreNearbyPoolsUseCase(repository)

    private val fakePools = listOf(
        Pool("p1", "Pool 1", 40.0, -3.0, "Address 1", null, null)
    )

    @Test
    fun `invoke returns success from repository`() = runTest {
        coEvery { repository.searchNearbyPools(40.0, -3.0, 50000) } returns Result.success(fakePools)

        val result = useCase(40.0, -3.0)

        assertTrue(result.isSuccess)
        assertEquals(fakePools, result.getOrNull())
    }

    @Test
    fun `invoke uses default radius of 50000`() = runTest {
        coEvery { repository.searchNearbyPools(40.0, -3.0, 50000) } returns Result.success(fakePools)

        useCase(40.0, -3.0)

        coVerify(exactly = 1) { repository.searchNearbyPools(40.0, -3.0, 50000) }
    }

    @Test
    fun `invoke passes custom radius to repository`() = runTest {
        coEvery { repository.searchNearbyPools(40.0, -3.0, 10000) } returns Result.success(fakePools)

        useCase(40.0, -3.0, 10000)

        coVerify(exactly = 1) { repository.searchNearbyPools(40.0, -3.0, 10000) }
    }

    @Test
    fun `invoke propagates repository failure`() = runTest {
        coEvery { repository.searchNearbyPools(any(), any(), any()) } returns Result.failure(Exception("Network error"))

        val result = useCase(0.0, 0.0)

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke returns empty list on zero results`() = runTest {
        coEvery { repository.searchNearbyPools(any(), any(), any()) } returns Result.success(emptyList())

        val result = useCase(40.0, -3.0)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }
}
