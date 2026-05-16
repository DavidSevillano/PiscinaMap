package com.burixer85.piscinamap.core.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.burixer85.piscinamap.core.data.local.entity.PoolCacheEntity
import com.burixer85.piscinamap.core.data.local.entity.PoolDetailCacheEntity
import com.burixer85.piscinamap.core.domain.model.Review
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PoolCacheDaoTest {

    private lateinit var db: PoolDatabase
    private lateinit var poolCacheDao: PoolCacheDao
    private lateinit var poolDetailCacheDao: PoolDetailCacheDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PoolDatabase::class.java).build()
        poolCacheDao = db.poolCacheDao()
        poolDetailCacheDao = db.poolDetailCacheDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndRetrievePoolsInBoundingBox() = runTest {
        val now = System.currentTimeMillis()
        val pool = PoolCacheEntity("id1", "Piscina Test", 40.416, -3.703, "Calle Mayor", 4.2f, true, now)
        poolCacheDao.insertPools(listOf(pool))

        val result = poolCacheDao.getPoolsInBoundingBox(40.0, 41.0, -4.0, -3.0, now - 1000L)

        assertEquals(1, result.size)
        assertEquals("Piscina Test", result[0].name)
    }

    @Test
    fun poolOutsideBoundingBoxIsNotReturned() = runTest {
        val now = System.currentTimeMillis()
        val pool = PoolCacheEntity("id2", "Piscina Lejana", 51.5, -0.1, "London Rd", 3.5f, null, now)
        poolCacheDao.insertPools(listOf(pool))

        val result = poolCacheDao.getPoolsInBoundingBox(40.0, 41.0, -4.0, -3.0, now - 1000L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun expiredPoolIsExcludedByTtlFilter() = runTest {
        val eightDaysAgo = System.currentTimeMillis() - 8 * 24 * 60 * 60 * 1000L
        val pool = PoolCacheEntity("id3", "Piscina Antigua", 40.416, -3.703, "Calle X", 4.0f, false, eightDaysAgo)
        poolCacheDao.insertPools(listOf(pool))

        val sevenDaysTtl = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val result = poolCacheDao.getPoolsInBoundingBox(40.0, 41.0, -4.0, -3.0, sevenDaysTtl)

        assertTrue(result.isEmpty())
    }

    @Test
    fun insertPoolsUpsertsDuplicatesWithLatestData() = runTest {
        val now = System.currentTimeMillis()
        val original = PoolCacheEntity("id4", "Piscina Original", 40.416, -3.703, "Calle A", 3.0f, true, now)
        val updated = PoolCacheEntity("id4", "Piscina Actualizada", 40.416, -3.703, "Calle A", 4.8f, true, now + 1000L)

        poolCacheDao.insertPools(listOf(original))
        poolCacheDao.insertPools(listOf(updated))

        val result = poolCacheDao.getPoolsInBoundingBox(40.0, 41.0, -4.0, -3.0, now - 1000L)

        assertEquals(1, result.size)
        assertEquals("Piscina Actualizada", result[0].name)
        assertEquals(4.8f, result[0].rating!!, 0.0f)
    }

    @Test
    fun insertDetailAndRetrieveWithFreshTtl() = runTest {
        val now = System.currentTimeMillis()
        val detail = PoolDetailCacheEntity(
            placeId = "id5", name = "Piscina Detalle", latitude = 40.416, longitude = -3.703,
            address = "Gran Vía 1", rating = 4.5f, isOpenNow = true,
            photoUrls = listOf("url1"), openingHours = listOf("Lunes: 9-21"),
            currentOpeningHours = "9:00 - 21:00", services = listOf("Piscina"),
            reviews = listOf(Review("Ana", 5f, "Genial", "hace 1 semana")),
            formattedPhone = "+34 900 000 000", cachedAt = now
        )
        poolDetailCacheDao.insertDetail(detail)

        val result = poolDetailCacheDao.getDetail("id5", now - 1000L)

        assertNotNull(result)
        assertEquals("Piscina Detalle", result!!.name)
        assertEquals(1, result.reviews.size)
        assertEquals("Ana", result.reviews[0].authorName)
        assertEquals(listOf("url1"), result.photoUrls)
    }

    @Test
    fun expiredDetailIsNotReturnedByFreshQuery() = runTest {
        val twentyFiveHoursAgo = System.currentTimeMillis() - 25 * 60 * 60 * 1000L
        val detail = PoolDetailCacheEntity(
            placeId = "id6", name = "Piscina Expirada", latitude = 40.416, longitude = -3.703,
            address = "Calle Y", rating = 3.0f, isOpenNow = false,
            photoUrls = emptyList(), openingHours = emptyList(),
            currentOpeningHours = null, services = emptyList(),
            reviews = emptyList(), formattedPhone = null, cachedAt = twentyFiveHoursAgo
        )
        poolDetailCacheDao.insertDetail(detail)

        val twentyFourHoursTtl = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        val result = poolDetailCacheDao.getDetail("id6", twentyFourHoursTtl)

        assertNull(result)
    }

    @Test
    fun expiredDetailIsReturnedByIgnoreTtlQuery() = runTest {
        val twentyFiveHoursAgo = System.currentTimeMillis() - 25 * 60 * 60 * 1000L
        val detail = PoolDetailCacheEntity(
            placeId = "id7", name = "Piscina Stale", latitude = 40.416, longitude = -3.703,
            address = "Calle Z", rating = 3.0f, isOpenNow = false,
            photoUrls = emptyList(), openingHours = emptyList(),
            currentOpeningHours = null, services = emptyList(),
            reviews = emptyList(), formattedPhone = null, cachedAt = twentyFiveHoursAgo
        )
        poolDetailCacheDao.insertDetail(detail)

        val result = poolDetailCacheDao.getDetailIgnoringTtl("id7")

        assertNotNull(result)
        assertEquals("Piscina Stale", result!!.name)
    }
}
