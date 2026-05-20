package io.github.kiranshny.rhinolens.shared.port

import io.github.kiranshny.rhinolens.shared.domain.DownloadProgress
import io.github.kiranshny.rhinolens.shared.domain.DownloadedPack
import io.github.kiranshny.rhinolens.shared.domain.LanguageCode
import kotlinx.coroutines.flow.Flow

interface ModelPackManager {

    fun observePacks(): Flow<List<DownloadedPack>>

    fun download(lang: LanguageCode): Flow<DownloadProgress>

    suspend fun delete(lang: LanguageCode)

    suspend fun isReady(source: LanguageCode?, target: LanguageCode): Boolean
}
