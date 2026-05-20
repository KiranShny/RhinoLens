package io.github.kiranshny.rhinolens.shared.orchestrator

import io.github.kiranshny.rhinolens.shared.domain.LanguageCode

data class TranslationKey(
    val text: String,
    val source: LanguageCode?,
    val target: LanguageCode,
)
