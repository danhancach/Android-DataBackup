package com.xayah.databackup.feature.restore

import androidx.lifecycle.viewModelScope
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.util.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

sealed interface RestoreHubUiState {
    data object Loading : RestoreHubUiState
    data object Empty : RestoreHubUiState
    data class Content(val backups: List<BackupConfig>) : RestoreHubUiState
}

open class RestoreHubViewModel(
    private val backupConfigRepo: BackupConfigRepository,
) : BaseViewModel() {
    val uiState: StateFlow<RestoreHubUiState> = backupConfigRepo.configs
        .map { configs ->
            when {
                backupConfigRepo.isLoaded.value.not() -> RestoreHubUiState.Loading
                configs.isEmpty() -> RestoreHubUiState.Empty
                else -> RestoreHubUiState.Content(configs)
            }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = RestoreHubUiState.Loading,
            started = SharingStarted.WhileSubscribed(5_000),
        )

    fun initialize() {
        withLock(Dispatchers.IO) {
            backupConfigRepo.loadBackupConfigsFromLocal()
        }
    }

    suspend fun selectBackup(index: Int) {
        withContext(Dispatchers.IO) {
            backupConfigRepo.selectBackup(index)
        }
    }
}
