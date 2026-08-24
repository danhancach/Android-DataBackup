package com.xayah.databackup.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class S3CloudConfig(
    @Json(name = "endpoint") val endpoint: String,
    @Json(name = "bucket") val bucket: String,
    @Json(name = "access_key") val accessKey: String,
    @Json(name = "secret_key") val secretKey: String,
    @Json(name = "region") val region: String = "",
    @Json(name = "root") val root: String = "databackup",
    @Json(name = "allow_insecure") val allowInsecure: Boolean = false,
) {
    fun isConfigured(): Boolean =
        endpoint.isNotBlank() && bucket.isNotBlank() && accessKey.isNotBlank() && secretKey.isNotBlank()

    fun summaryOrNull(): String? {
        if (bucket.isBlank() && endpoint.isBlank()) return null
        return when {
            bucket.isNotBlank() && endpoint.isNotBlank() -> "$bucket @ $endpoint"
            bucket.isNotBlank() -> bucket
            else -> endpoint
        }
    }

    fun toRusticOptions(): Map<String, String> = buildMap {
        put("bucket", bucket)
        put("root", root.trim('/'))
        put("endpoint", endpoint)
        put("access_key_id", accessKey)
        put("secret_access_key", secretKey)
        if (region.isNotBlank()) put("region", region)
        if (allowInsecure) put("allow_invalid_cert", "true")
    }

    companion object {
        const val RUSTIC_LOCATION = "opendal:s3"
    }
}
