package com.burixer85.piscinamap.features.home.data.repository

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

class PoolRepositoryImplIntegrationTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: PoolRepositoryImpl
    private lateinit var mockContext: Context
    private lateinit var mockSharedPrefs: SharedPreferences

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        mockSharedPrefs = mockk()
        every { mockSharedPrefs.getStringSet("hidden_pool_ids", emptySet()) } returns emptySet()

        mockContext = mockk()
        every { mockContext.getSharedPreferences("hidden_pools", Context.MODE_PRIVATE) } returns mockSharedPrefs

        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }
        val api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GooglePlacesApi::class.java)

        repository = PoolRepositoryImpl(api, mockContext)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `searchNearbyPools returns mapped pools on OK response`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(NEARBY_OK_RESPONSE))

        val result = repository.searchNearbyPools(37.388, -5.982, 2500)

        assertTrue(result.isSuccess)
        val pools = result.getOrThrow()
        assertEquals(1, pools.size)
        assertEquals("Piscina Municipal Las Delicias", pools[0].name)
        assertEquals("ChIJvalid1", pools[0].id)
        assertEquals(4.2f, pools[0].rating)
        assertEquals(true, pools[0].isOpenNow)
    }

    @Test
    fun `searchNearbyPools filters out exact generic names`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(NEARBY_GENERIC_NAMES_RESPONSE))

        val result = repository.searchNearbyPools(37.388, -5.982, 2500)

        assertTrue(result.isSuccess)
        val pools = result.getOrThrow()
        val names = pools.map { it.name.lowercase() }
        assertFalse(names.contains("swimming pool"))
        assertFalse(names.contains("piscina"))
        assertFalse(names.contains("pool"))
    }

    @Test
    fun `searchNearbyPools filters out excluded keyword patterns`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(NEARBY_EXCLUDED_PATTERNS_RESPONSE))

        val result = repository.searchNearbyPools(37.388, -5.982, 2500)

        assertTrue(result.isSuccess)
        val pools = result.getOrThrow()
        val names = pools.map { it.name.lowercase() }
        assertTrue("Should only keep valid pool", pools.size == 1)
        assertEquals("Piscina Deportiva Triana", pools[0].name)
        assertFalse(names.any { it.contains("tienda") })
        assertFalse(names.any { it.contains("venta") })
        assertFalse(names.any { it.contains("s.l.") })
        assertFalse(names.any { it.contains("s.a.") })
    }

    @Test
    fun `searchNearbyPools marks pool as hidden when id is in hidden set`() = runTest {
        every { mockSharedPrefs.getStringSet("hidden_pool_ids", emptySet()) } returns setOf("ChIJvalid1")

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(NEARBY_OK_RESPONSE))

        val result = repository.searchNearbyPools(37.388, -5.982, 2500)

        assertTrue(result.isSuccess)
        val pool = result.getOrThrow().first()
        assertTrue(pool.isHidden)
    }

    @Test
    fun `searchNearbyPools pool is not hidden when id is not in hidden set`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(NEARBY_OK_RESPONSE))

        val result = repository.searchNearbyPools(37.388, -5.982, 2500)

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow().first().isHidden)
    }

    @Test
    fun `searchNearbyPools returns failure on REQUEST_DENIED`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"REQUEST_DENIED","results":[]}"""))

        val result = repository.searchNearbyPools(37.388, -5.982, 2500)

        assertTrue(result.isFailure)
        assertEquals("Error de API: Solicitud denegada", result.exceptionOrNull()?.message)
    }

    @Test
    fun `searchNearbyPools returns failure on OVER_QUERY_LIMIT`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"OVER_QUERY_LIMIT","results":[]}"""))

        val result = repository.searchNearbyPools(37.388, -5.982, 2500)

        assertTrue(result.isFailure)
        assertEquals("Cuota de API excedida", result.exceptionOrNull()?.message)
    }

    @Test
    fun `searchNearbyPools returns failure on INVALID_REQUEST`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"INVALID_REQUEST","results":[]}"""))

        val result = repository.searchNearbyPools(37.388, -5.982, 2500)

        assertTrue(result.isFailure)
        assertEquals("Solicitud inválida", result.exceptionOrNull()?.message)
    }

    @Test
    fun `searchNearbyPools returns empty list on ZERO_RESULTS`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"ZERO_RESULTS","results":[]}"""))

        val result = repository.searchNearbyPools(37.388, -5.982, 2500)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun `searchNearbyPools returns failure on network error`() = runTest {
        mockWebServer.shutdown()

        val result = repository.searchNearbyPools(37.388, -5.982, 2500)

        assertTrue(result.isFailure)
    }

    @Test
    fun `getPoolDetails returns pool with placeId set as id`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(DETAIL_OK_RESPONSE))

        val result = repository.getPoolDetails("ChIJtest999")

        assertTrue(result.isSuccess)
        assertEquals("ChIJtest999", result.getOrThrow().id)
    }

    companion object {
        private val NEARBY_OK_RESPONSE = """
            {
              "status": "OK",
              "results": [
                {
                  "place_id": "ChIJvalid1",
                  "name": "Piscina Municipal Las Delicias",
                  "vicinity": "Calle Mayor 1, Sevilla",
                  "geometry": { "location": { "lat": 37.388, "lng": -5.982 } },
                  "rating": 4.2,
                  "opening_hours": { "open_now": true },
                  "photos": [{ "photo_reference": "photo_ref_1" }],
                  "types": ["swimming_pool"]
                }
              ]
            }
        """.trimIndent()

        private val NEARBY_GENERIC_NAMES_RESPONSE = """
            {
              "status": "OK",
              "results": [
                {
                  "place_id": "id_generic_1",
                  "name": "swimming pool",
                  "vicinity": "Dirección 1",
                  "geometry": { "location": { "lat": 37.388, "lng": -5.982 } }
                },
                {
                  "place_id": "id_generic_2",
                  "name": "Piscina",
                  "vicinity": "Dirección 2",
                  "geometry": { "location": { "lat": 37.389, "lng": -5.983 } }
                },
                {
                  "place_id": "id_generic_3",
                  "name": "pool",
                  "vicinity": "Dirección 3",
                  "geometry": { "location": { "lat": 37.390, "lng": -5.984 } }
                }
              ]
            }
        """.trimIndent()

        private val NEARBY_EXCLUDED_PATTERNS_RESPONSE = """
            {
              "status": "OK",
              "results": [
                {
                  "place_id": "id_valid",
                  "name": "Piscina Deportiva Triana",
                  "vicinity": "Triana, Sevilla",
                  "geometry": { "location": { "lat": 37.388, "lng": -5.982 } }
                },
                {
                  "place_id": "id_tienda",
                  "name": "Tienda de piscinas SurPool",
                  "vicinity": "Polígono 1",
                  "geometry": { "location": { "lat": 37.389, "lng": -5.983 } }
                },
                {
                  "place_id": "id_sl",
                  "name": "Piscinas Andalucía S.L.",
                  "vicinity": "Polígono 2",
                  "geometry": { "location": { "lat": 37.390, "lng": -5.984 } }
                },
                {
                  "place_id": "id_sa",
                  "name": "Aqua Pools S.A. Construcciones",
                  "vicinity": "Polígono 3",
                  "geometry": { "location": { "lat": 37.391, "lng": -5.985 } }
                },
                {
                  "place_id": "id_ventas",
                  "name": "Ventas de accesorios piscina",
                  "vicinity": "Centro Comercial",
                  "geometry": { "location": { "lat": 37.392, "lng": -5.986 } }
                }
              ]
            }
        """.trimIndent()

        private val DETAIL_OK_RESPONSE = """
            {
              "status": "OK",
              "result": {
                "name": "Piscina Municipal Centro",
                "geometry": { "location": { "lat": 37.388, "lng": -5.982 } },
                "formatted_address": "Av. Principal 1",
                "rating": 4.5,
                "types": ["swimming_pool"]
              }
            }
        """.trimIndent()
    }
}
