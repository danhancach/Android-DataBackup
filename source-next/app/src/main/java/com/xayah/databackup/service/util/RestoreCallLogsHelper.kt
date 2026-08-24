package com.xayah.databackup.service.util

import android.provider.CallLog
import arrow.optics.copy
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.R
import com.xayah.databackup.data.ProcessItem
import com.xayah.databackup.data.RestoreProcessRepository
import com.xayah.databackup.data.currentIndex
import com.xayah.databackup.data.msg
import com.xayah.databackup.data.progress
import com.xayah.databackup.database.entity.CallLog as CallLogEntity
import com.xayah.databackup.database.entity.FieldMap
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper
import kotlinx.coroutines.CancellationException

class RestoreCallLogsHelper(private val restoreProcessRepo: RestoreProcessRepository) {
    companion object {
        private const val TAG = "RestoreCallLogsHelper"
    }

    private val moshi = Moshi.Builder().build()

    private fun ensureNotCanceled() {
        if (restoreProcessRepo.mIsCanceled) throw CancellationException("Restore call logs canceled.")
    }

    suspend fun start() {
        val sourcePath = restoreProcessRepo.getSourcePath()
        val configPath = PathHelper.getBackupCallLogsConfigFilePath(sourcePath)
        if (RemoteRootService.exists(configPath).not()) {
            restoreProcessRepo.updateCallLogsItem {
                copy { ProcessItem.msg set application.getString(R.string.not_exist) }
            }
            return
        }

        val callLogs = runCatching {
            val json = RemoteRootService.readText(configPath)
            moshi.adapter<List<CallLogEntity>>().fromJson(json).orEmpty()
        }.onFailure {
            LogHelper.e(TAG, "start", "Failed to parse call logs json.", it)
        }.getOrDefault(emptyList())

        callLogs.forEachIndexed { index, callLog ->
            ensureNotCanceled()
            restoreProcessRepo.updateCallLogsItem {
                copy {
                    ProcessItem.currentIndex set index + 1
                    ProcessItem.msg set callLog.id.toString()
                    ProcessItem.progress set (index + 1).toFloat() / callLogs.size.coerceAtLeast(1)
                }
            }
            val callMap = callLog.call?.let { json ->
                runCatching { moshi.adapter<FieldMap>().fromJson(json) }.getOrNull()
            } ?: return@forEachIndexed
            ContentRestoreHelper.insert(CallLog.Calls.CONTENT_URI, callMap)
        }

        restoreProcessRepo.updateCallLogsItem {
            copy {
                ProcessItem.currentIndex set callLogs.size
                ProcessItem.msg set application.getString(R.string.finished)
                ProcessItem.progress set 1f
            }
        }
    }
}
