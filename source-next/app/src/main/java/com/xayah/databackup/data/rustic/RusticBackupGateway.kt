package com.xayah.databackup.data.rustic

import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.rootservice.ICallback
import com.xayah.databackup.rootservice.RemoteRootService

/** Provides privileged Rustic repository and filesystem operations through [RemoteRootService]. */
class RusticBackupGateway {
    suspend fun exists(path: String): Boolean = RemoteRootService.exists(path)

    suspend fun isDirectoryEmpty(path: String): Boolean = RemoteRootService.listFilePaths(path, listFiles = true, listDirs = true).isEmpty()

    suspend fun packageSourcePaths(packageName: String, userId: Int): List<String> = RemoteRootService.getPackageSourceDir(packageName, userId)

    suspend fun createDirectory(path: String): Boolean = RemoteRootService.mkdirs(path)

    suspend fun writeText(path: String, content: String) {
        RemoteRootService.writeText(path, content)
    }

    suspend fun deleteRecursively(path: String): Boolean = RemoteRootService.deleteRecursively(path)

    suspend fun initRepository(repositoryPath: String, password: String, options: Map<String, String> = emptyMap()) {
        RemoteRootService.initRusticRepository(repositoryPath, password, options)
    }

    suspend fun repositoryExists(repositoryPath: String): Boolean = RemoteRootService.rusticRepositoryExists(repositoryPath)

    suspend fun validateRepository(repositoryPath: String, password: String) {
        RemoteRootService.validateRusticRepository(repositoryPath, password)
    }

    suspend fun prepareRepository(repositoryPath: String, password: String, storage: BackupBackend.RusticStorage) {
        val location = storage.repositoryLocation(repositoryPath)
        if (repositoryExists(location)) {
            validateRepository(location, password)
            return
        }
        if (exists(repositoryPath) && isDirectoryEmpty(repositoryPath).not() && storage.isCloud.not()) {
            throw IllegalStateException("Rustic repository config is missing from a non-empty directory.")
        }
        if (storage.isCloud.not() && createDirectory(repositoryPath).not()) {
            throw IllegalStateException("Failed to create Rustic repository directory.")
        }
        initRepository(location, password, storage.backendOptions())
        if (repositoryExists(location).not()) {
            throw IllegalStateException("Rustic repository initialization did not create a repository config.")
        }
    }

    suspend fun createSnapshot(
        repositoryPath: String,
        password: String,
        sourcePaths: List<String>,
        tags: List<String>,
        storage: BackupBackend.RusticStorage,
        cancelId: Long,
        onProgress: (Long, Long, Float) -> Unit,
    ): String {
        val location = storage.repositoryLocation(repositoryPath)
        return RemoteRootService.createRusticSnapshot(
            repositoryPath = location,
            password = password,
            sourcePaths = sourcePaths,
            tags = tags,
            cancelId = cancelId,
            options = storage.backendOptions(),
            callback = object : ICallback.Stub() {
                override fun onProgress(bytesWritten: Long, speed: Long, progress: Float) {
                    onProgress(bytesWritten, speed, progress)
                }
            },
        )
    }

    suspend fun cancelSnapshot(cancelId: Long) {
        RemoteRootService.cancelRusticBackup(cancelId)
    }

    suspend fun listSnapshots(
        repositoryPath: String,
        password: String,
        tagFilter: String? = null,
        storage: BackupBackend.RusticStorage,
    ): String {
        val location = storage.repositoryLocation(repositoryPath)
        return RemoteRootService.listRusticSnapshots(location, password, tagFilter)
    }
}
