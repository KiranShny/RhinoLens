package io.github.kiranshny.rhinolens.shared.port

import io.github.kiranshny.rhinolens.shared.domain.Language
import io.github.kiranshny.rhinolens.shared.domain.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val targetLanguage: Flow<Language>

    val theme: Flow<ThemeMode>

    val dynamicColor: Flow<Boolean>

    suspend fun setTargetLanguage(language: Language)

    suspend fun setTheme(theme: ThemeMode)

    suspend fun setDynamicColor(enabled: Boolean)
}
