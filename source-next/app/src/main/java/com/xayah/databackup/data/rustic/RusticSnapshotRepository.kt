package com.xayah.databackup.data.rustic

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.entity.RusticSnapshot
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper

class RusticSnapshotRepository(
    private val gateway: RusticBackupGateway,
) {
    companion object {
        private const val TAG = "RusticSnapshotRepository"
        private const val SNAPSHOT_CONFIG_TAG_PREFIX = "databackup:config:"
    }

    private val moshi = Moshi.Builder().build()

    suspend fun listSnapshots(config: BackupConfig): List<RusticSnapshot> {
        val backend = config.backupBackend as? BackupBackend.Rustic
            ?: return emptyList()
        val repositoryPath = PathHelper.getBackupRepoDir(config.path)
        val tagFilter = "$SNAPSHOT_CONFIG_TAG_PREFIX${config.uuidString}"
        return runCatching {
            gateway.prepareRepository(repositoryPath, backend.password, backend.storage)
            val json = gateway.listSnapshots(
                repositoryPath = repositoryPath,
                password = backend.password,
                tagFilter = tagFilter,
                storage = backend.storage,
            )
            moshi.adapter<List<RusticSnapshot>>().fromJson(json).orEmpty()
        }.onFailure {
            LogHelper.e(TAG, "listSnapshots", "Failed to list Rustic snapshots.", it)
        }.getOrDefault(emptyList())
    }
}
