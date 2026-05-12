package com.burixer85.piscinamap.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.burixer85.piscinamap.R
import com.burixer85.piscinamap.core.domain.model.Pool
import com.burixer85.piscinamap.core.presentation.util.PoolUtils.getGooglePhotoUrl
import com.burixer85.piscinamap.ui.theme.AbyssalStar
import com.burixer85.piscinamap.ui.theme.AbyssalTextMute
import com.burixer85.piscinamap.ui.theme.DaylightStar
import com.burixer85.piscinamap.ui.theme.DaylightTextMute

@Composable
fun PoolListCard(
    pool: Pool,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val star = if (isDark) AbyssalStar else DaylightStar
    val textMute = if (isDark) AbyssalTextMute else DaylightTextMute

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cs.surfaceVariant)
            .border(1.dp, cs.outline, RoundedCornerShape(18.dp))
            .clickable { onNavigateToDetail(pool.id) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(cs.surface)
        ) {
            if (!pool.photoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(getGooglePhotoUrl(pool.photoUrl))
                        .crossfade(true)
                        .build(),
                    contentDescription = pool.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(R.drawable.ic_pool_placeholder)
                        .build(),
                    contentDescription = stringResource(R.string.no_photo_available),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = pool.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = (-0.01).sp,
                color = cs.onSurface,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(5.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = star,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = pool.rating?.toString() ?: "N/A",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface
                )
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(textMute)
                )
                val statusText = when (pool.isOpenNow) {
                    true -> stringResource(R.string.open)
                    false -> stringResource(R.string.closed)
                    null -> stringResource(R.string.no_schedule_dot)
                }
                val statusColor = when (pool.isOpenNow) {
                    true -> cs.tertiary
                    false -> cs.error
                    null -> textMute
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(statusColor)
                    )
                    Text(
                        text = statusText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }
            }
        }
    }
}
