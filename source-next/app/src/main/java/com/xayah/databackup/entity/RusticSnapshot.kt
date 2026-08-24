package com.xayah.databackup.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RusticSnapshot(
    val id: String,
    val time: Long,
    val tags: List<String> = emptyList(),
)
