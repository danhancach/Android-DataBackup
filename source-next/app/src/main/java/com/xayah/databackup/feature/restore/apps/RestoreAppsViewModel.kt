package com.xayah.databackup.feature.restore.apps

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.data.RestoreRepository
import com.xayah.databackup.entity.RestoreApp
import com.xayah.databackup.util.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

open class RestoreAppsViewModel(
    private val restoreRepo: RestoreRepository,
) : BaseViewModel() {
    val apps: StateFlow<List<RestoreApp>> = restoreRepo.apps
    val selectedCount: StateFlow<Int> = restoreRepo.apps
        .map { apps -> apps.count { it.selected } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun toggleSelection(dirName: String, selected: Boolean) {
        withLock(Dispatchers.Default) {
            restoreRepo.setAppSelected(dirName, selected)
        }
    }

    fun selectAll(selected: Boolean) {
        withLock(Dispatchers.Default) {
            restoreRepo.selectAll(selected)
        }
    }
}
