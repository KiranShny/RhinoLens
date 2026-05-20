package io.github.kiranshny.rhinolens.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.kiranshny.rhinolens.shared.domain.Language

@Composable
fun CameraBottomToolbar(
    source: Language?,
    target: Language,
    onSourceClick: () -> Unit,
    onTargetClick: () -> Unit,
    onSwap: () -> Unit,
    onCapture: () -> Unit,
    onLibraryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LangChip(
                label = source?.displayName ?: "Auto",
                onClick = onSourceClick,
            )
            IconButton(onClick = onSwap, enabled = source != null) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = "Swap languages",
                    tint = Color.White,
                )
            }
            LangChip(
                label = target.displayName,
                onClick = onTargetClick,
            )
            CaptureButton(onCapture = onCapture)
            LibraryButton(onClick = onLibraryClick)
        }
    }
}

@Composable
private fun RowScope.LangChip(label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, color = Color.White) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = Color.White.copy(alpha = 0.15f),
        ),
        border = AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = Color.White.copy(alpha = 0.4f),
        ),
        modifier = Modifier.padding(horizontal = 2.dp),
    )
}

@Composable
private fun CaptureButton(onCapture: () -> Unit) {
    FilledIconButton(
        onClick = onCapture,
        modifier = Modifier.size(64.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = Color.White,
            contentColor = Color.Black,
        ),
    ) {
        Icon(
            imageVector = Icons.Filled.PhotoCamera,
            contentDescription = "Capture",
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
private fun LibraryButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.15f),
        modifier = Modifier
            .size(48.dp)
            .background(Color.Transparent),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(text = "▣", color = Color.White)
        }
    }
}
