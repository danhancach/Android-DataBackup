package com.xayah.databackup.service.util

import com.xayah.databackup.rootservice.RemoteRootService

data class ChecksumMismatch(
    val archivePath: String,
    val expected: String,
    val actual: String,
)

/** Writes and verifies per-archive MD5 sidecar files. */
object ChecksumUtil {
    suspend fun write(src: String): String? =
        RemoteRootService.calculateMD5(src)?.also { md5 ->
            RemoteRootService.writeText("$src.md5", md5)
        }

    suspend fun verify(src: String): ChecksumMismatch? {
        val md5File = "$src.md5"
        if (RemoteRootService.exists(md5File).not()) return null

        val expected = RemoteRootService.readText(md5File).trim()
        if (expected.isEmpty()) return null

        val actual = RemoteRootService.calculateMD5(src) ?: return null
        if (actual.isEmpty()) return null

        return if (expected.equals(actual, ignoreCase = true)) {
            null
        } else {
            ChecksumMismatch(archivePath = src, expected = expected, actual = actual)
        }
    }
}
