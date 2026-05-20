package io.github.kiranshny.rhinolens.shared.domain

sealed interface RhinoError {
    data object NoNetwork : RhinoError
    data object LowDisk : RhinoError
    data object UnsupportedLanguagePair : RhinoError
    data class ModelDownloadFailed(val lang: LanguageCode, val cause: String) : RhinoError
    data class TranslationFailed(val cause: String) : RhinoError
    data class OcrFailed(val cause: String) : RhinoError
    data class StorageWriteFailed(val cause: String) : RhinoError
}

class RhinoException(val error: RhinoError) : Exception(error.toString())
