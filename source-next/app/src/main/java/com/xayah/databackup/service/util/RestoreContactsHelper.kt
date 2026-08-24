package com.xayah.databackup.service.util

import android.content.ContentUris
import android.provider.ContactsContract
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
import com.xayah.databackup.database.entity.Contact
import com.xayah.databackup.database.entity.FieldMap
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper
import kotlinx.coroutines.CancellationException

class RestoreContactsHelper(private val restoreProcessRepo: RestoreProcessRepository) {
    companion object {
        private const val TAG = "RestoreContactsHelper"
    }

    private val moshi = Moshi.Builder().build()

    private fun ensureNotCanceled() {
        if (restoreProcessRepo.mIsCanceled) throw CancellationException("Restore contacts canceled.")
    }

    suspend fun start() {
        val sourcePath = restoreProcessRepo.getSourcePath()
        val configPath = PathHelper.getBackupContactsConfigFilePath(sourcePath)
        if (RemoteRootService.exists(configPath).not()) {
            restoreProcessRepo.updateContactsItem {
                copy { ProcessItem.msg set application.getString(R.string.not_exist) }
            }
            return
        }

        val contacts = runCatching {
            val json = RemoteRootService.readText(configPath)
            moshi.adapter<List<Contact>>().fromJson(json).orEmpty()
        }.onFailure {
            LogHelper.e(TAG, "start", "Failed to parse contacts json.", it)
        }.getOrDefault(emptyList())

        contacts.forEachIndexed { index, contact ->
            ensureNotCanceled()
            restoreProcessRepo.updateContactsItem {
                copy {
                    ProcessItem.currentIndex set index + 1
                    ProcessItem.msg set contact.id.toString()
                    ProcessItem.progress set (index + 1).toFloat() / contacts.size.coerceAtLeast(1)
                }
            }
            restoreContact(contact)
        }

        restoreProcessRepo.updateContactsItem {
            copy {
                ProcessItem.currentIndex set contacts.size
                ProcessItem.msg set application.getString(R.string.finished)
                ProcessItem.progress set 1f
            }
        }
    }

    private fun restoreContact(contact: Contact) {
        val rawContactMap = contact.rawContact?.let { json ->
            runCatching { moshi.adapter<FieldMap>().fromJson(json) }.getOrNull()
        } ?: return
        val dataMaps = contact.data?.let { json ->
            runCatching { moshi.adapter<List<FieldMap>>().fromJson(json) }.getOrNull()
        }.orEmpty()

        val rawContactUri = ContentRestoreHelper.insert(ContactsContract.RawContacts.CONTENT_URI, rawContactMap)
            ?: return
        val rawContactId = ContentUris.parseId(rawContactUri)

        dataMaps.forEach { dataMap ->
            val values = ContentRestoreHelper.toContentValues(dataMap)
            values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
            application.contentResolver.insert(ContactsContract.Data.CONTENT_URI, values)
        }
    }
}
