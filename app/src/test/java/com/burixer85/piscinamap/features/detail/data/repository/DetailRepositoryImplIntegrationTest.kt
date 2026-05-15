package com.burixer85.piscinamap.features.detail.data.repository

import com.burixer85.piscinamap.core.data.GooglePlacesApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class DetailRepositoryImplIntegrationTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: DetailRepositoryImpl

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }
        val api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GooglePlacesApi::class.java)

        repository = DetailRepositoryImpl(api)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getPoolDetails returns pool with mapped fields on OK response`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(DETAIL_OK_RESPONSE)
        )

        val result = repository.getPoolDetails("ChIJtest123")

        assertTrue(result.isSuccess)
        val pool = result.getOrThrow()
        assertEquals("ChIJtest123", pool.id)
        assertEquals("Piscina Municipal Centro", pool.name)
        assertEquals(37.388, pool.latitude, 0.001)
        assertEquals(-5.982, pool.longitude, 0.001)
        assertEquals("Av. de la Constitución 1, Sevilla", pool.address)
        assertEquals(4.3f, pool.rating)
        assertEquals(true, pool.isOpenNow)
        assertEquals("+34 954 100 200", pool.formattedPhone)
        assertEquals(1, pool.reviews.size)
        assertEquals("Juan García", pool.reviews[0].authorName)
        assertNotNull(pool.photoUrls)
        assertTrue(pool.photoUrls.isNotEmpty())
    }

    @Test
    fun `getPoolDetails maps services from types correctly`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(DETAIL_WITH_TYPES_RESPONSE)
        )

        val result = repository.getPoolDetails("ChIJtest456")

        assertTrue(result.isSuccess)
        val pool = result.getOrThrow()
        assertTrue(pool.services.contains("Piscina"))
        assertTrue(pool.services.contains("Gimnasio"))
    }

    @Test
    fun `getPoolDetails returns failure when result is null`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"status":"NOT_FOUND","result":null}""")
        )

        val result = repository.getPoolDetails("ChIJnotfound")

        assertTrue(result.isFailure)
        assertEquals("Pool not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getPoolDetails returns failure on network error`() = runTest {
        mockWebServer.shutdown()

        val result = repository.getPoolDetails("ChIJtest789")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getPoolDetails maps opening hours weekday text`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(DETAIL_OK_RESPONSE)
        )

        val result = repository.getPoolDetails("ChIJtest123")

        assertTrue(result.isSuccess)
        val pool = result.getOrThrow()
        assertEquals(2, pool.openingHours.size)
        assertEquals("Lunes: 9:00 - 21:00", pool.openingHours[0])
    }

    @Test
    fun `getPoolDetails handles missing optional fields gracefully`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(DETAIL_MINIMAL_RESPONSE)
        )

        val result = repository.getPoolDetails("ChIJminimal")

        assertTrue(result.isSuccess)
        val pool = result.getOrThrow()
        assertEquals("Piscina Mínima", pool.name)
        assertEquals("", pool.address)
        assertTrue(pool.reviews.isEmpty())
        assertTrue(pool.photoUrls.isEmpty())
        assertTrue(pool.openingHours.isEmpty())
    }

    companion object {
        private val DETAIL_OK_RESPONSE = """
            {
              "status": "OK",
              "result": {
                "name": "Piscina Municipal Centro",
                "geometry": { "location": { "lat": 37.388, "lng": -5.982 } },
                "formatted_address": "Av. de la Constitución 1, Sevilla",
                "rating": 4.3,
                "opening_hours": {
                  "weekday_text": ["Lunes: 9:00 - 21:00", "Martes: 9:00 - 21:00"],
                  "open_now": true
                },
                "formatted_phone_number": "+34 954 100 200",
                "reviews": [
                  {
                    "author_name": "Juan García",
                    "rating": 5.0,
                    "text": "Excelente piscina",
                    "relative_time_description": "hace 2 semanas",
                    "time": 1715000000
                  }
                ],
                "types": ["swimming_pool"],
                "photos": [{ "photo_reference": "AUGGfZkPhoto1" }]
              }
            }
        """.trimIndent()

        private val DETAIL_WITH_TYPES_RESPONSE = """
            {
              "status": "OK",
              "result": {
                "name": "Club Deportivo Acuático",
                "geometry": { "location": { "lat": 37.390, "lng": -5.985 } },
                "types": ["swimming_pool", "gym"],
                "reviews": []
              }
            }
        """.trimIndent()

        private val DETAIL_MINIMAL_RESPONSE = """
            {
              "status": "OK",
              "result": {
                "name": "Piscina Mínima",
                "geometry": { "location": { "lat": 37.400, "lng": -5.990 } },
                "types": []
              }
            }
        """.trimIndent()
    }
}
