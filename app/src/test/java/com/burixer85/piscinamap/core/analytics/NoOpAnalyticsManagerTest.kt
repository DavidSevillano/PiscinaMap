package com.burixer85.piscinamap.core.analytics

import org.junit.Test

class NoOpAnalyticsManagerTest {

    private val sut = NoOpAnalyticsManager()

    @Test
    fun `trackScreen does not throw`() {
        sut.trackScreen("HomeScreen")
    }

    @Test
    fun `trackEvent does not throw`() {
        sut.trackEvent("pool_detail_viewed", mapOf("pool_id" to "abc123"))
    }

    @Test
    fun `trackEvent with empty params does not throw`() {
        sut.trackEvent("map_area_searched")
    }

    @Test
    fun `logNonFatalError does not throw`() {
        sut.logNonFatalError(RuntimeException("test error"))
    }
}
