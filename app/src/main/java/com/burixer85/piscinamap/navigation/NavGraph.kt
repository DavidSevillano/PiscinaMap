package com.burixer85.piscinamap.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.burixer85.piscinamap.features.detail.presentation.DetailScreen
import com.burixer85.piscinamap.features.home.presentation.HomeScreen
import com.burixer85.piscinamap.features.home.presentation.HomeViewModel

@Composable
fun PiscinaMapNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = Modifier.fillMaxSize()
    ) {
        composable<HomeRoute>(
            enterTransition = { fadeIn(animationSpec = tween(300)) }
        ) { backStackEntry ->
            val viewModel: HomeViewModel = hiltViewModel(backStackEntry)

            HomeScreen(
                viewModel = viewModel,
                onNavigateToDetail = { id ->
                    navController.navigate(DetailRoute(poolId = id)) {
                        popUpTo(HomeRoute) { saveState = true }
                    }
                }
            )
        }

        composable<DetailRoute>(
            enterTransition = {
                slideInVertically(initialOffsetY = { it }, animationSpec = tween(400))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400))
            }
        ) { backStackEntry ->
            val args = backStackEntry.toRoute<DetailRoute>()

            Surface(modifier = Modifier.fillMaxSize()) {
                DetailScreen(
                    poolId = args.poolId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}