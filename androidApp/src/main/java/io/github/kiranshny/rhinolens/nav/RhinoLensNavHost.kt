package io.github.kiranshny.rhinolens.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import io.github.kiranshny.rhinolens.capturedetail.CaptureDetailScreen
import io.github.kiranshny.rhinolens.camera.CameraScreen
import io.github.kiranshny.rhinolens.home.HomeScreen
import io.github.kiranshny.rhinolens.importimg.ImportImageScreen
import io.github.kiranshny.rhinolens.library.LibraryScreen
import io.github.kiranshny.rhinolens.settings.SettingsScreen

@Composable
fun RhinoLensNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = RhinoLensRoute.Home.path,
        modifier = modifier,
    ) {
        composable(RhinoLensRoute.Home.path) { HomeScreen(navController) }
        composable(RhinoLensRoute.Camera.path) { CameraScreen(navController) }
        composable(RhinoLensRoute.Library.path) { LibraryScreen(navController) }
        composable(RhinoLensRoute.ImportImage.path) { ImportImageScreen(navController) }
        composable(RhinoLensRoute.Settings.path) { SettingsScreen(navController) }
        composable(
            route = RhinoLensRoute.CaptureDetail.path,
            arguments = listOf(
                navArgument(RhinoLensRoute.CaptureDetail.ID_ARG) { type = NavType.StringType },
            ),
        ) { entry ->
            val id = entry.arguments?.getString(RhinoLensRoute.CaptureDetail.ID_ARG).orEmpty()
            CaptureDetailScreen(navController = navController, captureId = id)
        }
    }
}
