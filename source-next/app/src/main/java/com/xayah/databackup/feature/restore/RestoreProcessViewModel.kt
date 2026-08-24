package com.xayah.databackup.feature.restore

import androidx.lifecycle.viewModelScope
import arrow.optics.copy
import arrow.optics.optics
import com.xayah.databackup.data.ProcessAppItem
import com.xayah.databackup.data.ProcessItem
import com.xayah.databackup.data.RestoreProcessRepository
import com.xayah.databackup.data.STATUS_CANCEL
import com.xayah.databackup.data.isFailedStatus
import com.xayah.databackup.util.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class RestoreProcessStatus {
    Processing,
    Canceling,
    Canceled,
    Finished,
}

@optics
data class RestoreProcessUiState(
    val isLoaded: Boolean = false,
    val status: RestoreProcessStatus = RestoreProcessStatus.Processing,
) {
    companion object

    val isProcessing: Boolean = status == RestoreProcessStatus.Processing
    val isCanceling: Boolean = status == RestoreProcessStatus.Canceling
    val isCanceled: Boolean = status == RestoreProcessStatus.Canceled
    val canBeCanceled: Boolean = isLoaded && status == RestoreProcessStatus.Processing
}

open class RestoreProcessViewModel(
    private val restoreProcessRepo: RestoreProcessRepository,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(RestoreProcessUiState())
    val uiState: StateFlow<RestoreProcessUiState> = _uiState.asStateFlow()

    val appsItem: StateFlow<ProcessItem> = restoreProcessRepo.getAppsItem().asStateFlow()
    val allProcessedAppItems = restoreProcessRepo.getProcessAppItems().asStateFlow()
    val failedProcessedAppItems: StateFlow<List<ProcessAppItem>> = allProcessedAppItems
        .map { items -> items.filter { isRestoreAppFailed(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), listOf())
    val succeededProcessedAppItems: StateFlow<List<ProcessAppItem>> = allProcessedAppItems
        .map { items -> items.filter { isRestoreAppSucceeded(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), listOf())

    val networksItem: StateFlow<ProcessItem> = restoreProcessRepo.getNetworksItem().asStateFlow()
    val contactsItem: StateFlow<ProcessItem> = restoreProcessRepo.getContactsItem().asStateFlow()
    val callLogsItem: StateFlow<ProcessItem> = restoreProcessRepo.getCallLogsItem().asStateFlow()
    val messagesItem: StateFlow<ProcessItem> = restoreProcessRepo.getMessagesItem().asStateFlow()

    val overallProgress: StateFlow<String> = combine(
        appsItem,
        networksItem,
        contactsItem,
        callLogsItem,
        messagesItem,
    ) { appsItem, networksItem, contactsItem, callLogsItem, messagesItem ->
        var currentIndex = 0
        var totalCount = 0
        if (appsItem.isSelected) {
            currentIndex += appsItem.currentIndex
            totalCount += appsItem.totalCount
        }
        if (networksItem.isSelected) {
            currentIndex += networksItem.currentIndex
            totalCount += networksItem.totalCount
        }
        if (contactsItem.isSelected) {
            currentIndex += contactsItem.currentIndex
            totalCount += contactsItem.totalCount
        }
        if (callLogsItem.isSelected) {
            currentIndex += callLogsItem.currentIndex
            totalCount += callLogsItem.totalCount
        }
        if (messagesItem.isSelected) {
            currentIndex += messagesItem.currentIndex
            totalCount += messagesItem.totalCount
        }
        if (totalCount != 0) {
            ((currentIndex.toFloat() / totalCount) * 100).roundToInt().toString()
        } else {
            "0"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "0")

    fun updateUiState(onUpdate: RestoreProcessUiState.() -> RestoreProcessUiState) {
        _uiState.update { it.onUpdate() }
    }

    fun cancel() {
        if (uiState.value.canBeCanceled.not()) return
        updateUiState {
            copy {
                RestoreProcessUiState.status set RestoreProcessStatus.Canceling
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            restoreProcessRepo.cancel()
        }
    }

    fun loadProcessItems() {
        withLock(Dispatchers.Default) {
            if (uiState.value.isLoaded.not()) {
                updateUiState {
                    copy {
                        RestoreProcessUiState.isLoaded set true
                        RestoreProcessUiState.status set RestoreProcessStatus.Processing
                    }
                }
                restoreProcessRepo.onStart()
                updateUiState {
                    copy {
                        RestoreProcessUiState.status set if (status == RestoreProcessStatus.Canceling) {
                            RestoreProcessStatus.Canceled
                        } else {
                            RestoreProcessStatus.Finished
                        }
                    }
                }
            }
        }
    }
}

private fun isRestoreAppFailed(appItem: ProcessAppItem): Boolean {
    return listOf(
        appItem.apkItem.details,
        appItem.intDataItem.details,
        appItem.extDataItem.details,
        appItem.addlDataItem.details,
    ).any { details -> details.any { isFailedStatus(it.status) } }
}

private fun isRestoreAppSucceeded(appItem: ProcessAppItem): Boolean {
    return !isRestoreAppFailed(appItem) &&
        listOf(
            appItem.apkItem.details,
            appItem.intDataItem.details,
            appItem.extDataItem.details,
            appItem.addlDataItem.details,
        ).none { details -> details.any { it.status == STATUS_CANCEL } }
}
