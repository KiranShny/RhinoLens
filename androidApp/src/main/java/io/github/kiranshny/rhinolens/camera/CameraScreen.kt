package io.github.kiranshny.rhinolens.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import io.github.kiranshny.rhinolens.appContainer
import io.github.kiranshny.rhinolens.nav.RhinoLensRoute
import io.github.kiranshny.rhinolens.shared.domain.Language

private enum class LanguagePickerTarget { SOURCE, TARGET }

@Composable
fun CameraScreen(navController: NavHostController) {
    val context = LocalContext.current
    val container = context.appContainer
    val viewModel: CameraViewModel = viewModel(
        factory = CameraViewModel.factory(container, context.applicationContext),
    )

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasPermission) {
        CameraContent(
            viewModel = viewModel,
            onBack = { navController.popBackStack() },
            onOpenLibrary = { navController.navigate(RhinoLensRoute.Library.path) },
            onOpenCapture = { id ->
                navController.navigate(RhinoLensRoute.CaptureDetail.build(id))
            },
        )
    } else {
        CameraPermissionDenied(
            onRetry = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onBack = { navController.popBackStack() },
        )
    }
}

@Composable
private fun CameraContent(
    viewModel: CameraViewModel,
    onBack: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenCapture: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val blocks by viewModel.translatedBlocks.collectAsStateWithLifecycle()
    val pair by viewModel.pair.collectAsStateWithLifecycle()
    var pickerTarget by remember { mutableStateOf<LanguagePickerTarget?>(null) }

    LaunchedEffect(lifecycleOwner) {
        viewModel.bindCamera(lifecycleOwner)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    controller = viewModel.cameraController
                }
            },
        )
        AROverlay(blocks = blocks, modifier = Modifier.fillMaxSize())

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
            )
        }

        CameraBottomToolbar(
            source = pair.source,
            target = pair.target,
            onSourceClick = { pickerTarget = LanguagePickerTarget.SOURCE },
            onTargetClick = { pickerTarget = LanguagePickerTarget.TARGET },
            onSwap = viewModel::swap,
            onCapture = { viewModel.capture(onSaved = onOpenCapture) },
            onLibraryClick = onOpenLibrary,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    when (pickerTarget) {
        LanguagePickerTarget.SOURCE -> LanguagePickerSheet(
            title = "Source language",
            allowAuto = true,
            selected = pair.source,
            onSelect = { lang: Language? ->
                viewModel.setSource(lang)
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null },
        )
        LanguagePickerTarget.TARGET -> LanguagePickerSheet(
            title = "Target language",
            allowAuto = false,
            selected = pair.target,
            onSelect = { lang: Language? ->
                if (lang != null) viewModel.setTarget(lang)
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null },
        )
        null -> Unit
    }
}

@Composable
private fun CameraPermissionDenied(
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Camera permission required",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "RhinoLens needs camera access to translate text in real time.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onRetry) { Text("Grant permission") }
            Button(onClick = onBack) { Text("Back") }
        }
    }
}
