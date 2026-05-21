package io.github.kiranshny.rhinolens.importimg

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.github.kiranshny.rhinolens.AppContainer
import io.github.kiranshny.rhinolens.camera.toTextBlocks
import io.github.kiranshny.rhinolens.shared.domain.Capture
import io.github.kiranshny.rhinolens.shared.domain.Language
import io.github.kiranshny.rhinolens.shared.domain.LanguagePair
import io.github.kiranshny.rhinolens.shared.domain.Languages
import io.github.kiranshny.rhinolens.shared.domain.OcrFrame
import io.github.kiranshny.rhinolens.shared.domain.TranslatedBlock
import io.github.kiranshny.rhinolens.shared.orchestrator.TranslationOrchestrator
import io.github.kiranshny.rhinolens.shared.port.CaptureRepository
import io.github.kiranshny.rhinolens.shared.port.SettingsRepository
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

sealed interface ImportState {
    data object Empty : ImportState
    data class Reading(val uri: Uri) : ImportState
    data class Translating(val uri: Uri, val target: Language) : ImportState
    data class Ready(
        val uri: Uri,
        val blocks: List<TranslatedBlock>,
        val target: Language,
    ) : ImportState
    data class Failed(val message: String) : ImportState
    data class Saved(val captureId: String) : ImportState
}

class ImportImageViewModel(
    private val appContext: Context,
    private val orchestrator: TranslationOrchestrator,
    private val captures: CaptureRepository,
    private val settings: SettingsRepository,
    private val capturesDir: File,
) : ViewModel() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val _state = MutableStateFlow<ImportState>(ImportState.Empty)
    val state: StateFlow<ImportState> = _state.asStateFlow()

    fun process(uri: Uri) {
        _state.value = ImportState.Reading(uri)
        viewModelScope.launch {
            try {
                val target = settings.targetLanguage.first()
                _state.value = ImportState.Translating(uri, target)

                val input = withContext(Dispatchers.IO) {
                    InputImage.fromFilePath(appContext, uri)
                }
                val text = recognizer.process(input).await()
                val ocrBlocks = text.toTextBlocks(input.width, input.height)
                val frame = OcrFrame(
                    frameId = 0L,
                    timestampMs = System.currentTimeMillis(),
                    blocks = ocrBlocks,
                )
                val pairFlow = MutableStateFlow(LanguagePair(source = null, target = target))
                val translated = orchestrator
                    .translatedStream(flowOf(frame), pairFlow)
                    .first()
                _state.value = ImportState.Ready(uri, translated, target)
            } catch (cause: Exception) {
                _state.value = ImportState.Failed(cause.message ?: "Could not read image")
            }
        }
    }

    fun save() {
        val ready = _state.value as? ImportState.Ready ?: return
        viewModelScope.launch {
            try {
                val id = UUID.randomUUID().toString()
                val imageRelative = "captures/$id.jpg"
                val thumbRelative = "captures/thumbs/$id.jpg"
                val imageFile = File(capturesDir, "$id.jpg")
                val thumbFile = File(capturesDir, "thumbs/$id.jpg").apply { parentFile?.mkdirs() }

                withContext(Dispatchers.IO) {
                    appContext.contentResolver.openInputStream(ready.uri)?.use { input ->
                        imageFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    if (bitmap != null) {
                        val thumb = scaleForThumbnail(bitmap, maxEdge = 300)
                        FileOutputStream(thumbFile).use { out ->
                            thumb.compress(Bitmap.CompressFormat.JPEG, 80, out)
                        }
                    }
                }

                val capture = Capture(
                    id = id,
                    createdAt = Clock.System.now(),
                    imagePath = imageRelative,
                    thumbnailPath = thumbRelative,
                    pair = LanguagePair(source = null, target = ready.target),
                    detectedSource = ready.blocks.firstOrNull()?.detectedSource ?: Languages.default,
                    blocks = ready.blocks,
                )
                captures.save(capture)
                _state.value = ImportState.Saved(id)
            } catch (cause: Exception) {
                _state.value = ImportState.Failed(cause.message ?: "Could not save")
            }
        }
    }

    fun discard() {
        _state.value = ImportState.Empty
    }

    override fun onCleared() {
        recognizer.close()
        super.onCleared()
    }

    companion object {
        fun factory(container: AppContainer, appContext: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ImportImageViewModel(
                        appContext = appContext,
                        orchestrator = container.translationOrchestrator,
                        captures = container.captureRepository,
                        settings = container.settingsRepository,
                        capturesDir = container.capturesDir,
                    ) as T
            }
    }
}

private fun scaleForThumbnail(bitmap: Bitmap, maxEdge: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxEdge && h <= maxEdge) return bitmap
    val ratio = maxEdge.toFloat() / maxOf(w, h)
    val newW = (w * ratio).toInt().coerceAtLeast(1)
    val newH = (h * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
}
