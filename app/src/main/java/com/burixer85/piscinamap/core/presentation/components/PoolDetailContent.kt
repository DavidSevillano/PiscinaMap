package com.burixer85.piscinamap.core.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalPhone
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.burixer85.piscinamap.R
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.presentation.util.LocaleHelper.getString
import com.burixer85.piscinamap.core.presentation.util.PoolUtils
import com.burixer85.piscinamap.core.presentation.util.PoolUtils.getGooglePhotoUrl
import com.burixer85.piscinamap.ui.theme.AbyssalStar
import com.burixer85.piscinamap.ui.theme.AbyssalTextMute
import com.burixer85.piscinamap.ui.theme.DaylightStar
import com.burixer85.piscinamap.ui.theme.DaylightTextMute


@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun PoolDetailContent(
    pool: Pool,
    onCallClick: (String) -> Unit,
    onBack: () -> Unit,
    onMoreClick: () -> Unit,
    onHideClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val star = if (isDark) AbyssalStar else DaylightStar
    val textMute = if (isDark) AbyssalTextMute else DaylightTextMute

    val scrollState = rememberScrollState()
    val photoUrls = pool.photoUrls.ifEmpty { listOf(null) }
    val currentDayIndex = PoolUtils.getCurrentDayIndex()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        val pagerState = rememberPagerState(pageCount = { photoUrls.size })
        val hasMultiplePhotos = photoUrls.size > 1

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val photoUrl = photoUrls.getOrNull(page)
                AsyncImage(
                    model = photoUrl?.let { getGooglePhotoUrl(it) }
                        ?: R.drawable.ic_pool_placeholder,
                    contentDescription = getString(context, R.string.pool_photo, pool.name),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Hero veil: dark top → transparent → bg at bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0x59000000),
                            0.30f to Color.Transparent,
                            0.60f to Color.Transparent,
                            1f to cs.background
                        )
                    )
            )

            if (hasMultiplePhotos) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 38.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    repeat(photoUrls.size) { index ->
                        val isActive = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isActive) 18.dp else 6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (isActive) Color.White.copy(alpha = 0.95f)
                                    else Color.White.copy(alpha = 0.35f)
                                )
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBtn(icon = Icons.AutoMirrored.Filled.ArrowBack, size = 20, onClick = onBack)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconBtn(icon = Icons.Default.Share, size = 18, onClick = {
                        val mapsUrl = "https://www.google.com/maps/search/?api=1&query=${pool.latitude},${pool.longitude}"
                        val shareText = "${pool.name}\n${pool.address}\n$mapsUrl"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, getString(context, R.string.share_pool)))
                    })
                    IconBtn(icon = Icons.Default.MoreVert, size = 18, onClick = onMoreClick)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-12).dp)
                .padding(horizontal = 20.dp)
        ) {
            val statusText = when (pool.isOpenNow) {
                true -> getString(context, R.string.open)
                false -> getString(context, R.string.closed)
                null -> getString(context, R.string.no_schedule_dot)
            }
            val statusColor = when (pool.isOpenNow) {
                true -> cs.tertiary
                false -> cs.error
                null -> textMute
            }
            DetailStatusPill(text = statusText, color = statusColor)

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = pool.name,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                letterSpacing = (-0.025).sp,
                lineHeight = 30.sp,
                color = cs.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (pool.rating != null) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = star,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = pool.rating.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface
                    )
                    if (pool.reviews.isNotEmpty()) {
                        Text(
                            text = "(${pool.reviews.size} ${getString(context, R.string.reviews).lowercase()})",
                            fontSize = 14.sp,
                            color = cs.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(textMute)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = textMute,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = pool.address,
                    fontSize = 14.sp,
                    color = cs.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(cs.primary)
                        .clickable {
                            val uri = Uri.parse(
                                "https://www.google.com/maps/dir/?api=1&destination=${pool.latitude},${pool.longitude}"
                            )
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = null,
                            tint = cs.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = getString(context, R.string.get_directions),
                            color = cs.onPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            letterSpacing = (-0.005).sp
                        )
                    }
                }

                if (pool.formattedPhone != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(cs.surface)
                            .border(1.dp, cs.outlineVariant, RoundedCornerShape(16.dp))
                            .clickable { onCallClick(pool.formattedPhone) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalPhone,
                                contentDescription = null,
                                tint = cs.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = getString(context, R.string.call),
                                color = cs.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                letterSpacing = (-0.005).sp
                            )
                        }
                    }
                }
            }

            if (pool.openingHours.isNotEmpty() || pool.currentOpeningHours != null) {
                Spacer(modifier = Modifier.height(22.dp))

                var showAllHours by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(cs.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = cs.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = getString(context, R.string.schedule_today),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = (-0.01).sp,
                        color = cs.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (pool.openingHours.size > 1) {
                        Icon(
                            imageVector = if (showAllHours) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = textMute,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { showAllHours = !showAllHours }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (pool.currentOpeningHours != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(cs.surface)
                            .border(1.dp, cs.outline, RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DetailStatusPill(
                                text = if (pool.isOpenNow == true) getString(context, R.string.open)
                                else getString(context, R.string.closed),
                                color = if (pool.isOpenNow == true) cs.tertiary else cs.error
                            )
                            Text(
                                text = pool.currentOpeningHours,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = cs.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = textMute,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (showAllHours && pool.openingHours.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        pool.openingHours.forEachIndexed { index, hour ->
                            if (index != currentDayIndex) {
                                Text(
                                    text = hour,
                                    fontSize = 14.sp,
                                    color = cs.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (pool.services.isNotEmpty()) {
                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(cs.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Waves,
                            contentDescription = null,
                            tint = cs.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = getString(context, R.string.services),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = (-0.01).sp,
                        color = cs.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pool.services.forEach { service ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(cs.surface)
                                .border(1.dp, cs.outline, RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = service,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = cs.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (pool.reviews.isNotEmpty()) {
                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(cs.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = cs.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "${getString(context, R.string.reviews)} · ${pool.rating ?: ""} · (${pool.reviews.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = (-0.01).sp,
                        color = cs.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                var selectedFilter by remember { mutableStateOf("best") }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == "best",
                            onClick = { selectedFilter = "best" },
                            label = { Text(getString(context, R.string.best)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "worst",
                            onClick = { selectedFilter = "worst" },
                            label = { Text(getString(context, R.string.worst)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "recent",
                            onClick = { selectedFilter = "recent" },
                            label = { Text(getString(context, R.string.recent)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == "oldest",
                            onClick = { selectedFilter = "oldest" },
                            label = { Text(getString(context, R.string.oldest)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val sortedReviews = remember(pool.reviews, selectedFilter) {
                    when (selectedFilter) {
                        "recent" -> pool.reviews.sortedByDescending { it.publishedAt }
                        "oldest" -> pool.reviews.sortedBy { it.publishedAt }
                        "best" -> pool.reviews.sortedByDescending { it.rating }
                        "worst" -> pool.reviews.sortedBy { it.rating }
                        else -> pool.reviews
                    }.take(5)
                }

                sortedReviews.forEach { review ->
                    ReviewCard(
                        authorName = review.authorName,
                        rating = review.rating,
                        text = review.text,
                        relativeTime = review.relativeTimeDescription,
                        language = review.language,
                        profilePhotoUrl = review.profilePhotoUrl,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun IconBtn(
    icon: ImageVector,
    size: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x66000000))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size.dp)
        )
    }
}

@Composable
private fun DetailStatusPill(text: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color)
        )
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.01.sp
        )
    }
}
