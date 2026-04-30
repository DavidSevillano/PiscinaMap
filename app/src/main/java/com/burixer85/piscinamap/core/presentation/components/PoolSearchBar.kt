package com.burixer85.piscinamap.core.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.burixer85.piscinamap.R
import com.google.android.libraries.places.api.model.AutocompletePrediction

@Composable
fun PoolSearchBar(
    searchText: String,
    predictions: List<AutocompletePrediction>,
    onSearchTextChange: (String) -> Unit,
    onPredictionClick: (AutocompletePrediction) -> Unit,
    onClearSearch: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.poolmark),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PiscinaMap",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color(0xFF1A2F4F)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { onFocusChanged(it.isFocused) },
                placeholder = { Text("Buscar piscinas o zonas...", color = Color.Gray) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Borrar",
                                tint = Color.Gray
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF7F8FA),
                    unfocusedContainerColor = Color(0xFFF7F8FA),
                    disabledContainerColor = Color(0xFFF7F8FA),
                    focusedBorderColor = Color(0xFFE0E6ED),
                    unfocusedBorderColor = Color(0xFFE0E6ED),
                    disabledBorderColor = Color(0xFFE0E6ED),
                    focusedTextColor = Color(0xFF1D293F),
                    unfocusedTextColor = Color(0xFF1D293F),
                    cursorColor = Color(0xFF1A2F4F)
                )
            )

            if (predictions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp)
                ) {
                    items(predictions) { prediction ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPredictionClick(prediction) }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Text(
                                text = prediction.getPrimaryText(null).toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF1A2F4F)
                            )
                            Text(
                                text = prediction.getSecondaryText(null).toString(),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE0E6ED))
                    }
                }
            }
        }
    }
}