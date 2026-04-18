package com.burixer85.piscinamap.core.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PiscinaMapBottomBar() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Explorar", "Favoritos", "Ajustes")
    val icons = listOf(Icons.Default.Place, Icons.Default.FavoriteBorder, Icons.Default.Settings)

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(icons[index], contentDescription = item) },
                label = { Text(item) },
                selected = selectedItem == index,
                onClick = { selectedItem = index },
                //colors = NavigationBarItemDefaults.colors(
                //    selectedIconColor = Color(0xFF006064),
                //    selectedLabelColor = Color(0xFF006064),
                //    indicatorColor = Color(0xFFE0F7FA),
                //    unselectedIconColor = Color.Gray,
                //    unselectedLabelColor = Color.Gray
                //)
            )
        }
    }
}