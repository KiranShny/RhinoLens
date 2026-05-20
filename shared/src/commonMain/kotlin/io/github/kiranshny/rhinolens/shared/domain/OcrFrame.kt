package io.github.kiranshny.rhinolens.shared.domain

data class OcrFrame(
    val frameId: Long,
    val timestampMs: Long,
    val blocks: List<TextBlock>,
)
