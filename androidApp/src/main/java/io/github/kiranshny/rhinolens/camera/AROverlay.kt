package io.github.kiranshny.rhinolens.camera

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import io.github.kiranshny.rhinolens.shared.domain.TranslatedBlock

@Composable
fun AROverlay(
    blocks: List<TranslatedBlock>,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        blocks.forEach { block ->
            val bbox = block.source.bbox
            val left = bbox.left * size.width
            val top = bbox.top * size.height
            val width = (bbox.right - bbox.left) * size.width
            val height = (bbox.bottom - bbox.top) * size.height
            if (width <= 1f || height <= 1f) return@forEach

            drawRoundRect(
                color = Color.Black.copy(alpha = 0.85f),
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = CornerRadius(4f, 4f),
            )

            val display = block.translated.ifBlank { block.source.text }
            val fontSizeSp = with(density) { (height * 0.55f).toSp() }
            val style = TextStyle(
                color = Color.White,
                fontSize = fontSizeSp,
            )
            val measured = measurer.measure(
                text = AnnotatedString(display),
                style = style,
                constraints = Constraints(
                    maxWidth = width.toInt().coerceAtLeast(1),
                    maxHeight = height.toInt().coerceAtLeast(1),
                ),
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
            )
            val textY = top + ((height - measured.size.height) / 2f).coerceAtLeast(0f)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(left + 6f, textY),
            )
        }
    }
}
