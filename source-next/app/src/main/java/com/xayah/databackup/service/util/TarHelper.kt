package com.xayah.databackup.service.util

import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.RootShellHelper
import com.xayah.databackup.util.SymbolHelper

object TarHelper {
    private const val TAG = "TarHelper"

    suspend fun decompress(src: String, dst: String): Pair<Int, String> {
        val shell = RootShellHelper.executeQuoted(
            "zstd", "-d", "-c", src,
            "|", "tar", "--totals", "-xmpf", "-",
            "-C", dst,
        )
        if (shell.code != 0) {
            LogHelper.e(TAG, "decompress", "Failed: ${shell.out.joinToString("\n")}")
        }
        return shell.code to shell.out.joinToString("\n")
    }

    suspend fun decompressWithExclusions(
        src: String,
        dst: String,
        exclusions: List<String>,
    ): Pair<Int, String> {
        val exclusionArgs = exclusions.flatMap { listOf("--exclude", SymbolHelper.shellQuote(it)) }
        val shell = RootShellHelper.executeQuoted(
            "zstd", "-d", "-c", src,
            "|", "tar", "--totals", *exclusionArgs.toTypedArray(), "-xmpf", "-",
            "-C", dst,
        )
        if (shell.code != 0) {
            LogHelper.e(TAG, "decompressWithExclusions", "Failed: ${shell.out.joinToString("\n")}")
        }
        return shell.code to shell.out.joinToString("\n")
    }
}
