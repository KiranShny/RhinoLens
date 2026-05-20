package io.github.kiranshny.rhinolens.camera

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.kiranshny.rhinolens.AppContainer
import io.github.kiranshny.rhinolens.shared.domain.Language
import io.github.kiranshny.rhinolens.shared.domain.LanguagePair
import io.github.kiranshny.rhinolens.shared.domain.TranslatedBlock
import io.github.kiranshny.rhinolens.shared.orchestrator.TranslationOrchestrator
import io.github.kiranshny.rhinolens.shared.port.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CameraViewModel(
    appContext: Context,
    private val orchestrator: TranslationOrchestrator,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val ocrSource = CameraXOcrSource(appContext.applicationContext)

    private val sourceOverride = MutableStateFlow<Language?>(null)

    val pair: StateFlow<LanguagePair> = combine(
        sourceOverride,
        settings.targetLanguage,
    ) { source, target ->
        LanguagePair(source = source, target = target)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = LanguagePair(source = null, target = io.github.kiranshny.rhinolens.shared.domain.Languages.default),
    )

    val translatedBlocks: StateFlow<List<TranslatedBlock>> =
        orchestrator.translatedStream(ocrSource.frames, pair)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = emptyList(),
            )

    fun bindCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        viewModelScope.launch {
            runCatching { ocrSource.bind(previewView, lifecycleOwner) }
        }
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
                    ) as T
            }
    }
}
