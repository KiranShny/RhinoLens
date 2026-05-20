@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.kiranshny.rhinolens.shared.orchestrator

import io.github.kiranshny.rhinolens.shared.domain.Language
import io.github.kiranshny.rhinolens.shared.domain.LanguageCode
import io.github.kiranshny.rhinolens.shared.domain.LanguagePair
import io.github.kiranshny.rhinolens.shared.domain.Languages
import io.github.kiranshny.rhinolens.shared.domain.OcrFrame
import io.github.kiranshny.rhinolens.shared.domain.TextBlock
import io.github.kiranshny.rhinolens.shared.domain.TranslatedBlock
import io.github.kiranshny.rhinolens.shared.port.TranslationEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class TranslationOrchestrator(
    private val translator: TranslationEngine,
    private val cache: LruCache<TranslationKey, String> = LruCache(maxSize = 256),
    private val smoothing: BboxSmoother = BboxSmoother(alpha = 0.6f),
    private val confidenceFloor: Float = 0.5f,
    private val maxInFlight: Int = 4,
    private val autoDetectSampleLength: Int = 200,
) {

    fun translatedStream(
        ocr: Flow<OcrFrame>,
        pair: StateFlow<LanguagePair>,
    ): Flow<List<TranslatedBlock>> =
        ocr.combine(pair) { frame, languagePair -> frame to languagePair }
            .mapLatest { (frame, languagePair) -> processFrame(frame, languagePair) }

    private suspend fun processFrame(
        frame: OcrFrame,
        pair: LanguagePair,
    ): List<TranslatedBlock> = coroutineScope {
        val filtered = frame.blocks.filter { it.confidence >= confidenceFloor }
        if (filtered.isEmpty()) return@coroutineScope emptyList()

        val smoothed = filtered.map { block ->
            block.copy(bbox = smoothing.smooth(block.id, block.bbox))
        }

        val detectedSource = resolveSource(pair.source, smoothed)
        val effectiveSourceCode = detectedSource.code
        val target = pair.target

        val semaphore = Semaphore(maxInFlight)
        smoothed.map { block ->
            async {
                semaphore.withPermit {
                    translateOne(block, effectiveSourceCode, target, detectedSource)
                }
            }
        }.awaitAll()
    }

    private suspend fun resolveSource(
        explicitSource: Language?,
        blocks: List<TextBlock>,
    ): Language {
        if (explicitSource != null) return explicitSource
        if (blocks.isEmpty()) return Languages.default
        val sample = blocks
            .joinToString(separator = " ") { it.text }
            .take(autoDetectSampleLength)
        val detected = runCatching { translator.detectLanguage(sample) }.getOrNull()
        return detected
            ?.let { Languages.byCode(it) }
            ?: Languages.default
    }

    private suspend fun translateOne(
        block: TextBlock,
        sourceCode: LanguageCode,
        target: Language,
        detectedSource: Language,
    ): TranslatedBlock {
        val key = TranslationKey(block.text, sourceCode, target.code)
        val cached = cache.get(key)
        val translatedText = cached ?: run {
            val result = runCatching {
                translator.translate(block.text, sourceCode, target.code)
            }.getOrElse { block.text }
            cache.put(key, result)
            result
        }
        return TranslatedBlock(
            source = block,
            translated = translatedText,
            detectedSource = detectedSource,
            target = target,
        )
    }
}
