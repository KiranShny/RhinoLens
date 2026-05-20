package io.github.kiranshny.rhinolens.data.mlkit

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import io.github.kiranshny.rhinolens.shared.domain.DownloadProgress
import io.github.kiranshny.rhinolens.shared.domain.DownloadedPack
import io.github.kiranshny.rhinolens.shared.domain.LanguageCode
import io.github.kiranshny.rhinolens.shared.domain.Languages
import io.github.kiranshny.rhinolens.shared.domain.RhinoError
import io.github.kiranshny.rhinolens.shared.port.ModelPackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

private const val POLL_INTERVAL_MS = 5_000L
private const val APPROX_PACK_SIZE_BYTES = 30_000_000L

class MlKitModelPackManager(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : ModelPackManager {

    private val modelManager = RemoteModelManager.getInstance()
    private val packsState = MutableStateFlow<List<DownloadedPack>>(emptyList())

    init {
        scope.launch {
            while (isActive) {
                runCatching { refresh() }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    override fun observePacks(): Flow<List<DownloadedPack>> = packsState.asStateFlow()

    override fun download(lang: LanguageCode): Flow<DownloadProgress> = flow {
        val tag = TranslateLanguage.fromLanguageTag(lang.value)
        if (tag == null) {
            emit(DownloadProgress.Failed(lang, RhinoError.UnsupportedLanguagePair))
            return@flow
        }
        val model = TranslateRemoteModel.Builder(tag).build()
        emit(DownloadProgress.InProgress(lang, 0))
        try {
            modelManager.download(
                model,
                DownloadConditions.Builder().build(),
            ).await()
            refresh()
            emit(DownloadProgress.Done(lang))
        } catch (cause: Exception) {
            emit(
                DownloadProgress.Failed(
                    lang,
                    RhinoError.ModelDownloadFailed(lang, cause.message ?: "unknown"),
                ),
            )
        }
    }

    override suspend fun delete(lang: LanguageCode) {
        val tag = TranslateLanguage.fromLanguageTag(lang.value) ?: return
        val model = TranslateRemoteModel.Builder(tag).build()
        modelManager.deleteDownloadedModel(model).await()
        refresh()
    }

    override suspend fun isReady(source: LanguageCode?, target: LanguageCode): Boolean {
        val downloaded = downloadedTags()
        if (target.value !in downloaded) return false
        if (source == null) return true
        return source.value in downloaded
    }

    private suspend fun downloadedTags(): Set<String> {
        val models = modelManager
            .getDownloadedModels(TranslateRemoteModel::class.java)
            .await()
        return models.map { it.language }.toSet()
    }

    private suspend fun refresh() {
        val downloaded = downloadedTags()
        packsState.value = downloaded.mapNotNull { tag ->
            val language = Languages.byCode(LanguageCode(tag)) ?: return@mapNotNull null
            DownloadedPack(
                lang = language,
                sizeBytes = APPROX_PACK_SIZE_BYTES,
                lastUsedEpochMs = null,
            )
        }
    }
}
