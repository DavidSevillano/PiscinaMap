package com.burixer85.piscinamap.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class PiscinaRoute : NavKey

@Serializable
data object HomeRouteNav : PiscinaRoute()

@Serializable
data object ExploreRouteNav : PiscinaRoute()

@Serializable
data class DetailRouteNav(val poolId: String) : PiscinaRoute()