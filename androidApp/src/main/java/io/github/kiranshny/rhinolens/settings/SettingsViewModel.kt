package io.github.kiranshny.rhinolens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.kiranshny.rhinolens.AppContainer
import io.github.kiranshny.rhinolens.shared.domain.DownloadedPack
import io.github.kiranshny.rhinolens.shared.domain.Language
import io.github.kiranshny.rhinolens.shared.domain.LanguageCode
import io.github.kiranshny.rhinolens.shared.domain.Languages
import io.github.kiranshny.rhinolens.shared.domain.ThemeMode
import io.github.kiranshny.rhinolens.shared.port.CaptureRepository
import io.github.kiranshny.rhinolens.shared.port.ModelPackManager
import io.github.kiranshny.rhinolens.shared.port.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val captures: CaptureRepository,
    private val packs: ModelPackManager,
) : ViewModel() {

    val targetLanguage: StateFlow<Language> = settings.targetLanguage
        .stateIn(viewModelScope, SharingStarted.Eagerly, Languages.default)

    val theme: StateFlow<ThemeMode> = settings.theme
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val dynamicColor: StateFlow<Boolean> = settings.dynamicColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val downloadedPacks: StateFlow<List<DownloadedPack>> = packs.observePacks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    fun setTargetLanguage(language: Language) {
        viewModelScope.launch { settings.setTargetLanguage(language) }
    }

    fun setTheme(theme: ThemeMode) {
        viewModelScope.launch { settings.setTheme(theme) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settings.setDynamicColor(enabled) }
    }

    fun deletePack(code: LanguageCode) {
        viewModelScope.launch { packs.delete(code) }
    }

    fun clearHistory() {
        viewModelScope.launch { captures.clear() }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SettingsViewModel(
                        settings = container.settingsRepository,
                        captures = container.captureRepository,
                        packs = container.modelPackManager,
                    ) as T
            }
    }
}
