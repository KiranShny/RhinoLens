package io.github.kiranshny.rhinolens.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captures")
data class CaptureEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "image_path") val imagePath: String,
    @ColumnInfo(name = "thumbnail_path") val thumbnailPath: String,
    @ColumnInfo(name = "source_lang_code") val sourceLangCode: String?,
    @ColumnInfo(name = "detected_lang_code") val detectedLangCode: String,
    @ColumnInfo(name = "target_lang_code") val targetLangCode: String,
    @ColumnInfo(name = "blocks_json") val blocksJson: String,
)
