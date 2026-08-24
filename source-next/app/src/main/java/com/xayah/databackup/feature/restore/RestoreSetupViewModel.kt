package com.xayah.databackup.feature.restore

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.data.RestoreProcessRepository
import com.xayah.databackup.data.RestoreRepository
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.feature.backup.MaxSelectedItems
import com.xayah.databackup.feature.backup.TargetItem
import com.xayah.databackup.util.AppsOptionSelectedRestore
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.CallLogsOptionSelectedRestore
import com.xayah.databackup.util.ContactsOptionSelectedRestore
import com.xayah.databackup.util.MessagesOptionSelectedRestore
import com.xayah.databackup.util.NetworksOptionSelectedRestore
import com.xayah.databackup.util.combine
import com.xayah.databackup.util.readBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

open class RestoreSetupViewModel(
    private val backupConfigRepo: BackupConfigRepository,
    private val restoreRepo: RestoreRepository,
    private val restoreProcessRepo: RestoreProcessRepository,
) : BaseViewModel() {
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val selectedBackup: StateFlow<BackupConfig?> = combine(
        backupConfigRepo.configs,
        backupConfigRepo.selectedIndex,
    ) { configs, selectedIndex ->
        configs.getOrNull(selectedIndex)
    }.stateIn(
        scope = viewModelScope,
        initialValue = backupConfigRepo.configs.value.getOrNull(backupConfigRepo.selectedIndex.value),
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val appsItem: StateFlow<TargetItem?> = combine(
        restoreRepo.apps,
        restoreRepo.isLoading,
        application.readBoolean(AppsOptionSelectedRestore),
    ) { apps, loading, appsSelected ->
        if (loading) null
        else TargetItem(
            selected = appsSelected,
            selections = apps.count { it.selected } to apps.size,
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = null,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val networksItem: StateFlow<TargetItem?> = combine(
        restoreRepo.isLoading,
        application.readBoolean(NetworksOptionSelectedRestore),
        selectedBackup,
    ) { loading, selected, backup ->
        if (loading) null
        else structuredTargetItem(
            backup = backup,
            selected = selected,
            hasData = restoreRepo.hasNetworks(),
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = null,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val contactsItem: StateFlow<TargetItem?> = combine(
        restoreRepo.isLoading,
        application.readBoolean(ContactsOptionSelectedRestore),
        selectedBackup,
    ) { loading, selected, backup ->
        if (loading) null
        else structuredTargetItem(
            backup = backup,
            selected = selected,
            hasData = restoreRepo.hasContacts(),
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = null,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val callLogsItem: StateFlow<TargetItem?> = combine(
        restoreRepo.isLoading,
        application.readBoolean(CallLogsOptionSelectedRestore),
        selectedBackup,
    ) { loading, selected, backup ->
        if (loading) null
        else structuredTargetItem(
            backup = backup,
            selected = selected,
            hasData = restoreRepo.hasCallLogs(),
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = null,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val messagesItem: StateFlow<TargetItem?> = combine(
        restoreRepo.isLoading,
        application.readBoolean(MessagesOptionSelectedRestore),
        selectedBackup,
    ) { loading, selected, backup ->
        if (loading) null
        else structuredTargetItem(
            backup = backup,
            selected = selected,
            hasData = restoreRepo.hasMessages(),
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = null,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val selectedItems: StateFlow<Pair<Int, Int>?> = combine(
        appsItem,
        networksItem,
        contactsItem,
        callLogsItem,
        messagesItem,
    ) { appsItem, networksItem, contactsItem, callLogsItem, messagesItem ->
        if (appsItem == null || networksItem == null || contactsItem == null || callLogsItem == null || messagesItem == null) {
            return@combine null
        }
        var count = 0
        if (appsItem.selected && appsItem.selections.first > 0) count++
        if (networksItem.selected && networksItem.selections.first > 0) count++
        if (contactsItem.selected && contactsItem.selections.first > 0) count++
        if (callLogsItem.selected && callLogsItem.selections.first > 0) count++
        if (messagesItem.selected && messagesItem.selections.first > 0) count++
        count to MaxSelectedItems
    }.stateIn(
        scope = viewModelScope,
        initialValue = null,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val nextBtnEnabled = combine(isLoading, selectedItems, selectedBackup) { loading, selectedItems, backup ->
        loading.not() && selectedItems?.first != 0 && backup != null
    }.stateIn(
        scope = viewModelScope,
        initialValue = false,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    fun initialize() {
        withLock(Dispatchers.IO) {
            _isLoading.value = true
            backupConfigRepo.loadBackupConfigsFromLocal()
            val config = backupConfigRepo.getCurrentConfig()
            restoreRepo.loadFromConfig(config)
            _isLoading.value = false
        }
    }

    fun resetProcessRepo() {
        restoreProcessRepo.reset()
    }

    fun isCurrentBackupRustic(): Boolean {
        return selectedBackup.value?.backupBackend is BackupBackend.Rustic
    }

    private fun structuredTargetItem(
        backup: BackupConfig?,
        selected: Boolean,
        hasData: Boolean,
    ): TargetItem {
        val rustic = backup?.backupBackend is BackupBackend.Rustic
        val available = hasData || rustic
        return TargetItem(
            selected = selected && available,
            selections = if (available) 1 to 1 else 0 to 0,
        )
    }
}
