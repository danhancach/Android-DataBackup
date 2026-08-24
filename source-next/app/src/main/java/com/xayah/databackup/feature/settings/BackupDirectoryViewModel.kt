package com.xayah.databackup.feature.settings

import com.xayah.databackup.App
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.data.BackupDirectoryHelper
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.KeyBackupPath
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.saveString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

data class BackupDirectoryUiState(
    val isLoading: Boolean = true,
    val selectedPath: String = PathHelper.DEFAULT_BACKUP_PATH,
    val directories: List<String> = emptyList(),
    val errorMessage: String? = null,
)

class BackupDirectoryViewModel(
    private val backupConfigRepository: BackupConfigRepository,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(BackupDirectoryUiState())
    val uiState: StateFlow<BackupDirectoryUiState> = _uiState.asStateFlow()

    fun loadDirectories() {
        withLock(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val selectedPath = PathHelper.getBackupPath().first()
            val directories = BackupDirectoryHelper.discover(selectedPath)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    selectedPath = selectedPath,
                    directories = directories,
                )
            }
        }
    }

    fun selectDirectory(path: String) {
        withLock(Dispatchers.IO) {
            val normalized = path.trim().trimEnd('/')
            if (normalized.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "Path is required.") }
                return@withLock
            }
            if (RemoteRootService.exists(normalized).not() && RemoteRootService.mkdirs(normalized).not()) {
                _uiState.update { it.copy(errorMessage = "Failed to access directory: $normalized") }
                return@withLock
            }
            App.application.saveString(KeyBackupPath, normalized)
            backupConfigRepository.loadBackupConfigsFromLocal()
            _uiState.update {
                it.copy(
                    selectedPath = normalized,
                    directories = BackupDirectoryHelper.discover(normalized),
                    errorMessage = null,
                )
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
