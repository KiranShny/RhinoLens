package io.github.kiranshny.rhinolens.shared.port

import io.github.kiranshny.rhinolens.shared.domain.Capture
import kotlinx.coroutines.flow.Flow

interface CaptureRepository {

    fun observe(): Flow<List<Capture>>

    suspend fun get(id: String): Capture?

    suspend fun save(capture: Capture)

    suspend fun delete(id: String)

    suspend fun clear()
}
