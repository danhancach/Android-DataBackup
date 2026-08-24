package com.xayah.databackup.data.rustic

import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper

class RusticRestoreCoordinator(
    private val mGateway: RusticBackupGateway,
) {
    companion object {
        private const val TAG = "RusticRestoreCoordinator"
    }

    suspend fun restoreSnapshot(config: BackupConfig, snapshotId: String): String {
        val backend = config.backupBackend as? BackupBackend.Rustic
            ?: throw IllegalStateException("Current backup is not a Rustic backup.")
        val repositoryPath = PathHelper.getBackupRepoDir(config.path)
        val createdAt = System.currentTimeMillis()
        val stagingPath = PathHelper.getRusticRestoreStagingDir(config.uuidString, createdAt)

        RemoteRootService.deleteRecursively(stagingPath)
        RemoteRootService.mkdirs(stagingPath)

        mGateway.prepareRepository(repositoryPath, backend.password, backend.storage)
        val rusticRepositoryPath = backend.storage.repositoryLocation(repositoryPath)
        RemoteRootService.restoreRusticSnapshot(
            repositoryPath = rusticRepositoryPath,
            password = backend.password,
            snapshotId = snapshotId,
            destinationPath = stagingPath,
        )

        LogHelper.i(TAG, "restoreSnapshot", "Restored snapshot $snapshotId to $stagingPath")
        return stagingPath
    }

    suspend fun restoreLatestSnapshot(config: BackupConfig): String {
        return restoreSnapshot(config, "latest")
    }

    suspend fun cleanupStaging(stagingPath: String) {
        if (RemoteRootService.deleteRecursively(stagingPath).not()) {
            LogHelper.w(TAG, "cleanupStaging", "Failed to clean staging: $stagingPath")
        }
    }
}
