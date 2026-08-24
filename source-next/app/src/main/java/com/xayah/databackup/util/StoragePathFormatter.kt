package com.xayah.databackup.util

import android.content.Context
import com.xayah.databackup.R

object StoragePathFormatter {
    fun locationTitle(context: Context, path: String): String {
        val normalized = path.trim().trimEnd('/')
        return when {
            normalized.contains("/emulated/") -> context.getString(R.string.internal_storage)
            normalized.startsWith("/mnt/media_rw/") -> {
                val volumeId = normalized
                    .removePrefix("/mnt/media_rw/")
                    .substringBefore('/')
                if (volumeId.isNotEmpty()) {
                    context.getString(R.string.external_storage_volume, volumeId)
                } else {
                    context.getString(R.string.external_storage)
                }
            }
            normalized.startsWith("/storage/") && normalized.contains("/emulated/").not() ->
                context.getString(R.string.external_storage)
            else -> context.getString(R.string.custom_storage)
        }
    }

    fun locationSubtitle(path: String): String {
        val normalized = path.trim().trimEnd('/')
        if (normalized.isEmpty()) return ""
        val leaf = PathHelper.getChildPath(normalized)
        return leaf.ifEmpty { normalized }
    }
}
