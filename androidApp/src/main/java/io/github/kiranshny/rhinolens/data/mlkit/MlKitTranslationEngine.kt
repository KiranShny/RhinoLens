package io.github.kiranshny.rhinolens.data.mlkit

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import io.github.kiranshny.rhinolens.shared.domain.LanguageCode
import io.github.kiranshny.rhinolens.shared.port.TranslationEngine
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.tasks.await

class MlKitTranslationEngine : TranslationEngine {

    private val translators = ConcurrentHashMap<String, Translator>()
    private val languageId = LanguageIdentification.getClient()

    override suspend fun translate(
        text: String,
        source: LanguageCode?,
        target: LanguageCode,
    ): String {
        if (text.isBlank()) return text
        val sourceTag = source?.value ?: detectLanguage(text)?.value ?: return text
        val translator = translatorFor(sourceTag, target.value) ?: return text
        translator.downloadModelIfNeeded(
            DownloadConditions.Builder().build(),
        ).await()
        return translator.translate(text).await()
    }

    override suspend fun detectLanguage(text: String): LanguageCode? {
        if (text.isBlank()) return null
        val tag = languageId.identifyLanguage(text).await()
        return if (tag == "und") null else LanguageCode(tag)
    }

    private fun translatorFor(sourceTag: String, targetTag: String): Translator? {
        val sourceLang = TranslateLanguage.fromLanguageTag(sourceTag) ?: return null
        val targetLang = TranslateLanguage.fromLanguageTag(targetTag) ?: return null
        val key = "$sourceTag>$targetTag"
        return translators.getOrPut(key) {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLang)
                    .setTargetLanguage(targetLang)
                    .build(),
            )
        }
    }

    fun close() {
        translators.values.forEach { it.close() }
        translators.clear()
        languageId.close()
    }
}
