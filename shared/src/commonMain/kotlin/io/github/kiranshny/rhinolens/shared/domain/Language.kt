package io.github.kiranshny.rhinolens.shared.domain

import kotlinx.serialization.Serializable

@Serializable
data class LanguageCode(val value: String)

@Serializable
data class Language(
    val code: LanguageCode,
    val displayName: String,
    val nativeName: String,
)
