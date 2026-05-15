package com.burixer85.piscinamap.features.detail.domain.usecases

import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.features.home.domain.repository.PoolRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetPoolDetailsUseCaseTest {

    private val repository: PoolRepository = mockk()
    private val useCase = GetPoolDetailsUseCase(repository)

    private val fakePool = Pool(
        id = "pool1",
        name = "Test Pool",
        latitude = 40.0,
        longitude = -3.0,
        address = "Calle Test",
        rating = null,
        isOpenNow = null
    )

    @Test
    fun `invoke returns success from repository`() = runTest {
        coEvery { repository.getPoolDetails("pool1") } returns Result.success(fakePool)

        val result = useCase("pool1")

        assertTrue(result.isSuccess)
        assertEquals(fakePool, result.getOrNull())
    }

    @Test
    fun `invoke returns failure from repository`() = runTest {
        coEvery { repository.getPoolDetails("invalid") } returns Result.failure(Exception("Not found"))

        val result = useCase("invalid")

        assertTrue(result.isFailure)
        assertEquals("Not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke passes placeId to repository`() = runTest {
        coEvery { repository.getPoolDetails(any()) } returns Result.success(fakePool)

        useCase("pool1")

        coVerify(exactly = 1) { repository.getPoolDetails("pool1") }
    }
}
