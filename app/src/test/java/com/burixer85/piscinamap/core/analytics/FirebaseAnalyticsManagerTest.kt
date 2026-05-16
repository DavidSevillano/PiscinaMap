package com.burixer85.piscinamap.core.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.Runs
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FirebaseAnalyticsManagerTest {

    private val firebaseAnalytics: FirebaseAnalytics = mockk(relaxed = true)
    private val crashlytics: FirebaseCrashlytics = mockk(relaxed = true)
    private lateinit var sut: FirebaseAnalyticsManager

    @Before
    fun setUp() {
        mockkConstructor(Bundle::class)
        sut = FirebaseAnalyticsManager(firebaseAnalytics, crashlytics)
    }

    @After
    fun tearDown() {
        unmockkConstructor(Bundle::class)
    }

    @Test
    fun `trackScreen logs SCREEN_VIEW event with screen_name param`() {
        val keys = mutableListOf<String>()
        val values = mutableListOf<String>()

        every { anyConstructed<Bundle>().putString(capture(keys), capture(values)) } just Runs

        sut.trackScreen("HomeScreen")

        verify { firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, any()) }
        assert(keys.contains(FirebaseAnalytics.Param.SCREEN_NAME))
        assert(values[keys.indexOf(FirebaseAnalytics.Param.SCREEN_NAME)] == "HomeScreen")
    }

    @Test
    fun `trackEvent logs custom event with all params in bundle`() {
        val putStringKeys = mutableListOf<String>()
        val putStringValues = mutableListOf<String>()

        every { anyConstructed<Bundle>().putString(capture(putStringKeys), capture(putStringValues)) } returns Unit

        sut.trackEvent("pool_detail_viewed", mapOf("pool_id" to "abc", "pool_name" to "Piscina Test"))

        verify { firebaseAnalytics.logEvent("pool_detail_viewed", any()) }
        val params = putStringKeys.zip(putStringValues).toMap()
        assertEquals("abc", params["pool_id"])
        assertEquals("Piscina Test", params["pool_name"])
    }

    @Test
    fun `trackEvent with empty params logs event with empty bundle`() {
        sut.trackEvent("map_area_searched")

        verify { firebaseAnalytics.logEvent("map_area_searched", any()) }
    }

    @Test
    fun `logNonFatalError records exception to Crashlytics`() {
        val ex = RuntimeException("network failure")

        sut.logNonFatalError(ex)

        verify { crashlytics.recordException(ex) }
    }
}
