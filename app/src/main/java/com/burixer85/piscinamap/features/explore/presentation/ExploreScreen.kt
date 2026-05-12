package com.burixer85.piscinamap.features.explore.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burixer85.piscinamap.BuildConfig
import com.burixer85.piscinamap.R
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.presentation.components.ExitConfirmationDialog
import com.burixer85.piscinamap.core.presentation.components.NativeAdCard
import com.burixer85.piscinamap.core.presentation.components.PoolListCard
import com.burixer85.piscinamap.core.presentation.util.HiddenPoolsManager
import com.burixer85.piscinamap.core.presentation.util.LocaleHelper.getString
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ExploreScreen(
    onNavigateToDetail: (String) -> Unit = {},
    bottomPadding: Int = 0,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()


    var nativeAdList by remember { mutableStateOf<List<NativeAd>>(emptyList()) }
    var locationPermissionDecided by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        showExitConfirmation = true
    }

    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    LaunchedEffect(locationPermissionState.status) {
        delay(500)
        locationPermissionDecided = true
    }

    val nativeId = if (BuildConfig.USE_TEST_ADS) {
        "ca-app-pub-3940256099942544/2247696110"
    } else {
        BuildConfig.ADMOB_NATIVE_ID
    }

    LaunchedEffect(uiState.pools, locationPermissionDecided) {
        if (uiState.pools.isEmpty()) return@LaunchedEffect
        if (nativeAdList.isNotEmpty()) return@LaunchedEffect
        if (!locationPermissionDecided) return@LaunchedEffect

        try {
            val adLoader = AdLoader.Builder(context, nativeId)
                .forNativeAd { ad ->
                    nativeAdList = nativeAdList + ad
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                    }
                })
                .build()

            adLoader.loadAds(AdRequest.Builder().build(), 5)
        } catch (e: Exception) {
        }
    }

    var initialLocationObtained by remember { mutableStateOf(false) }

    @SuppressLint("MissingPermission")
    LaunchedEffect(locationPermissionState.status.isGranted, uiState.pools.isEmpty()) {
        if (locationPermissionState.status.isGranted && !initialLocationObtained && uiState.pools.isEmpty()) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    viewModel.fetchPools(it.latitude, it.longitude)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            uiState.error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            uiState.pools.isEmpty() -> {
                Text(
                    text = stringResource(R.string.no_pools_in_this_area),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 18.dp, end = 18.dp, top = 32.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.poolmark),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = getString(context, R.string.app_name),
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.01f).sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = (16 + bottomPadding).dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val visiblePools = uiState.pools.filter { pool ->
                            !HiddenPoolsManager.isHidden(context, pool.id)
                        }

                        var totalPoolsAdShown = 0

                        val itemList = buildList {
                            var totalPools = 0
                            visiblePools.forEachIndexed { index, pool ->
                                add(pool)
                                totalPools++

                                val adPosition = 2 + (totalPoolsAdShown * 4)
                                if (totalPools == adPosition && totalPools >= 2) {
                                    add(totalPoolsAdShown)
                                    totalPoolsAdShown++
                                }
                            }
                        }

                        items(itemList.size) { index ->
                            val item = itemList[index]
                            when (item) {
                                is Pool -> {
                                    PoolListCard(
                                        pool = item,
                                        onNavigateToDetail = onNavigateToDetail
                                    )
                                }

                                is Int -> {
                                    val adIndex =
                                        if (nativeAdList.isNotEmpty()) item % nativeAdList.size else 0
                                    val adForPosition = nativeAdList.getOrNull(adIndex)
                                        ?: nativeAdList.firstOrNull()
                                    NativeAdCard(
                                        nativeAd = adForPosition,
                                        ctaText = stringResource(R.string.see_more)
                                    )
                                }
                            }
                        }

                        item {
                            if (!uiState.hasSearchedMore) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp, bottom = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.isLoadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Button(
                                            onClick = { viewModel.fetchMorePools() }
                                        ) {
                                            Text(stringResource(R.string.search_more))
                                        }
                                    }
                                }
                            } else if (uiState.warning != null) {
                                Text(
                                    text = uiState.warning!!,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExitConfirmation) {
        ExitConfirmationDialog(
            onConfirm = {
                showExitConfirmation = false
                (context as? Activity)?.finish()
            },
            onDismiss = { showExitConfirmation = false }
        )
    }
}