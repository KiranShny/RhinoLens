package io.github.kiranshny.rhinolens.ui.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.kiranshny.rhinolens.nav.RhinoLensRoute

private data class DrawerEntry(
    val label: String,
    val route: String,
    val icon: ImageVector,
)

private val ENTRIES = listOf(
    DrawerEntry("Home", RhinoLensRoute.Home.path, Icons.Filled.Home),
    DrawerEntry("Library", RhinoLensRoute.Library.path, Icons.Filled.Collections),
    DrawerEntry("Settings", RhinoLensRoute.Settings.path, Icons.Filled.Settings),
)

@Composable
fun RhinoLensDrawerContent(
    currentRoute: String,
    onNavigate: (String) -> Unit,
) {
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "RhinoLens",
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
    )
    Spacer(modifier = Modifier.height(8.dp))
    ENTRIES.forEach { entry ->
        NavigationDrawerItem(
            icon = { Icon(entry.icon, contentDescription = null) },
            label = { Text(entry.label) },
            selected = entry.route == currentRoute,
            onClick = { onNavigate(entry.route) },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    NavigationDrawerItem(
        icon = { Icon(Icons.Filled.Info, contentDescription = null) },
        label = { Text("About") },
        selected = false,
        onClick = { },
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
    )
}
