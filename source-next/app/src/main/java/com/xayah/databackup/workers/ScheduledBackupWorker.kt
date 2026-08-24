package com.xayah.databackup.workers

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import com.xayah.databackup.R
import com.xayah.databackup.data.ScheduledBackupRunner
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.NotificationHelper
import com.xayah.databackup.util.NotificationHelper.NOTIFICATION_ID_SCHEDULED_BACKUP_WORKER
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext

class ScheduledBackupWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    private val notificationBuilder = NotificationHelper.getNotificationBuilder(appContext)

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID_SCHEDULED_BACKUP_WORKER,
                notificationBuilder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID_SCHEDULED_BACKUP_WORKER, notificationBuilder.build())
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        notificationBuilder
            .setContentTitle(appContext.getString(R.string.worker_scheduled_backup))
            .setProgress(0, 0, true)
            .setOngoing(true)
        setForeground(getForegroundInfo())

        if (RemoteRootService.checkService().not()) {
            LogHelper.w(TAG, "doWork", "Root service unavailable; retrying scheduled backup later.")
            return@withContext Result.retry()
        }

        val runner = GlobalContext.get().get<ScheduledBackupRunner>()
        val result = runner.run()
        val scheduleRepository = GlobalContext.get().get<com.xayah.databackup.data.BackupScheduleRepository>()
        scheduleRepository.reschedule()

        if (result.isSuccess) {
            Result.success()
        } else {
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "ScheduledBackupWorker"

        fun buildRequest(initialDelayMillis: Long) = OneTimeWorkRequestBuilder<ScheduledBackupWorker>()
            .setInitialDelay(initialDelayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
    }
}
