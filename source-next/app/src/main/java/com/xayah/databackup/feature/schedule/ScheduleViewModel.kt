package com.xayah.databackup.feature.schedule

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.data.BackupScheduleRepository
import com.xayah.databackup.data.BackupScheduleState
import com.xayah.databackup.util.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow

data class ScheduleUiState(
    val schedule: BackupScheduleState = BackupScheduleState(),
    val backupTargetTitle: String = "",
    val hasSelectedBackup: Boolean = false,
    val isLoaded: Boolean = false,
)

class ScheduleViewModel(
    private val backupScheduleRepository: BackupScheduleRepository,
    private val backupConfigRepository: BackupConfigRepository,
) : BaseViewModel() {
    private val _backupTargetTitle = MutableStateFlow("")
    private val _hasSelectedBackup = MutableStateFlow(false)
    private val _isLoaded = MutableStateFlow(false)

    val uiState: StateFlow<ScheduleUiState> = combine(
        backupScheduleRepository.state,
        _backupTargetTitle,
        _hasSelectedBackup,
        _isLoaded,
    ) { schedule, targetTitle, hasSelectedBackup, isLoaded ->
        ScheduleUiState(
            schedule = schedule,
            backupTargetTitle = targetTitle,
            hasSelectedBackup = hasSelectedBackup,
            isLoaded = isLoaded,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScheduleUiState(),
    )

    private var hasRefreshed = false

    fun refresh() {
        if (hasRefreshed) {
            return
        }
        hasRefreshed = true
        withLock(Dispatchers.IO) {
            backupConfigRepository.loadBackupConfigsFromLocal()
            val config = backupConfigRepository.getCurrentConfig()
            val selectedIndex = backupConfigRepository.selectedIndex.value
            _hasSelectedBackup.value = selectedIndex >= 0
            _backupTargetTitle.value = if (selectedIndex >= 0) config.displayTitle else ""
            _isLoaded.value = true
        }
    }

    fun setEnabled(enabled: Boolean) {
        withLock(Dispatchers.IO) {
            backupScheduleRepository.setEnabled(enabled)
        }
    }

    fun updateTime(hour: Int, minute: Int) {
        withLock(Dispatchers.IO) {
            backupScheduleRepository.updateTime(hour, minute)
        }
    }
}
