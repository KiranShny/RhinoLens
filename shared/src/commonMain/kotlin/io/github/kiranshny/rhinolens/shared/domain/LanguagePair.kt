package io.github.kiranshny.rhinolens.shared.domain

import kotlinx.serialization.Serializable

@Serializable
data class LanguagePair(
    val source: Language?,
    val target: Language,
)
