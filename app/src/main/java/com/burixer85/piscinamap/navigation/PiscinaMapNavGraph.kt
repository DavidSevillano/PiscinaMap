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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.burixer85.piscinamap.R
import com.burixer85.piscinamap.core.presentation.components.AdMobBanner
import com.burixer85.piscinamap.core.presentation.components.NavItem
import com.burixer85.piscinamap.features.detail.presentation.DetailScreen
import com.burixer85.piscinamap.features.detail.presentation.DetailViewModel
import com.burixer85.piscinamap.features.explore.presentation.ExploreScreen
import com.burixer85.piscinamap.features.home.presentation.CameraStateHolder
import com.burixer85.piscinamap.features.favorites.presentation.FavoritesScreen
import com.burixer85.piscinamap.features.home.presentation.HomeScreen

@Composable
fun PiscinaMapNavGraph(
    modifier: Modifier = Modifier
) {
    var navCounter by remember { mutableIntStateOf(0) }
    val backStack = rememberNavBackStack(HomeRouteNav)

    val currentRoute = backStack.lastOrNull()

    val showBottomBar = currentRoute is HomeRouteNav || currentRoute is ExploreRouteNav || currentRoute is FavoritesRouteNav

    Box(modifier = modifier.fillMaxSize()) {
        NavDisplay(
            modifier = Modifier.fillMaxSize(),
            backStack = backStack,
            onBack = {
                navCounter++
                backStack.removeLastOrNull()
            },
            entryProvider = entryProvider {
                entry<HomeRouteNav> {
                    HomeScreen(
                        onNavigateToDetail = { poolId ->
                            CameraStateHolder.isNavigatingToDetail = true
                            navCounter++
                            backStack.add(DetailRouteNav(poolId))
                        },
                        bottomPadding = 130
                    )
                }
                entry<ExploreRouteNav> {
                    ExploreScreen(
                        onNavigateToDetail = { poolId ->
                            CameraStateHolder.isNavigatingToDetail = true
                            navCounter++
                            backStack.add(DetailRouteNav(poolId))
                        },
                        bottomPadding = 130
                    )
                }
                entry<FavoritesRouteNav> {
                    FavoritesScreen(
                        onNavigateToDetail = { poolId ->
                            CameraStateHolder.isNavigatingToDetail = true
                            navCounter++
                            backStack.add(DetailRouteNav(poolId))
                        },
                        bottomPadding = 130
                    )
                }
                entry<DetailRouteNav> { key ->
                    val detailViewModel: DetailViewModel = viewModel(
                        key = "detail_vm_${key.poolId}_$navCounter"
                    )
                    detailViewModel.setPoolId(key.poolId)
                    DetailScreen(
                        poolId = key.poolId,
                        viewModel = detailViewModel,
                        onBack = {
                            backStack.removeLastOrNull()
                        }
                    )
                }
            },
            transitionSpec = {
                val horizontalAnimation = (slideInHorizontally(
                    animationSpec = tween(300)
                ) { it } + fadeIn(animationSpec = tween(300))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(300)
                        ) { -it } + fadeOut(animationSpec = tween(300)))
                val verticalUpAnimation = (slideInVertically(
                    animationSpec = tween(300)
                ) { it } + fadeIn(animationSpec = tween(300))) togetherWith
                        (slideOutVertically(
                            animationSpec = tween(300)
                        ) { -it } + fadeOut(animationSpec = tween(300)))
                val verticalDownAnimation = (slideInVertically(
                    animationSpec = tween(300)
                ) { -it } + fadeIn(animationSpec = tween(300))) togetherWith
                        (slideOutVertically(
                            animationSpec = tween(300)
                        ) { it } + fadeOut(animationSpec = tween(300)))

                val fromRouteName = initialState.key.toString()
                val toRouteName = targetState.key.toString()

                when {
                    !toRouteName.startsWith("DetailRouteNav") &&
                            !fromRouteName.startsWith("DetailRouteNav") -> horizontalAnimation

                    toRouteName.startsWith("DetailRouteNav") &&
                            !fromRouteName.startsWith("DetailRouteNav") -> verticalUpAnimation

                    fromRouteName.startsWith("DetailRouteNav") -> verticalDownAnimation
                    else -> horizontalAnimation
                }
            },
            popTransitionSpec = {
                val horizontalAnimation = (slideInHorizontally(
                    animationSpec = tween(300)
                ) { -it } + fadeIn(animationSpec = tween(300))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(300)
                        ) { it } + fadeOut(animationSpec = tween(300)))
                val verticalDownAnimation = (slideInVertically(
                    animationSpec = tween(300)
                ) { -it } + fadeIn(animationSpec = tween(300))) togetherWith
                        (slideOutVertically(
                            animationSpec = tween(300)
                        ) { it } + fadeOut(animationSpec = tween(300)))

                val fromRouteName = initialState.key.toString()

                when {
                    fromRouteName.startsWith("DetailRouteNav") -> verticalDownAnimation
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AdMobBanner()

                val cs = MaterialTheme.colorScheme
                Row(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(cs.surfaceVariant)
                        .border(
                            width = 1.dp,
                            color = cs.outline,
                            shape = RoundedCornerShape(999.dp)
                        ),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isMapActive = currentRoute is HomeRouteNav
                    NavItem(
                        icon = Icons.Default.Home,
                        label = stringResource(R.string.nav_map),
                        isActive = isMapActive,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val existingHomeIndex = backStack.indexOfFirst { it is HomeRouteNav }
                            if (existingHomeIndex != -1 && existingHomeIndex != backStack.lastIndex) {
                                while (backStack.lastIndex > existingHomeIndex) {
                                    backStack.removeLastOrNull()
                                }
                            }
                        }
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(cs.outline)
                    )

                    val isListActive = currentRoute is ExploreRouteNav
                    NavItem(
                        icon = Icons.Default.Place,
                        label = stringResource(R.string.nav_list),
                        isActive = isListActive,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            CameraStateHolder.isNavigatingToDetail = true
                            val existingExploreIndex =
                                backStack.indexOfFirst { it is ExploreRouteNav }
                            if (existingExploreIndex != -1 && existingExploreIndex != backStack.lastIndex) {
                                while (backStack.lastIndex > existingExploreIndex) {
                                    backStack.removeLastOrNull()
                                }
                            } else if (existingExploreIndex == -1) {
                                backStack.add(ExploreRouteNav)
                            }
                        }
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(cs.outline)
                    )

                    val isFavoritesActive = currentRoute is FavoritesRouteNav
                    NavItem(
                        icon = Icons.Default.Favorite,
                        label = stringResource(R.string.favorites),
                        isActive = isFavoritesActive,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val existingFavoritesIndex =
                                backStack.indexOfFirst { it is FavoritesRouteNav }
                            if (existingFavoritesIndex != -1 && existingFavoritesIndex != backStack.lastIndex) {
                                while (backStack.lastIndex > existingFavoritesIndex) {
                                    backStack.removeLastOrNull()
                                }
                            } else if (existingFavoritesIndex == -1) {
                                backStack.add(FavoritesRouteNav)
                            }
                        }
                    )
                }
            }
        }
    }
}

