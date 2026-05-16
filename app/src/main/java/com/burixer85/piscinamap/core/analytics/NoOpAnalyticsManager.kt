package com.burixer85.piscinamap.core.analytics

import javax.inject.Inject

class NoOpAnalyticsManager @Inject constructor() : AnalyticsManager {
    override fun trackScreen(screenName: String) = Unit
    override fun trackEvent(name: String, params: Map<String, String>) = Unit
    override fun logNonFatalError(throwable: Throwable) = Unit
}
