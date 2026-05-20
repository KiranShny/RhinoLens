package io.github.kiranshny.rhinolens.shared.domain

import kotlinx.serialization.Serializable

@Serializable
data class TextBlock(
    val id: String,
    val text: String,
    val bbox: NormalizedRect,
    val rotationDeg: Float,
    val confidence: Float,
)
