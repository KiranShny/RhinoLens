@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.kiranshny.rhinolens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.github.kiranshny.rhinolens.nav.RhinoLensRoute
import io.github.kiranshny.rhinolens.ui.common.RhinoLensDrawerContent
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(navController: NavHostController) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    DismissibleNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DismissibleDrawerSheet {
                RhinoLensDrawerContent(
                    currentRoute = RhinoLensRoute.Home.path,
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        if (route != RhinoLensRoute.Home.path) {
                            navController.navigate(route)
                        }
                    },
                )
            }
        },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("RhinoLens") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open drawer")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
        ) { padding ->
            HomeContent(
                padding = padding,
                onStartScanning = { navController.navigate(RhinoLensRoute.Camera.path) },
                onImportImage = { navController.navigate(RhinoLensRoute.ImportImage.path) },
                onSeeAllCaptures = { navController.navigate(RhinoLensRoute.Library.path) },
            )
        }
    }
}

@Composable
private fun HomeContent(
    padding: PaddingValues,
    onStartScanning: () -> Unit,
    onImportImage: () -> Unit,
    onSeeAllCaptures: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Live translation",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Point your camera at foreign text and see it translated in place.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = onStartScanning,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = "Start scanning",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        OutlinedButton(
            onClick = onImportImage,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Image,
                contentDescription = null,
            )
            Text(
                text = "Import image",
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        OutlinedButton(
            onClick = onSeeAllCaptures,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("View captures")
        }
    }
}
