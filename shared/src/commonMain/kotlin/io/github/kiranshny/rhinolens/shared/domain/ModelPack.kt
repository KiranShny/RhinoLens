package io.github.kiranshny.rhinolens.shared.domain

data class DownloadedPack(
    val lang: Language,
    val sizeBytes: Long,
    val lastUsedEpochMs: Long?,
)

sealed interface DownloadProgress {
    data class InProgress(val lang: LanguageCode, val percent: Int) : DownloadProgress
    data class Done(val lang: LanguageCode) : DownloadProgress
    data class Failed(val lang: LanguageCode, val error: RhinoError) : DownloadProgress
}
