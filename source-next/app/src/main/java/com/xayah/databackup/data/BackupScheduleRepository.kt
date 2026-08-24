package com.xayah.databackup.data

import android.content.Context
import com.xayah.databackup.App
import com.xayah.databackup.entity.BackupScheduleCalculator
import com.xayah.databackup.entity.BackupScheduleDefaults
import com.xayah.databackup.util.KeyScheduleEnabled
import com.xayah.databackup.util.KeyScheduleHour
import com.xayah.databackup.util.KeyScheduleLastRunAt
import com.xayah.databackup.util.KeyScheduleMinute
import com.xayah.databackup.util.ScheduleEnabled
import com.xayah.databackup.util.ScheduleHour
import com.xayah.databackup.util.ScheduleLastRunAt
import com.xayah.databackup.util.ScheduleMinute
import com.xayah.databackup.util.WorkManagerHelper
import com.xayah.databackup.util.readBoolean
import com.xayah.databackup.util.readInt
import com.xayah.databackup.util.readLong
import com.xayah.databackup.util.saveBoolean
import com.xayah.databackup.util.saveInt
import com.xayah.databackup.util.saveLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

data class BackupScheduleState(
    val enabled: Boolean = false,
    val hour: Int = BackupScheduleDefaults.HOUR,
    val minute: Int = BackupScheduleDefaults.MINUTE,
    val lastRunAt: Long = 0L,
) {
    val nextRunAt: Long?
        get() = if (enabled) BackupScheduleCalculator.nextRunAtMillis(hour, minute) else null
}

class BackupScheduleRepository(
    private val context: Context = App.application,
) {
    val state: Flow<BackupScheduleState> = combine(
        context.readBoolean(ScheduleEnabled),
        context.readInt(ScheduleHour),
        context.readInt(ScheduleMinute),
        context.readLong(ScheduleLastRunAt),
    ) { enabled, hour, minute, lastRunAt ->
        BackupScheduleState(
            enabled = enabled,
            hour = hour,
            minute = minute,
            lastRunAt = lastRunAt,
        )
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.saveBoolean(KeyScheduleEnabled, enabled)
        if (enabled) {
            reschedule()
        } else {
            WorkManagerHelper.cancelScheduledBackup()
        }
    }

    suspend fun updateTime(hour: Int, minute: Int) {
        context.saveInt(KeyScheduleHour, hour.coerceIn(0, 23))
        context.saveInt(KeyScheduleMinute, minute.coerceIn(0, 59))
        if (context.readBoolean(ScheduleEnabled).first()) {
            reschedule()
        }
    }

    suspend fun recordLastRun(atMillis: Long = System.currentTimeMillis()) {
        context.saveLong(KeyScheduleLastRunAt, atMillis)
    }

    suspend fun reschedule() {
        val current = state.first()
        if (current.enabled.not()) {
            WorkManagerHelper.cancelScheduledBackup()
            return
        }
        val delay = BackupScheduleCalculator.delayToNextRunMillis(current.hour, current.minute)
        WorkManagerHelper.enqueueScheduledBackup(delay)
    }
}
