package com.burixer85.piscinamap.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.burixer85.piscinamap.features.detail.presentation.DetailScreen
import com.burixer85.piscinamap.features.detail.presentation.DetailViewModel
import com.burixer85.piscinamap.features.explore.presentation.ExploreScreen
import com.burixer85.piscinamap.features.home.presentation.HomeScreen

@Composable
fun PiscinaMapNavGraph(
    modifier: Modifier = Modifier
) {
    val backStack = rememberNavBackStack(HomeRouteNav)

    val currentRoute = backStack.lastOrNull()

    val showBottomBar = currentRoute is HomeRouteNav || currentRoute is ExploreRouteNav

    Box(modifier = modifier.fillMaxSize()) {
        NavDisplay(
            modifier = Modifier.fillMaxSize(),
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<HomeRouteNav> {
                    HomeScreen(
                        onNavigateToDetail = { poolId ->
                            backStack.add(DetailRouteNav(poolId))
                        },
                        bottomPadding = 100
                    )
                }
                entry<ExploreRouteNav> {
                    ExploreScreen()
                }
                entry<DetailRouteNav> { key ->
                    val detailViewModel: DetailViewModel = viewModel(
                        key = "detail_vm_${key.poolId}"
                    )
                    detailViewModel.setPoolId(key.poolId)
                    DetailScreen(
                        poolId = key.poolId,
                        viewModel = detailViewModel,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
            },
            transitionSpec = {
                val horizontalAnimation = slideInHorizontally(
                    animationSpec = tween(300)
                ) { it } togetherWith slideOutHorizontally(
                    animationSpec = tween(300)
                ) { -it }
                val verticalUpAnimation = slideInVertically(
                    animationSpec = tween(300)
                ) { it } togetherWith slideOutVertically(
                    animationSpec = tween(300)
                ) { -it }
                val verticalDownAnimation = slideInVertically(
                    animationSpec = tween(300)
                ) { -it } togetherWith slideOutVertically(
                    animationSpec = tween(300)
                ) { it }

                val fromRouteName = initialState.key.toString()
                val toRouteName = targetState.key.toString()

                when {
                    // Home -> Explore, Explore -> Home
                    !toRouteName.startsWith("DetailRouteNav") &&
                            !fromRouteName.startsWith("DetailRouteNav") -> horizontalAnimation

                    // Home -> Detail (de abajo hacia arriba)
                    toRouteName.startsWith("DetailRouteNav") &&
                            !fromRouteName.startsWith("DetailRouteNav") -> verticalUpAnimation

                    // Detail -> Home (de arriba hacia abajo)
                    fromRouteName.startsWith("DetailRouteNav") -> verticalDownAnimation

                    else -> horizontalAnimation
                }
            },
            popTransitionSpec = {
                val horizontalAnimation = slideInHorizontally(
                    animationSpec = tween(300)
                ) { -it } togetherWith slideOutHorizontally(
                    animationSpec = tween(300)
                ) { it }
                val verticalDownAnimation = slideInVertically(
                    animationSpec = tween(300)
                ) { -it } togetherWith slideOutVertically(
                    animationSpec = tween(300)
                ) { it }

                val fromRouteName = initialState.key.toString()

                when {
                    // Detail -> Home/Explore (de arriba hacia abajo)
                    fromRouteName.startsWith("DetailRouteNav") -> verticalDownAnimation

                    // Explore -> Home
                    else -> horizontalAnimation
                }
            }
        )

        AnimatedVisibility(
            visible = showBottomBar,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = currentRoute is HomeRouteNav,
                    onClick = {
                        val existingHomeIndex = backStack.indexOfFirst { it is HomeRouteNav }
                        if (existingHomeIndex != -1 && existingHomeIndex != backStack.lastIndex) {
                            while (backStack.lastIndex > existingHomeIndex) {
                                backStack.removeLastOrNull()
                            }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Place, contentDescription = "Explorar") },
                    label = { Text("Explorar") },
                    selected = currentRoute is ExploreRouteNav,
                    onClick = {
                        val existingExploreIndex = backStack.indexOfFirst { it is ExploreRouteNav }
                        if (existingExploreIndex != -1 && existingExploreIndex != backStack.lastIndex) {
                            while (backStack.lastIndex > existingExploreIndex) {
                                backStack.removeLastOrNull()
                            }
                        } else if (existingExploreIndex == -1) {
                            backStack.add(ExploreRouteNav)
                        }
                    }
                )
            }
        }
    }
}