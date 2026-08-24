package com.xayah.databackup.entity

import java.util.Calendar

object BackupScheduleDefaults {
    const val HOUR = 2
    const val MINUTE = 0
}

object BackupScheduleCalculator {
    fun delayToNextRunMillis(
        hour: Int,
        minute: Int,
        now: Calendar = Calendar.getInstance(),
    ): Long {
        val target = (now.clone() as Calendar).apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return target.timeInMillis - now.timeInMillis
    }

    fun nextRunAtMillis(
        hour: Int,
        minute: Int,
        now: Calendar = Calendar.getInstance(),
    ): Long {
        return now.timeInMillis + delayToNextRunMillis(hour, minute, now)
    }
}
