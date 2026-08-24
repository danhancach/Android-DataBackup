package com.xayah.databackup.service.util

import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.PathHelper

data class IntegrityIssue(
    val label: String,
    val packageName: String,
    val missingFiles: List<String>,
) {
    val isEmpty: Boolean get() = missingFiles.isEmpty()
}

data class IntegrityReport(
    val issues: List<IntegrityIssue>,
) {
    val isEmpty: Boolean get() = issues.isEmpty()

    fun formatMessage(): String = buildString {
        issues.forEach { issue ->
            if (issue.isEmpty) {
                append("· ${issue.label} (${issue.packageName}): no backup files\n")
            } else {
                append("· ${issue.label} (${issue.packageName}): ${issue.missingFiles.joinToString(", ")}\n")
            }
        }
    }
}

object IntegrityChecker {
    suspend fun checkDir(
        srcDir: String,
        label: String,
        packageName: String,
    ): IntegrityIssue? {
        if (RemoteRootService.exists(srcDir).not()) {
            return IntegrityIssue(label = label, packageName = packageName, missingFiles = emptyList())
        }

        val files = RemoteRootService.listFilePaths(path = srcDir, listFiles = true, listDirs = false)
            .map { it.path }
        val archiveFiles = files.filter(PathHelper::isArchiveFile)
        val md5Files = files.filter { it.endsWith(".md5") }

        if (archiveFiles.isEmpty() && md5Files.isEmpty()) {
            return IntegrityIssue(label = label, packageName = packageName, missingFiles = emptyList())
        }

        val missing = md5Files.mapNotNull { md5 ->
            val archive = md5.removeSuffix(".md5")
            if (RemoteRootService.exists(archive)) null else archive.substringAfterLast('/')
        }
        return if (missing.isNotEmpty()) {
            IntegrityIssue(label = label, packageName = packageName, missingFiles = missing)
        } else {
            null
        }
    }

    suspend fun checkAppBackup(
        backupRoot: String,
        label: String,
        packageName: String,
    ): IntegrityIssue? {
        val appDirName = PathHelper.sanitizeAppDirName(label, packageName)
        val appDir = PathHelper.getBackupAppsDir(backupRoot, appDirName)
        return checkDir(appDir, label, packageName)
    }
}
