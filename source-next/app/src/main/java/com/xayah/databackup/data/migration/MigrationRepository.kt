package com.xayah.databackup.data.migration

import android.content.Context
import android.net.Uri
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.RootShellHelper
import com.xayah.databackup.util.SymbolHelper
import com.xayah.databackup.util.TimeHelper
import java.io.File
import java.security.MessageDigest

data class MigrationAppItem(
    val dirName: String,
    val label: String,
    val packageName: String,
)

data class MigrationExportResult(
    val sha256: String,
    val appCount: Int,
)

object MigrationSafety {
    private val SHELL_META_REGEX = Regex("['\"`$\\\\;|&\r\n\u0000-\u001f]")

    fun isEntryNameSafe(name: String): Boolean = SHELL_META_REGEX.containsMatchIn(name).not()
}

class MigrationRepository(private val context: Context) {
    suspend fun listAppsInBackup(backupPath: String): List<MigrationAppItem> {
        val appsDir = "$backupPath/apps"
        if (RemoteRootService.exists(appsDir).not()) return emptyList()

        return RemoteRootService.listFilePaths(appsDir, listFiles = false, listDirs = true)
            .map { it.path }
            .mapNotNull { dirPath ->
                val dirName = PathHelper.getChildPath(dirPath)
                if (dirName.isBlank()) return@mapNotNull null
                val packageName = dirName.substringAfterLast('_', dirName)
                MigrationAppItem(dirName = dirName, label = dirName.substringBeforeLast('_', dirName), packageName = packageName)
            }
            .sortedBy { it.label.lowercase() }
    }

    suspend fun exportSelectedApps(backupPath: String, selectedDirNames: Set<String>, outputUri: Uri): MigrationExportResult {
        require(selectedDirNames.isNotEmpty()) { "No apps selected." }
        val tmpPath = "${context.filesDir}/migration_export_${System.currentTimeMillis()}.tar.zst"
        val srcArgs = selectedDirNames.map { SymbolHelper.shellQuote("apps/$it") }
        val shell = RootShellHelper.execute(
            "tar", "--totals", "-cpf", "-",
            "-C", SymbolHelper.shellQuote(backupPath),
            "--", *srcArgs.toTypedArray(),
            "|", "zstd -r -T0 -q --priority=rt",
            ">", SymbolHelper.shellQuote(tmpPath),
        )
        check(shell.code == 0) { shell.out.joinToString("\n") }

        val sha256 = sha256Of(File(tmpPath))
        context.contentResolver.openOutputStream(outputUri)?.use { out ->
            File(tmpPath).inputStream().use { input ->
                val buffer = ByteArray(1024 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    out.write(buffer, 0, read)
                }
            }
        } ?: error("Failed to open output stream.")
        File(tmpPath).delete()
        return MigrationExportResult(sha256 = sha256, appCount = selectedDirNames.size)
    }

    suspend fun parseMigrationPackage(uri: Uri, expectedSha256: String? = null): List<String> {
        cleanupImportTmp()
        val tmpFile = "${context.filesDir}/import_${TimeHelper.formatTimestampInDetail(System.currentTimeMillis())}.tar.zst"
        context.contentResolver.openInputStream(uri)?.use { input ->
            File(tmpFile).outputStream().use { output ->
                val buffer = ByteArray(1024 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                }
            }
        } ?: error("Failed to open input stream.")

        if (!expectedSha256.isNullOrBlank()) {
            val actual = sha256Of(File(tmpFile))
            val expected = expectedSha256.trim().lowercase().removePrefix("sha256:")
            check(actual == expected) { "SHA-256 mismatch." }
        }

        val shell = RootShellHelper.execute(
            "zstd", "-d", "-c", SymbolHelper.shellQuote(tmpFile),
            "|", "tar", "-tf", "-",
        )
        check(shell.code == 0) { shell.out.joinToString("\n") }

        val apps = shell.out.mapNotNull { line ->
            if (line.startsWith("apps/")) {
                line.substringAfter("apps/").substringBefore('/').takeIf { it.isNotEmpty() }
            } else {
                null
            }
        }.distinct()

        val unsafe = apps.filter { MigrationSafety.isEntryNameSafe(it).not() }
        check(unsafe.isEmpty()) { "Unsafe entry names in migration package." }

        importTmpPath = tmpFile
        return apps
    }

    suspend fun importMigrationPackage(backupPath: String) {
        val tmpFile = importTmpPath ?: error("No migration package loaded.")
        RemoteRootService.mkdirs(backupPath)
        val shell = RootShellHelper.execute(
            "zstd", "-d", "-c", SymbolHelper.shellQuote(tmpFile),
            "|", "tar", "-xpf", "-",
            "-C", SymbolHelper.shellQuote(backupPath),
        )
        check(shell.code == 0) { shell.out.joinToString("\n") }
        cleanupImportTmp()
    }

    fun cleanupImportTmp() {
        importTmpPath?.let { runCatching { File(it).delete() } }
        importTmpPath = null
    }

    private var importTmpPath: String? = null

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
