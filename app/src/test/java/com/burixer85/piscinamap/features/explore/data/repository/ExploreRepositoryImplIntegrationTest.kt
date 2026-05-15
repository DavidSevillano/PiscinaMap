package com.burixer85.piscinamap.features.explore.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.burixer85.piscinamap.core.data.GooglePlacesApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class ExploreRepositoryImplIntegrationTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: ExploreRepositoryImpl
    private lateinit var mockContext: Context
    private lateinit var mockSharedPrefs: SharedPreferences

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        mockSharedPrefs = mockk()
        every { mockSharedPrefs.getStringSet("hidden_pool_ids", emptySet()) } returns emptySet()
        every { mockSharedPrefs.getStringSet("favorite_pool_ids", emptySet()) } returns emptySet()

        mockContext = mockk()
        every { mockContext.getSharedPreferences("hidden_pools", Context.MODE_PRIVATE) } returns mockSharedPrefs
        every { mockContext.getSharedPreferences("favorite_pools", Context.MODE_PRIVATE) } returns mockSharedPrefs

        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }
        val api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GooglePlacesApi::class.java)

        repository = ExploreRepositoryImpl(api, mockContext)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `searchNearbyPools returns all non-filtered pools on OK response`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(NEARBY_MIXED_RESPONSE))

        val result = repository.searchNearbyPools(37.388, -5.982, 50000)

        assertTrue(result.isSuccess)
        val pools = result.getOrThrow()
        assertEquals(2, pools.size)
        val names = pools.map { it.name }
        assertTrue(names.contains("Piscina Olímpica Norte"))
        assertTrue(names.contains("Club Natación Sur"))
    }

    @Test
    fun `searchNearbyPools sends request with correct coordinates`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(EMPTY_RESPONSE))

        repository.searchNearbyPools(40.4168, -3.7038, 50000)

        val request = mockWebServer.takeRequest()
        val path = request.path ?: ""
        assertTrue("Location param should contain lat,lng", path.contains("location=40.4168%2C-3.7038") || path.contains("location=40.4168,-3.7038"))
        assertTrue("Radius param should be present", path.contains("radius=50000"))
    }

    @Test
    fun `searchNearbyPools filters out generic names`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "status": "OK",
                  "results": [
                    {
                      "place_id": "id1",
                      "name": "Piscina",
                      "vicinity": "Calle 1",
                      "geometry": { "location": { "lat": 37.0, "lng": -5.0 } }
                    },
                    {
                      "place_id": "id2",
                      "name": "Pool",
                      "vicinity": "Calle 2",
                      "geometry": { "location": { "lat": 37.1, "lng": -5.1 } }
                    },
                    {
                      "place_id": "id3",
                      "name": "Complejo Aquático Municipal",
                      "vicinity": "Calle 3",
                      "geometry": { "location": { "lat": 37.2, "lng": -5.2 } }
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        val result = repository.searchNearbyPools(37.0, -5.0, 50000)

        assertTrue(result.isSuccess)
        val pools = result.getOrThrow()
        assertEquals(1, pools.size)
        assertEquals("Complejo Aquático Municipal", pools[0].name)
    }

    @Test
    fun `searchNearbyPools filters out excluded keyword patterns`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "status": "OK",
                  "results": [
                    {
                      "place_id": "id_slne",
                      "name": "Piscinas Aquapool SLNE",
                      "vicinity": "Polígono",
                      "geometry": { "location": { "lat": 37.0, "lng": -5.0 } }
                    },
                    {
                      "place_id": "id_venta",
                      "name": "Venta de piscinas online",
                      "vicinity": "Internet",
                      "geometry": { "location": { "lat": 37.1, "lng": -5.1 } }
                    },
                    {
                      "place_id": "id_ok",
                      "name": "Piscina Parque del Alamillo",
                      "vicinity": "Parque del Alamillo",
                      "geometry": { "location": { "lat": 37.2, "lng": -5.2 } }
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        val result = repository.searchNearbyPools(37.0, -5.0, 50000)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
        assertEquals("Piscina Parque del Alamillo", result.getOrThrow()[0].name)
    }

    @Test
    fun `searchNearbyPools does NOT return failure on REQUEST_DENIED unlike PoolRepository`() = runTest {
        // ExploreRepositoryImpl has no API status error checking — returns empty list instead
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"REQUEST_DENIED","results":[]}"""))

        val result = repository.searchNearbyPools(37.388, -5.982, 50000)

        assertTrue("ExploreRepository does not check status, should succeed with empty list", result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun `searchNearbyPools marks pool as favorite when id is in favorite set`() = runTest {
        every { mockSharedPrefs.getStringSet("favorite_pool_ids", emptySet()) } returns setOf("id_pool_1")

        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "status": "OK",
                  "results": [
                    {
                      "place_id": "id_pool_1",
                      "name": "Piscina Favorita",
                      "vicinity": "Calle Favorita",
                      "geometry": { "location": { "lat": 37.0, "lng": -5.0 } }
                    },
                    {
                      "place_id": "id_pool_2",
                      "name": "Piscina Normal",
                      "vicinity": "Calle Normal",
                      "geometry": { "location": { "lat": 37.1, "lng": -5.1 } }
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        val result = repository.searchNearbyPools(37.0, -5.0, 50000)

        assertTrue(result.isSuccess)
        val pools = result.getOrThrow()
        val favorite = pools.find { it.id == "id_pool_1" }
        val normal = pools.find { it.id == "id_pool_2" }
        assertTrue(favorite?.isFavorite == true)
        assertFalse(normal?.isFavorite == true)
    }

    @Test
    fun `searchNearbyPools pool is not favorite when id is not in favorite set`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "status": "OK",
                  "results": [
                    {
                      "place_id": "id_pool_1",
                      "name": "Piscina Visible",
                      "vicinity": "Calle Visible",
                      "geometry": { "location": { "lat": 37.0, "lng": -5.0 } }
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        val result = repository.searchNearbyPools(37.0, -5.0, 50000)

        assertTrue(result.isSuccess)
        val pools = result.getOrThrow()
        assertTrue("Expected at least one pool in result", pools.isNotEmpty())
        assertFalse(pools.first().isFavorite)
    }

    @Test
    fun `searchNearbyPools marks hidden pools correctly`() = runTest {
        every { mockSharedPrefs.getStringSet("hidden_pool_ids", emptySet()) } returns setOf("id_pool_1")

        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "status": "OK",
                  "results": [
                    {
                      "place_id": "id_pool_1",
                      "name": "Piscina Oculta",
                      "vicinity": "Calle Oculta",
                      "geometry": { "location": { "lat": 37.0, "lng": -5.0 } }
                    },
                    {
                      "place_id": "id_pool_2",
                      "name": "Piscina Visible",
                      "vicinity": "Calle Visible",
                      "geometry": { "location": { "lat": 37.1, "lng": -5.1 } }
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        val result = repository.searchNearbyPools(37.0, -5.0, 50000)

        assertTrue(result.isSuccess)
        val pools = result.getOrThrow()
        val hidden = pools.find { it.id == "id_pool_1" }
        val visible = pools.find { it.id == "id_pool_2" }
        assertTrue(hidden?.isHidden == true)
        assertFalse(visible?.isHidden == true)
    }

    @Test
    fun `searchNearbyPools returns failure on network error`() = runTest {
        mockWebServer.shutdown()

        val result = repository.searchNearbyPools(37.388, -5.982, 50000)

        assertTrue(result.isFailure)
    }

    companion object {
        private val NEARBY_MIXED_RESPONSE = """
            {
              "status": "OK",
              "results": [
                {
                  "place_id": "id_valid_1",
                  "name": "Piscina Olímpica Norte",
                  "vicinity": "Av. Norte 10, Sevilla",
                  "geometry": { "location": { "lat": 37.400, "lng": -5.970 } },
                  "rating": 4.5
                },
                {
                  "place_id": "id_valid_2",
                  "name": "Club Natación Sur",
                  "vicinity": "Av. Sur 20, Sevilla",
                  "geometry": { "location": { "lat": 37.380, "lng": -5.990 } },
                  "rating": 3.8
                },
                {
                  "place_id": "id_filter_1",
                  "name": "Piscinas s.l.",
                  "vicinity": "Polígono Industrial",
                  "geometry": { "location": { "lat": 37.350, "lng": -6.000 } }
                }
              ]
            }
        """.trimIndent()

        private val EMPTY_RESPONSE = """{"status":"ZERO_RESULTS","results":[]}"""
    }
}
