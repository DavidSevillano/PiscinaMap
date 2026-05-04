package com.burixer85.piscinamap.features.explore.presentation

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.google.android.gms.maps.model.LatLng

private const val USE_MOCK_LOCATION = true
private val MOCK_LOCATION = LatLng(30.2672, -97.7431) // Austin, Texas

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ExploreScreen(
    onNavigateToDetail: (String) -> Unit = {},
    bottomPadding: Int = 0,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var adLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val nativeId =
            if (BuildConfig.USE_TEST_ADS) "ca-app-pub-3940256099942544/2247696110" else com.burixer85.piscinamap.BuildConfig.ADMOB_NATIVE_ID
        val adLoader = AdLoader.Builder(context, nativeId)
            .forNativeAd { ad ->
                nativeAd = ad
                adLoaded = true
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {}
                override fun onAdLoaded() {}
            })
            .build()
        adLoader.loadAds(AdRequest.Builder().build(), 1)
    }

    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    var initialLocationObtained by remember { mutableStateOf(false) }

    @SuppressLint("MissingPermission")
    LaunchedEffect(locationPermissionState.status.isGranted, uiState.pools.isEmpty()) {
        if (USE_MOCK_LOCATION && !initialLocationObtained && uiState.pools.isEmpty()) {
            viewModel.fetchPools(MOCK_LOCATION.latitude, MOCK_LOCATION.longitude)
            initialLocationObtained = true
        } else if (locationPermissionState.status.isGranted && !initialLocationObtained && uiState.pools.isEmpty()) {
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
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding(),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp,
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.poolmark),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = getString(context, R.string.app_name),
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
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

                        if (visiblePools.size >= 2) {
                            items(2) { index ->
                                PoolListCard(
                                    pool = visiblePools[index],
                                    onNavigateToDetail = onNavigateToDetail
                                )
                            }
                            item(key = "nativeAd") {
                                NativeAdCard(nativeAd = nativeAd)
                            }
                            items(visiblePools.size - 2) { index ->
                                PoolListCard(
                                    pool = visiblePools[index + 2],
                                    onNavigateToDetail = onNavigateToDetail
                                )
                            }
                        } else {
                            items(visiblePools) { pool ->
                                PoolListCard(
                                    pool = pool,
                                    onNavigateToDetail = onNavigateToDetail
                                )
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
}