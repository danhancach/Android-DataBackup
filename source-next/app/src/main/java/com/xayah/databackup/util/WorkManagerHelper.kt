package com.xayah.databackup.util

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.xayah.databackup.App
import com.xayah.databackup.workers.AppsUpdateWorker
import com.xayah.databackup.workers.OthersUpdateWorker
import com.xayah.databackup.workers.ScheduledBackupWorker

object WorkManagerHelper {
    private const val APPS_UPDATE_WORK_NAME = "apps_update_work"
    private const val OTHERS_UPDATE_WORK_NAME = "others_update_work"
    private const val SCHEDULED_BACKUP_WORK_NAME = "scheduled_backup_work"

    fun enqueueAppsUpdateWork() {
        WorkManager.getInstance(App.application)
            .enqueueUniqueWork(APPS_UPDATE_WORK_NAME, ExistingWorkPolicy.KEEP, AppsUpdateWorker.buildRequest())
    }

    fun enqueueOthersUpdateWork() {
        WorkManager.getInstance(App.application)
            .enqueueUniqueWork(OTHERS_UPDATE_WORK_NAME, ExistingWorkPolicy.KEEP, OthersUpdateWorker.buildRequest())
    }

    fun enqueueScheduledBackup(delayMillis: Long) {
        WorkManager.getInstance(App.application).enqueueUniqueWork(
            SCHEDULED_BACKUP_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            ScheduledBackupWorker.buildRequest(delayMillis),
        )
    }

    fun cancelScheduledBackup() {
        WorkManager.getInstance(App.application).cancelUniqueWork(SCHEDULED_BACKUP_WORK_NAME)
    }
}
