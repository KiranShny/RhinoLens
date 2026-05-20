@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.kiranshny.rhinolens.capturedetail

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import io.github.kiranshny.rhinolens.appContainer
import io.github.kiranshny.rhinolens.camera.AROverlay
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun CaptureDetailScreen(
    navController: NavHostController,
    captureId: String,
) {
    val context = LocalContext.current
    val container = context.appContainer
    val viewModel: CaptureDetailViewModel = viewModel(
        key = captureId,
        factory = CaptureDetailViewModel.factory(captureId, container, context.filesDir),
    )
    val capture by viewModel.capture.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capture") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val current = capture
                    IconButton(
                        onClick = {
                            if (current != null) {
                                shareCapture(context, viewModel.filesRoot, current.imagePath)
                            }
                        },
                        enabled = current != null,
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                    IconButton(
                        onClick = {
                            viewModel.delete { navController.popBackStack() }
                        },
                        enabled = current != null,
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black),
        ) {
            val current = capture
            if (current == null) {
                Text(
                    text = "Loading…",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                val file = File(viewModel.filesRoot, current.imagePath)
                AsyncImage(
                    model = file,
                    contentDescription = current.id,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                AROverlay(blocks = current.blocks, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

private fun shareCapture(
    context: android.content.Context,
    filesRoot: File,
    relativePath: String,
) {
    val file = File(filesRoot, relativePath)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.captures",
        file,
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share capture"))
}
