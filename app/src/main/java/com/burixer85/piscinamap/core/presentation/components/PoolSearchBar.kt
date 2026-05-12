package com.burixer85.piscinamap.core.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.burixer85.piscinamap.R
import com.burixer85.piscinamap.core.presentation.util.LocaleHelper.getString
import com.google.android.libraries.places.api.model.AutocompletePrediction

@Composable
fun PoolSearchBar(
    searchText: String,
    predictions: List<AutocompletePrediction>,
    onSearchTextChange: (String) -> Unit,
    onPredictionClick: (AutocompletePrediction) -> Unit,
    onClearSearch: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 26.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
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
                    letterSpacing = (-0.01).sp,
                    color = cs.onSurface
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(cs.surfaceVariant)
                .border(width = 1.dp, color = cs.outline, shape = RoundedCornerShape(999.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { onFocusChanged(it.isFocused) },
                textStyle = TextStyle(
                    color = cs.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(cs.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                decorationBox = { inner ->
                    if (searchText.isEmpty()) {
                        Text(
                            text = getString(context, R.string.search_pools_or_areas),
                            color = cs.onSurfaceVariant,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    inner()
                }
            )
            if (searchText.isNotEmpty()) {
                IconButton(
                    onClick = onClearSearch,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = getString(context, R.string.clear),
                        tint = cs.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (predictions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .heightIn(max = 250.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(cs.surface)
                    .border(1.dp, cs.outline, RoundedCornerShape(18.dp))
            ) {
                items(predictions) { prediction ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPredictionClick(prediction) }
                            .padding(vertical = 12.dp, horizontal = 14.dp)
                    ) {
                        val primaryText = prediction.getPrimaryText(null)
                        val secondaryText = prediction.getSecondaryText(null)
                        if (primaryText != null) {
                            Text(
                                text = primaryText.toString(),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = cs.onSurface
                            )
                        }
                        if (secondaryText != null) {
                            Text(
                                text = secondaryText.toString(),
                                fontSize = 12.sp,
                                color = cs.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = cs.outline)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
