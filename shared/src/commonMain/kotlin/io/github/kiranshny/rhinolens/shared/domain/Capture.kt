package io.github.kiranshny.rhinolens.shared.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Capture(
    val id: String,
    val createdAt: Instant,
    val imagePath: String,
    val thumbnailPath: String,
    val pair: LanguagePair,
    val detectedSource: Language,
    val blocks: List<TranslatedBlock>,
)
