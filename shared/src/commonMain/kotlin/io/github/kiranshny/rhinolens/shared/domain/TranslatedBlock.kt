package io.github.kiranshny.rhinolens.shared.domain

import kotlinx.serialization.Serializable

@Serializable
data class TranslatedBlock(
    val source: TextBlock,
    val translated: String,
    val detectedSource: Language,
    val target: Language,
)
