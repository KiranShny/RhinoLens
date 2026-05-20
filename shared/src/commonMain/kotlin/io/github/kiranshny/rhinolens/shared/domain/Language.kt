package io.github.kiranshny.rhinolens.shared.domain

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class LanguageCode(val value: String)

@Serializable
data class Language(
    val code: LanguageCode,
    val displayName: String,
    val nativeName: String,
)
