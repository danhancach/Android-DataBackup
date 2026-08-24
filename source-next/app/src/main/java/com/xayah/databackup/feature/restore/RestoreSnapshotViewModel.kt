package com.xayah.databackup.feature.restore

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.data.RestoreRepository
import com.xayah.databackup.data.rustic.RusticSnapshotRepository
import com.xayah.databackup.entity.RusticSnapshot
import com.xayah.databackup.util.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

open class RestoreSnapshotViewModel(
    private val backupConfigRepo: BackupConfigRepository,
    private val restoreRepo: RestoreRepository,
    private val rusticSnapshotRepo: RusticSnapshotRepository,
) : BaseViewModel() {
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _snapshots = MutableStateFlow<List<RusticSnapshot>>(emptyList())
    val snapshots: StateFlow<List<RusticSnapshot>> = _snapshots.asStateFlow()

    private val _selectedIndex = MutableStateFlow(-1)
    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()

    val nextBtnEnabled: StateFlow<Boolean> = combine(isLoading, selectedIndex, snapshots) { loading, index, items ->
        loading.not() && index >= 0 && index < items.size
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
            val items = rusticSnapshotRepo.listSnapshots(config)
            _snapshots.value = items

            val savedId = restoreRepo.getSelectedSnapshotId()
            val savedIndex = items.indexOfFirst { it.id == savedId }
            _selectedIndex.value = when {
                savedIndex >= 0 -> savedIndex
                items.isNotEmpty() -> 0
                else -> -1
            }
            if (_selectedIndex.value >= 0) {
                restoreRepo.setSelectedSnapshotId(items[_selectedIndex.value].id)
            }
            _isLoading.value = false
        }
    }

    fun selectSnapshot(index: Int) {
        val snapshot = _snapshots.value.getOrNull(index) ?: return
        _selectedIndex.value = index
        withLock(Dispatchers.IO) {
            restoreRepo.setSelectedSnapshotId(snapshot.id)
        }
    }
}
