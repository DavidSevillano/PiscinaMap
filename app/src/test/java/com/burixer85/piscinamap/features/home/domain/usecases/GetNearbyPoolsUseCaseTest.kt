package com.burixer85.piscinamap.features.home.domain.usecases

import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.home.domain.repository.PoolRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetNearbyPoolsUseCaseTest {

    private val repository: PoolRepository = mockk()
    private val useCase = GetNearbyPoolsUseCase(repository)

    private val fakePools = listOf(
        Pool("p1", "Pool 1", 40.0, -3.0, "Address 1", null, null),
        Pool("p2", "Pool 2", 40.1, -3.1, "Address 2", 4.0f, true)
    )

    @Test
    fun `invoke returns success from repository`() = runTest {
        coEvery { repository.searchNearbyPools(40.0, -3.0, 2500) } returns Result.success(fakePools)

        val result = useCase(40.0, -3.0)

        assertTrue(result.isSuccess)
        assertEquals(fakePools, result.getOrNull())
    }

    @Test
    fun `invoke uses default radius of 2500`() = runTest {
        coEvery { repository.searchNearbyPools(40.0, -3.0, 2500) } returns Result.success(fakePools)

        useCase(40.0, -3.0)

        coVerify(exactly = 1) { repository.searchNearbyPools(40.0, -3.0, 2500) }
    }

    @Test
    fun `invoke passes custom radius to repository`() = runTest {
        coEvery { repository.searchNearbyPools(40.0, -3.0, 5000) } returns Result.success(fakePools)

        useCase(40.0, -3.0, 5000)

        coVerify(exactly = 1) { repository.searchNearbyPools(40.0, -3.0, 5000) }
    }

    @Test
    fun `invoke propagates repository failure`() = runTest {
        coEvery { repository.searchNearbyPools(any(), any(), any()) } returns Result.failure(Exception("API error"))

        val result = useCase(0.0, 0.0)

        assertTrue(result.isFailure)
        assertEquals("API error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke returns empty list on zero results`() = runTest {
        coEvery { repository.searchNearbyPools(any(), any(), any()) } returns Result.success(emptyList())

        val result = useCase(40.0, -3.0)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }
}
