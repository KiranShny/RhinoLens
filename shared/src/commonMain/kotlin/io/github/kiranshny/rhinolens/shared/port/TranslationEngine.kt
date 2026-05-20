package io.github.kiranshny.rhinolens.shared.port

import io.github.kiranshny.rhinolens.shared.domain.LanguageCode

interface TranslationEngine {

    suspend fun translate(
        text: String,
        source: LanguageCode?,
        target: LanguageCode,
    ): String

    suspend fun detectLanguage(text: String): LanguageCode?
}
