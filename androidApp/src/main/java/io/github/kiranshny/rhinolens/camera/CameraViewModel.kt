package io.github.kiranshny.rhinolens.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.kiranshny.rhinolens.AppContainer
import io.github.kiranshny.rhinolens.shared.domain.Capture
import io.github.kiranshny.rhinolens.shared.domain.Language
import io.github.kiranshny.rhinolens.shared.domain.LanguagePair
import io.github.kiranshny.rhinolens.shared.domain.Languages
import io.github.kiranshny.rhinolens.shared.domain.TranslatedBlock
import io.github.kiranshny.rhinolens.shared.orchestrator.TranslationOrchestrator
import io.github.kiranshny.rhinolens.shared.port.CaptureRepository
import io.github.kiranshny.rhinolens.shared.port.SettingsRepository
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class CameraViewModel(
    private val appContext: Context,
    private val orchestrator: TranslationOrchestrator,
    private val settings: SettingsRepository,
    private val captures: CaptureRepository,
    private val capturesDir: File,
) : ViewModel() {

    private val ocrSource = CameraXOcrSource(appContext.applicationContext)
    val cameraController: LifecycleCameraController get() = ocrSource.controller

    private val sourceOverride = MutableStateFlow<Language?>(null)

    val pair: StateFlow<LanguagePair> = combine(
        sourceOverride,
        settings.targetLanguage,
    ) { source, target ->
        LanguagePair(source = source, target = target)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LanguagePair(source = null, target = Languages.default),
    )

    val translatedBlocks: StateFlow<List<TranslatedBlock>> =
        orchestrator.translatedStream(ocrSource.frames, pair)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = emptyList(),
            )

    private val _capturing = MutableStateFlow(false)
    val capturing: StateFlow<Boolean> = _capturing

    fun bindCamera(lifecycleOwner: LifecycleOwner) {
        ocrSource.bind(lifecycleOwner)
    }

    fun setSource(language: Language?) {
        sourceOverride.value = language
    }

    fun setTarget(language: Language) {
        viewModelScope.launch { settings.setTargetLanguage(language) }
    }

    fun swap() {
        viewModelScope.launch {
            val current = pair.first()
            val source = current.source ?: return@launch
            sourceOverride.value = current.target
            settings.setTargetLanguage(source)
        }
    }

    fun capture(onSaved: (captureId: String) -> Unit) {
        if (_capturing.value) return
        _capturing.value = true

        val id = UUID.randomUUID().toString()
        val imageFile = File(capturesDir, "$id.jpg")
        imageFile.parentFile?.mkdirs()
        val output = ImageCapture.OutputFileOptions.Builder(imageFile).build()
        val mainExecutor = ContextCompat.getMainExecutor(appContext)

        viewModelScope.launch {
            try {
                suspendCancellableCoroutine<Unit> { cont ->
                    cameraController.takePicture(
                        output,
                        mainExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                cont.resume(Unit)
                            }
                            override fun onError(exception: ImageCaptureException) {
                                cont.resumeWithException(exception)
                            }
                        },
                    )
                }

                val thumbFile = File(capturesDir, "thumbs/$id.jpg").apply {
                    parentFile?.mkdirs()
                }
                withContext(Dispatchers.IO) {
                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    if (bitmap != null) {
                        val thumb = scaleThumbnail(bitmap, 300)
                        FileOutputStream(thumbFile).use { thumb.compress(Bitmap.CompressFormat.JPEG, 80, it) }
                    }
                }

                val currentPair = pair.value
                val currentBlocks = translatedBlocks.value
                val detected = currentBlocks.firstOrNull()?.detectedSource
                    ?: currentPair.source
                    ?: Languages.default

                val capture = Capture(
                    id = id,
                    createdAt = Clock.System.now(),
                    imagePath = "captures/$id.jpg",
                    thumbnailPath = "captures/thumbs/$id.jpg",
                    pair = currentPair,
                    detectedSource = detected,
                    blocks = currentBlocks,
                )
                captures.save(capture)
                onSaved(id)
            } catch (cause: Exception) {
                imageFile.delete()
            } finally {
                _capturing.value = false
            }
        }
    }

    override fun onCleared() {
        ocrSource.close()
        super.onCleared()
    }

    companion object {
        fun factory(container: AppContainer, appContext: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CameraViewModel(
                        appContext = appContext,
                        orchestrator = container.translationOrchestrator,
                        settings = container.settingsRepository,
                        captures = container.captureRepository,
                        capturesDir = container.capturesDir,
                    ) as T
            }
    }
}

private fun scaleThumbnail(bitmap: Bitmap, maxEdge: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxEdge && h <= maxEdge) return bitmap
    val ratio = maxEdge.toFloat() / maxOf(w, h)
    val newW = (w * ratio).toInt().coerceAtLeast(1)
    val newH = (h * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
}
