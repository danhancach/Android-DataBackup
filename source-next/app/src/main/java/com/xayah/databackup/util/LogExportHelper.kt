package com.xayah.databackup.util

import com.xayah.databackup.App
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object LogExportHelper {
    private const val LOG_DIR = "logs"
    private const val LOG_FILE_PREFIX = "databackup_"
    private const val MAX_LOG_FILES = 8

    fun logFile(): File {
        val dir = File(App.application.cacheDir, LOG_DIR).apply { mkdirs() }
        return File(dir, "$LOG_FILE_PREFIX${System.currentTimeMillis()}.txt")
    }

    fun append(line: String) {
        runCatching {
            val file = logFile()
            file.appendText(line)
        }
    }

    fun createLogsZip(): File? {
        val dir = File(App.application.cacheDir, LOG_DIR)
        val logFiles = dir.listFiles { file ->
            file.isFile && file.name.startsWith(LOG_FILE_PREFIX) && file.name.endsWith(".txt")
        }?.sortedByDescending { it.lastModified() }?.take(MAX_LOG_FILES).orEmpty()
        if (logFiles.isEmpty()) return null

        return runCatching {
            val zipFile = File(App.application.cacheDir, "DataBackup_logs_${System.currentTimeMillis()}.zip")
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                logFiles.forEach { file ->
                    zos.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { input -> input.copyTo(zos) }
                    zos.closeEntry()
                }
            }
            zipFile
        }.getOrNull()
    }
}
