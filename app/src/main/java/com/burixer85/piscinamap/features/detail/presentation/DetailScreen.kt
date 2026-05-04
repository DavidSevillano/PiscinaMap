package com.burixer85.piscinamap.features.detail.presentation

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burixer85.piscinamap.R
import com.burixer85.piscinamap.core.presentation.components.PoolDetailContent
import com.burixer85.piscinamap.core.presentation.components.ShimmerPlaceholder
import com.burixer85.piscinamap.core.presentation.util.HiddenPoolsManager
import com.burixer85.piscinamap.core.presentation.util.LocaleHelper.getString
import com.burixer85.piscinamap.core.presentation.util.PoolStateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
        mutableStateOf(
            HiddenPoolsManager.isHidden(
                context,
                poolId
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (isHidden) {
                                DropdownMenuItem(
                                    text = { Text(getString(context, R.string.unhide_pool)) },
                                    onClick = {
                                        showMenu = false
                                        scope.launch {
                                            delay(100)
                                            HiddenPoolsManager.showPool(context, poolId)
                                            isHidden = false
                                            PoolStateManager.emitHiddenStateChange(poolId, false)
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Visibility, contentDescription = null)
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(getString(context, R.string.not_a_pool)) },
                                    onClick = {
                                        showMenu = false
                                        scope.launch {
                                            delay(100)
                                            HiddenPoolsManager.hidePool(context, poolId)
                                            isHidden = true
                                            PoolStateManager.emitHiddenStateChange(poolId, true)
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Error: ${uiState.error}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.retry() }) {
                            Text("Reintentar")
                        }
                    }
                }

                uiState.pool != null -> {
                    if (isHidden) {
                        Text(
                            text = getString(context, R.string.pool_hidden),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        val poolToShow = uiState.pool!!.copy(isHidden = isHidden)
                        PoolDetailContent(
                            pool = poolToShow,
                            onCallClick = { phone ->
                                val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                else -> {
                    ShimmerPlaceholder()
                }
            }
        }
    }
}