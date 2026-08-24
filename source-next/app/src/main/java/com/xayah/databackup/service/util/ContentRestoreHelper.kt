package com.xayah.databackup.service.util

import android.content.ContentValues
import android.net.Uri
import android.provider.BaseColumns
import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.database.entity.FieldMap

object ContentRestoreHelper {
    private val skipKeys = setOf(
        BaseColumns._ID,
        "_id",
    )

    fun toContentValues(fields: FieldMap): ContentValues {
        val values = ContentValues()
        fields.forEach { (key, value) ->
            if (skipKeys.contains(key) || key.endsWith("_id", ignoreCase = true)) return@forEach
            when (value) {
                is Long -> values.put(key, value)
                is Int -> values.put(key, value)
                is Double -> {
                    val longValue = value.toLong()
                    if (value == longValue.toDouble()) {
                        values.put(key, longValue)
                    } else {
                        values.put(key, value)
                    }
                }
                is Float -> values.put(key, value)
                is Boolean -> values.put(key, value)
                is String -> values.put(key, value)
            }
        }
        return values
    }

    fun insert(uri: Uri, fields: FieldMap): Uri? {
        return application.contentResolver.insert(uri, toContentValues(fields))
    }
}
