package io.github.kiranshny.rhinolens.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {

    @Query("SELECT * FROM captures ORDER BY created_at DESC")
    fun observeAll(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE id = :id")
    suspend fun get(id: String): CaptureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CaptureEntity)

    @Query("DELETE FROM captures WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM captures")
    suspend fun clear()
}
