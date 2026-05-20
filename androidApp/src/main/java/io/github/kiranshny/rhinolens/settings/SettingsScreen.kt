@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.kiranshny.rhinolens.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import io.github.kiranshny.rhinolens.appContainer
import io.github.kiranshny.rhinolens.camera.LanguagePickerSheet
import io.github.kiranshny.rhinolens.shared.domain.DownloadedPack
import io.github.kiranshny.rhinolens.shared.domain.Language
import io.github.kiranshny.rhinolens.shared.domain.ThemeMode

@Composable
fun SettingsScreen(navController: NavHostController) {
    val container = LocalContext.current.appContainer
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))

    val target by viewModel.targetLanguage.collectAsStateWithLifecycle()
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val packs by viewModel.downloadedPacks.collectAsStateWithLifecycle()

    var showTargetPicker by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        SettingsContent(
            padding = padding,
            target = target,
            theme = theme,
            dynamicColor = dynamicColor,
            packs = packs,
            onOpenTargetPicker = { showTargetPicker = true },
            onSetTheme = viewModel::setTheme,
            onSetDynamicColor = viewModel::setDynamicColor,
            onDeletePack = { viewModel.deletePack(it.lang.code) },
            onClearHistory = { showClearConfirm = true },
        )
    }

    if (showTargetPicker) {
        LanguagePickerSheet(
            title = "Default target language",
            allowAuto = false,
            selected = target,
            onSelect = { lang ->
                if (lang != null) viewModel.setTargetLanguage(lang)
                showTargetPicker = false
            },
            onDismiss = { showTargetPicker = false },
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear capture history?") },
            text = { Text("This permanently deletes every saved capture and its image. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearConfirm = false
                    },
                ) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SettingsContent(
    padding: PaddingValues,
    target: Language,
    theme: ThemeMode,
    dynamicColor: Boolean,
    packs: List<DownloadedPack>,
    onOpenTargetPicker: () -> Unit,
    onSetTheme: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onDeletePack: (DownloadedPack) -> Unit,
    onClearHistory: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsSection("Languages") {
            ListItem(
                headlineContent = { Text("Default target language") },
                supportingContent = { Text("${target.displayName} (${target.nativeName})") },
                modifier = Modifier.clickable(onClick = onOpenTargetPicker),
            )
        }

        SettingsSection("Appearance") {
            ThemeMode.entries.forEach { mode ->
                ListItem(
                    headlineContent = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    leadingContent = {
                        RadioButton(
                            selected = mode == theme,
                            onClick = { onSetTheme(mode) },
                        )
                    },
                    modifier = Modifier.clickable { onSetTheme(mode) },
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    headlineContent = { Text("Dynamic color") },
                    supportingContent = { Text("Use system Material You palette") },
                    trailingContent = {
                        Switch(
                            checked = dynamicColor,
                            onCheckedChange = onSetDynamicColor,
                        )
                    },
                )
            }
        }

        SettingsSection("Language packs") {
            if (packs.isEmpty()) {
                ListItem(
                    headlineContent = { Text("No packs downloaded yet") },
                    supportingContent = { Text("Packs download on first use of a language pair.") },
                )
            } else {
                packs.forEach { pack ->
                    ListItem(
                        headlineContent = { Text(pack.lang.displayName) },
                        supportingContent = { Text("~${pack.sizeBytes / 1_000_000} MB") },
                        trailingContent = {
                            IconButton(onClick = { onDeletePack(pack) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete pack")
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }

        SettingsSection("Storage") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onClearHistory,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) { Text("Clear capture history") }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
    )
    content()
    HorizontalDivider(color = Color.Transparent)
}
