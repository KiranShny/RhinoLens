package io.github.kiranshny.rhinolens.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.github.kiranshny.rhinolens.shared.domain.NormalizedRect
import io.github.kiranshny.rhinolens.shared.domain.OcrFrame
import io.github.kiranshny.rhinolens.shared.domain.TextBlock
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.tasks.await

private const val TARGET_FPS = 12
private const val FRAME_INTERVAL_MS = 1000L / TARGET_FPS

class CameraXOcrSource(
    private val context: Context,
) {

    private val recognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val executor = Executors.newSingleThreadExecutor()
    private val _frames = MutableSharedFlow<OcrFrame>(replay = 0, extraBufferCapacity = 1)
    val frames: SharedFlow<OcrFrame> = _frames.asSharedFlow()

    private val frameCounter = AtomicLong(0L)
    private var lastProcessedMs = 0L

    var imageCapture: ImageCapture? = null
        private set

    suspend fun bind(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    ) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).await()
        cameraProvider.unbindAll()

        val preview = Preview.Builder().build().apply {
            surfaceProvider = previewView.surfaceProvider
        }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply { setAnalyzer(executor, ::analyze) }
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        imageCapture = capture

        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            analysis,
            capture,
        )
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun analyze(proxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastProcessedMs < FRAME_INTERVAL_MS) {
            proxy.close()
            return
        }
        lastProcessedMs = now

        val media = proxy.image
        if (media == null) {
            proxy.close()
            return
        }

        val rotation = proxy.imageInfo.rotationDegrees
        val portrait = rotation == 90 || rotation == 270
        val frameWidth = if (portrait) proxy.height else proxy.width
        val frameHeight = if (portrait) proxy.width else proxy.height

        val input = InputImage.fromMediaImage(media, rotation)
        recognizer.process(input)
            .addOnSuccessListener { text ->
                val blocks = text.toTextBlocks(frameWidth, frameHeight)
                _frames.tryEmit(
                    OcrFrame(
                        frameId = frameCounter.incrementAndGet(),
                        timestampMs = now,
                        blocks = blocks,
                    ),
                )
            }
            .addOnCompleteListener { proxy.close() }
    }

    fun close() {
        runCatching { recognizer.close() }
        runCatching { executor.shutdown() }
    }
}

private fun Text.toTextBlocks(width: Int, height: Int): List<TextBlock> {
    if (width <= 0 || height <= 0) return emptyList()
    val w = width.toFloat()
    val h = height.toFloat()
    return blocks.flatMap { block ->
        block.lines.mapNotNull { line ->
            val rect = line.boundingBox ?: return@mapNotNull null
            val cx = (rect.left + rect.right) / 2
            val cy = (rect.top + rect.bottom) / 2
            TextBlock(
                id = idFor(line.text, cx, cy),
                text = line.text,
                bbox = NormalizedRect(
                    left = (rect.left / w).coerceIn(0f, 1f),
                    top = (rect.top / h).coerceIn(0f, 1f),
                    right = (rect.right / w).coerceIn(0f, 1f),
                    bottom = (rect.bottom / h).coerceIn(0f, 1f),
                ),
                rotationDeg = line.angle,
                confidence = line.confidence ?: 1f,
            )
        }
    }
}

private fun idFor(text: String, centerX: Int, centerY: Int): String {
    val bucketX = centerX / 64
    val bucketY = centerY / 64
    val textHash = text.take(24).hashCode()
    return "$bucketX:$bucketY:$textHash"
}
