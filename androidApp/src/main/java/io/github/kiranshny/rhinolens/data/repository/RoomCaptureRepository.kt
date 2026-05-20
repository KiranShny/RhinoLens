package io.github.kiranshny.rhinolens.data.repository

import io.github.kiranshny.rhinolens.data.db.CaptureDao
import io.github.kiranshny.rhinolens.data.db.CaptureEntity
import io.github.kiranshny.rhinolens.shared.domain.Capture
import io.github.kiranshny.rhinolens.shared.domain.LanguageCode
import io.github.kiranshny.rhinolens.shared.domain.LanguagePair
import io.github.kiranshny.rhinolens.shared.domain.Languages
import io.github.kiranshny.rhinolens.shared.domain.TranslatedBlock
import io.github.kiranshny.rhinolens.shared.port.CaptureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

class RoomCaptureRepository(
    private val dao: CaptureDao,
    private val filesRoot: File,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : CaptureRepository {

    private val blockListSerializer = ListSerializer(TranslatedBlock.serializer())

    override fun observe(): Flow<List<Capture>> =
        dao.observeAll().map { rows -> rows.map(::toDomain) }

    override suspend fun get(id: String): Capture? =
        dao.get(id)?.let(::toDomain)

    override suspend fun save(capture: Capture) {
        dao.upsert(toEntity(capture))
    }

    override suspend fun delete(id: String) {
        val entity = dao.get(id) ?: return
        File(filesRoot, entity.imagePath).delete()
        File(filesRoot, entity.thumbnailPath).delete()
        dao.delete(id)
    }

    override suspend fun clear() {
        dao.clear()
        val captures = File(filesRoot, "captures")
        captures.deleteRecursively()
        captures.mkdirs()
    }

    private fun toDomain(entity: CaptureEntity): Capture {
        val target = Languages.byCode(LanguageCode(entity.targetLangCode)) ?: Languages.default
        val source = entity.sourceLangCode?.let { Languages.byCode(LanguageCode(it)) }
        val detected = Languages.byCode(LanguageCode(entity.detectedLangCode)) ?: Languages.default
        val blocks = runCatching {
            json.decodeFromString(blockListSerializer, entity.blocksJson)
        }.getOrDefault(emptyList())
        return Capture(
            id = entity.id,
            createdAt = Instant.fromEpochMilliseconds(entity.createdAt),
            imagePath = entity.imagePath,
            thumbnailPath = entity.thumbnailPath,
            pair = LanguagePair(source = source, target = target),
            detectedSource = detected,
            blocks = blocks,
        )
    }

    private fun toEntity(capture: Capture): CaptureEntity = CaptureEntity(
        id = capture.id,
        createdAt = capture.createdAt.toEpochMilliseconds(),
        imagePath = capture.imagePath,
        thumbnailPath = capture.thumbnailPath,
        sourceLangCode = capture.pair.source?.code?.value,
        detectedLangCode = capture.detectedSource.code.value,
        targetLangCode = capture.pair.target.code.value,
        blocksJson = json.encodeToString(blockListSerializer, capture.blocks),
    )
}
