package com.xayah.databackup.service.util

import android.content.ContentUris
import android.net.Uri
import android.provider.Telephony
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
import com.xayah.databackup.database.entity.FieldMap
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.database.entity.Sms
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper
import kotlinx.coroutines.CancellationException

class RestoreMessagesHelper(private val restoreProcessRepo: RestoreProcessRepository) {
    companion object {
        private const val TAG = "RestoreMessagesHelper"
    }

    private val moshi = Moshi.Builder().build()

    private fun ensureNotCanceled() {
        if (restoreProcessRepo.mIsCanceled) throw CancellationException("Restore messages canceled.")
    }

    suspend fun start() {
        val sourcePath = restoreProcessRepo.getSourcePath()
        val smsPath = PathHelper.getBackupMessagesSmsConfigFilePath(sourcePath)
        val mmsPath = PathHelper.getBackupMessagesMmsConfigFilePath(sourcePath)

        val smsList = if (RemoteRootService.exists(smsPath)) {
            runCatching {
                moshi.adapter<List<Sms>>().fromJson(RemoteRootService.readText(smsPath)).orEmpty()
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val mmsList = if (RemoteRootService.exists(mmsPath)) {
            runCatching {
                moshi.adapter<List<Mms>>().fromJson(RemoteRootService.readText(mmsPath)).orEmpty()
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val totalCount = smsList.size + mmsList.size
        if (totalCount == 0) {
            restoreProcessRepo.updateMessagesItem {
                copy { ProcessItem.msg set application.getString(R.string.not_exist) }
            }
            return
        }

        smsList.forEachIndexed { index, sms ->
            ensureNotCanceled()
            restoreProcessRepo.updateMessagesItem {
                copy {
                    ProcessItem.currentIndex set index + 1
                    ProcessItem.msg set "SMS: ${sms.id}"
                    ProcessItem.progress set (index + 1).toFloat() / totalCount
                }
            }
            val config = sms.config?.let { json ->
                runCatching { moshi.adapter<FieldMap>().fromJson(json) }.getOrNull()
            } ?: return@forEachIndexed
            ContentRestoreHelper.insert(Telephony.Sms.CONTENT_URI, config)
        }

        mmsList.forEachIndexed { index, mms ->
            ensureNotCanceled()
            val currentIndex = smsList.size + index + 1
            restoreProcessRepo.updateMessagesItem {
                copy {
                    ProcessItem.currentIndex set currentIndex
                    ProcessItem.msg set "MMS: ${mms.id}"
                    ProcessItem.progress set currentIndex.toFloat() / totalCount
                }
            }
            restoreMms(mms)
        }

        restoreProcessRepo.updateMessagesItem {
            copy {
                ProcessItem.currentIndex set totalCount
                ProcessItem.msg set application.getString(R.string.finished)
                ProcessItem.progress set 1f
            }
        }
    }

    private fun restoreMms(mms: Mms) {
        val pdu = mms.pdu?.let { json ->
            runCatching { moshi.adapter<FieldMap>().fromJson(json) }.getOrNull()
        } ?: return
        val addrMaps = mms.addr?.let { json ->
            runCatching { moshi.adapter<List<FieldMap>>().fromJson(json) }.getOrNull()
        }.orEmpty()
        val partMaps = mms.part?.let { json ->
            runCatching { moshi.adapter<List<FieldMap>>().fromJson(json) }.getOrNull()
        }.orEmpty()

        val mmsUri = ContentRestoreHelper.insert(Telephony.Mms.CONTENT_URI, pdu) ?: return
        val mmsId = ContentUris.parseId(mmsUri)

        val addrUri = Telephony.Mms.CONTENT_URI.buildUpon()
            .appendPath(mmsId.toString())
            .appendPath("addr")
            .build()
        addrMaps.forEach { addrMap ->
            val values = ContentRestoreHelper.toContentValues(addrMap)
            values.put(Telephony.Mms.Addr.MSG_ID, mmsId)
            application.contentResolver.insert(addrUri, values)
        }

        val partUri = Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, "part")
        partMaps.forEach { partMap ->
            val values = ContentRestoreHelper.toContentValues(partMap)
            values.put(Telephony.Mms.Part.MSG_ID, mmsId)
            application.contentResolver.insert(partUri, values)
        }
    }
}
