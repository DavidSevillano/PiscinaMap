package com.burixer85.piscinamap.core.analytics

interface AnalyticsManager {
    fun trackScreen(screenName: String)
    fun trackEvent(name: String, params: Map<String, String> = emptyMap())
    fun logNonFatalError(throwable: Throwable)
}
