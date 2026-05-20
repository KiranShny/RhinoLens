package io.github.kiranshny.rhinolens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import io.github.kiranshny.rhinolens.nav.RhinoLensNavHost
import io.github.kiranshny.rhinolens.shared.domain.ThemeMode
import io.github.kiranshny.rhinolens.ui.theme.RhinoLensTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as RhinoLensApp).container.settingsRepository
        setContent {
            val theme by settings.theme.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val dynamicColor by settings.dynamicColor.collectAsStateWithLifecycle(initialValue = true)
            RhinoLensTheme(
                themeMode = theme,
                dynamicColor = dynamicColor,
            ) {
                val navController = rememberNavController()
                RhinoLensNavHost(navController = navController)
            }
        }
    }
}
