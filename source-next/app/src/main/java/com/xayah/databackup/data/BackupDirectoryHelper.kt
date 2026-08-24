package com.xayah.databackup.data

import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.RootShellHelper

object BackupDirectoryHelper {
    suspend fun discover(currentPath: String): List<String> {
        val directories = linkedSetOf<String>()
        directories.add(PathHelper.DEFAULT_BACKUP_PATH)

        runCatching {
            RemoteRootService.listFilePaths(
                path = "/storage/emulated",
                listFiles = false,
                listDirs = true,
            ).forEach { entry ->
                if (entry.isDirectory) {
                    directories.add("${entry.path.trimEnd('/')}/DataBackup")
                }
            }
        }

        runCatching {
            val result = RootShellHelper.execute("ls", "-1", "/mnt/media_rw")
            if (result.code == 0) {
                result.out
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { volume ->
                        directories.add("/mnt/media_rw/$volume/DataBackup")
                    }
            }
        }

        val normalizedCurrent = currentPath.trim().trimEnd('/')
        if (normalizedCurrent.isNotEmpty()) {
            directories.add(normalizedCurrent)
        }

        return directories.sorted()
    }
}
