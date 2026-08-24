package com.xayah.databackup.data

import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.R
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.entity.RestoreApp
import com.xayah.databackup.service.RestoreService
import com.xayah.databackup.util.AppsOptionSelectedRestore
import com.xayah.databackup.util.CallLogsOptionSelectedRestore
import com.xayah.databackup.util.ContactsOptionSelectedRestore
import com.xayah.databackup.util.MessagesOptionSelectedRestore
import com.xayah.databackup.util.NetworksOptionSelectedRestore
import com.xayah.databackup.util.ShellHelper
import com.xayah.databackup.util.readBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

class RestoreProcessRepository(
    private val mRestoreRepo: RestoreRepository,
) {
    companion object {
        private const val TAG = "RestoreProcessRepository"
    }

    var mIsCanceled: Boolean = false
        private set

    private var _backupConfig: BackupConfig = BackupConfig()
    private var _sourcePath: String = ""

    private var _appsItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var _apps: List<RestoreApp> = listOf()
    private var _processAppItems: MutableStateFlow<List<ProcessAppItem>> = MutableStateFlow(listOf())

    private var _networksItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var _contactsItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var _callLogsItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())
    private var _messagesItem: MutableStateFlow<ProcessItem> = MutableStateFlow(ProcessItem())

    suspend fun loadAppsProcessItems() {
        _apps = mRestoreRepo.getSelectedApps()
        _appsItem.update {
            it.copy(
                isLoading = false,
                isSelected = application.readBoolean(AppsOptionSelectedRestore).first(),
                currentIndex = 0,
                totalCount = _apps.size,
                progress = 0f,
            )
        }
    }

    suspend fun loadOtherProcessItems() {
        val sourcePath = _sourcePath
        _networksItem.update {
            it.copy(
                isLoading = false,
                isSelected = application.readBoolean(NetworksOptionSelectedRestore).first() &&
                    mRestoreRepo.hasNetworks(),
                totalCount = if (mRestoreRepo.hasNetworks()) 1 else 0,
            )
        }
        _contactsItem.update {
            it.copy(
                isLoading = false,
                isSelected = application.readBoolean(ContactsOptionSelectedRestore).first() &&
                    mRestoreRepo.hasContacts(),
                totalCount = if (mRestoreRepo.hasContacts()) 1 else 0,
            )
        }
        _callLogsItem.update {
            it.copy(
                isLoading = false,
                isSelected = application.readBoolean(CallLogsOptionSelectedRestore).first() &&
                    mRestoreRepo.hasCallLogs(),
                totalCount = if (mRestoreRepo.hasCallLogs()) 1 else 0,
            )
        }
        _messagesItem.update {
            it.copy(
                isLoading = false,
                isSelected = application.readBoolean(MessagesOptionSelectedRestore).first() &&
                    mRestoreRepo.hasMessages(),
                totalCount = if (mRestoreRepo.hasMessages()) 1 else 0,
            )
        }
    }

    private suspend fun loadProcessItems() {
        clearProcessAppItems()
        loadAppsProcessItems()
        loadOtherProcessItems()
    }

    suspend fun cancel() {
        if (mIsCanceled.not()) {
            mIsCanceled = true
            ShellHelper.killRootService()
        }
    }

    suspend fun onStart() {
        mIsCanceled = false
        _backupConfig = mRestoreRepo.getBackupConfig()
        _sourcePath = mRestoreRepo.getSourcePath()
        loadProcessItems()
        RestoreService.start()
    }

    fun getBackupConfig(): BackupConfig = _backupConfig

    fun getSourcePath(): String = _sourcePath

    fun getAppsItem(): MutableStateFlow<ProcessItem> = _appsItem

    fun getNetworksItem(): MutableStateFlow<ProcessItem> = _networksItem

    fun getContactsItem(): MutableStateFlow<ProcessItem> = _contactsItem

    fun getCallLogsItem(): MutableStateFlow<ProcessItem> = _callLogsItem

    fun getMessagesItem(): MutableStateFlow<ProcessItem> = _messagesItem

    fun getApps(): List<RestoreApp> = _apps

    fun reset() {
        updateAppsItem { ProcessItem() }
        updateNetworksItem { ProcessItem() }
        updateContactsItem { ProcessItem() }
        updateCallLogsItem { ProcessItem() }
        updateMessagesItem { ProcessItem() }
        clearProcessAppItems()
    }

    fun clearProcessAppItems() {
        _processAppItems.value = listOf()
    }

    fun getProcessAppItems(): MutableStateFlow<List<ProcessAppItem>> = _processAppItems

    fun updateAppsItem(onUpdate: ProcessItem.() -> ProcessItem) {
        _appsItem.value = onUpdate(_appsItem.value)
    }

    fun updateNetworksItem(onUpdate: ProcessItem.() -> ProcessItem) {
        _networksItem.value = onUpdate(_networksItem.value)
    }

    fun updateContactsItem(onUpdate: ProcessItem.() -> ProcessItem) {
        _contactsItem.value = onUpdate(_contactsItem.value)
    }

    fun updateCallLogsItem(onUpdate: ProcessItem.() -> ProcessItem) {
        _callLogsItem.value = onUpdate(_callLogsItem.value)
    }

    fun updateMessagesItem(onUpdate: ProcessItem.() -> ProcessItem) {
        _messagesItem.value = onUpdate(_messagesItem.value)
    }

    fun addProcessAppItem(item: ProcessAppItem) {
        _processAppItems.update { it + item }
    }

    fun updateProcessAppItem(onUpdate: ProcessAppItem.() -> ProcessAppItem) {
        val currentList = _processAppItems.value
        _processAppItems.value = currentList.mapIndexed { index, item ->
            if (index == currentList.size - 1) onUpdate(item) else item
        }
    }
}
