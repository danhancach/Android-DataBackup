package com.xayah.databackup.data

import com.xayah.databackup.App
import com.xayah.databackup.data.rustic.RusticBackupCoordinator
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.util.BackupConfigSelectedUuid
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.readString
import kotlinx.coroutines.flow.first

class ScheduledBackupRunner(
    private val backupConfigRepository: BackupConfigRepository,
    private val backupProcessRepository: BackupProcessRepository,
    private val rusticBackupCoordinator: RusticBackupCoordinator,
    private val backupScheduleRepository: BackupScheduleRepository,
) {
    companion object {
        private const val TAG = "ScheduledBackupRunner"
    }

    suspend fun run(): Result<Unit> = runCatching {
        backupConfigRepository.loadBackupConfigsFromLocal()
        val savedUuid = App.application.readString(BackupConfigSelectedUuid).first()
        val configs = backupConfigRepository.configs.first()
        val index = configs.indexOfFirst { it.uuidString == savedUuid }
        check(index >= 0) { "No backup is selected for scheduled backup." }

        backupConfigRepository.selectBackup(index)
        val config = backupConfigRepository.getCurrentConfig()
        LogHelper.i(TAG, "run", "Starting scheduled backup for ${config.displayTitle}.")

        when (config.backupBackend) {
            is BackupBackend.Archive -> {
                backupProcessRepository.reset()
                backupProcessRepository.onStart()
            }
            is BackupBackend.Rustic -> {
                rusticBackupCoordinator.start(cancelId = 0) { event ->
                    LogHelper.d(TAG, "run", "Rustic scheduled backup event: $event")
                }
            }
        }
        backupScheduleRepository.recordLastRun()
    }.onFailure { error ->
        LogHelper.e(TAG, "run", "Scheduled backup failed.", error)
    }
}
