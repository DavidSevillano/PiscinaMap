package com.burixer85.piscinamap.features.detail.presentation

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burixer85.piscinamap.R
import com.burixer85.piscinamap.core.presentation.components.PoolDetailContent
import com.burixer85.piscinamap.core.presentation.components.ShimmerPlaceholder
import com.burixer85.piscinamap.core.presentation.util.FavoritesManager
import com.burixer85.piscinamap.core.presentation.util.HiddenPoolsManager
import com.burixer85.piscinamap.core.presentation.util.LocaleHelper.getString
import com.burixer85.piscinamap.core.presentation.util.PoolStateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    poolId: String,
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var isHidden by remember(poolId) {
        mutableStateOf(HiddenPoolsManager.isHidden(context, poolId))
    }
    var isFavorite by remember(poolId) {
        mutableStateOf(FavoritesManager.isFavorite(context, poolId))
    }

    DetailContent(
        uiState = uiState,
        isHidden = isHidden,
        isFavorite = isFavorite,
        showMenu = showMenu,
        onRetry = viewModel::retry,
        onCallClick = { phone ->
            val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
            context.startActivity(intent)
        },
        onBack = onBack,
        onMoreClick = { showMenu = true },
        onHidePool = {
            showMenu = false
            scope.launch {
                delay(100)
                HiddenPoolsManager.hidePool(context, poolId)
                isHidden = true
                PoolStateManager.emitHiddenStateChange(poolId, true)
                viewModel.onPoolHidden(poolId)
            }
        },
        onUnhidePool = {
            showMenu = false
            scope.launch {
                delay(100)
                HiddenPoolsManager.showPool(context, poolId)
                isHidden = false
                PoolStateManager.emitHiddenStateChange(poolId, false)
                viewModel.onPoolUnhidden(poolId)
            }
        },
        onDismissMenu = { showMenu = false },
        onFavoriteToggle = {
            val newState = !isFavorite
            if (newState) {
                FavoritesManager.addFavorite(context, poolId)
            } else {
                FavoritesManager.removeFavorite(context, poolId)
            }
            isFavorite = newState
            PoolStateManager.emitFavoriteStateChange(poolId, newState)
            viewModel.onFavoriteToggled(poolId, newState)
        },
    )
}

@Composable
internal fun DetailContent(
    uiState: DetailUiState,
    isHidden: Boolean = false,
    isFavorite: Boolean = false,
    showMenu: Boolean = false,
    onRetry: () -> Unit = {},
    onCallClick: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onHidePool: () -> Unit = {},
    onUnhidePool: () -> Unit = {},
    onDismissMenu: () -> Unit = {},
    onFavoriteToggle: () -> Unit = {},
) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.error_occurred, uiState.error ?: ""))
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRetry) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }

            uiState.pool != null -> {
                if (isHidden) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = getString(context, R.string.pool_hidden))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onUnhidePool) {
                            Text(text = getString(context, R.string.unhide_pool))
                        }
                    }
                } else {
                    PoolDetailContent(
                        pool = uiState.pool.copy(isHidden = isHidden),
                        onCallClick = onCallClick,
                        onBack = onBack,
                        onMoreClick = onMoreClick,
                        isFavorite = isFavorite,
                        onFavoriteToggle = onFavoriteToggle,
                    )
                }
            }

            else -> {
                ShimmerPlaceholder()
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 52.dp, end = 14.dp)
        ) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = onDismissMenu,
            ) {
                if (isHidden) {
                    DropdownMenuItem(
                        text = { Text(getString(context, R.string.unhide_pool)) },
                        onClick = onUnhidePool,
                        leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text(getString(context, R.string.not_a_pool)) },
                        onClick = onHidePool,
                        leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) }
                    )
                }
            }
        }
    }
}
