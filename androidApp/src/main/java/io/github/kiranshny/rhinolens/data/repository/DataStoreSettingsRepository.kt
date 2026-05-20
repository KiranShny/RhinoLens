package io.github.kiranshny.rhinolens.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.kiranshny.rhinolens.shared.domain.Language
import io.github.kiranshny.rhinolens.shared.domain.LanguageCode
import io.github.kiranshny.rhinolens.shared.domain.Languages
import io.github.kiranshny.rhinolens.shared.domain.ThemeMode
import io.github.kiranshny.rhinolens.shared.port.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private val targetLangKey = stringPreferencesKey("target_lang")
    private val themeKey = stringPreferencesKey("theme_mode")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")

    override val targetLanguage: Flow<Language> = dataStore.data.map { prefs ->
        val stored = prefs[targetLangKey]
        if (stored != null) {
            Languages.byCode(LanguageCode(stored)) ?: deviceDefault()
        } else {
            deviceDefault()
        }
    }

    override val theme: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[themeKey]?.let { name ->
            runCatching { ThemeMode.valueOf(name) }.getOrNull()
        } ?: ThemeMode.SYSTEM
    }

    override val dynamicColor: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[dynamicColorKey] ?: true
    }

    override suspend fun setTargetLanguage(language: Language) {
        dataStore.edit { it[targetLangKey] = language.code.value }
    }

    override suspend fun setTheme(theme: ThemeMode) {
        dataStore.edit { it[themeKey] = theme.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[dynamicColorKey] = enabled }
    }

    private fun deviceDefault(): Language {
        val tag = Locale.getDefault().language
        return Languages.byTag(tag) ?: Languages.default
    }
}
