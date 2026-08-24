package com.xayah.databackup.feature.restore.rustic

import androidx.lifecycle.viewModelScope
import arrow.optics.copy
import arrow.optics.optics
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.data.RestoreProcessRepository
import com.xayah.databackup.data.RestoreRepository
import com.xayah.databackup.data.rustic.RusticRestoreCoordinator
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.LogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RusticRestoreStage {
    PrepareSnapshot,
    RestoreApps,
    Finished,
}

@optics
data class RusticRestoreProcessUiState(
    val isLoaded: Boolean = false,
    val stage: RusticRestoreStage = RusticRestoreStage.PrepareSnapshot,
    val isProcessing: Boolean = true,
    val errorMessage: String? = null,
) {
    companion object
}

open class RusticRestoreProcessViewModel(
    private val backupConfigRepo: BackupConfigRepository,
    private val restoreRepo: RestoreRepository,
    private val restoreProcessRepo: RestoreProcessRepository,
    private val rusticRestoreCoordinator: RusticRestoreCoordinator,
) : BaseViewModel() {
    companion object {
        private const val TAG = "RusticRestoreProcessViewModel"
    }

    private var stagingPath: String? = null
    private val _uiState = MutableStateFlow(RusticRestoreProcessUiState())
    val uiState: StateFlow<RusticRestoreProcessUiState> = _uiState.asStateFlow()

    fun updateUiState(onUpdate: RusticRestoreProcessUiState.() -> RusticRestoreProcessUiState) {
        _uiState.update { it.onUpdate() }
    }

    fun start() {
        withLock(Dispatchers.IO) {
            if (_uiState.value.isLoaded) return@withLock
            updateUiState {
                copy {
                    RusticRestoreProcessUiState.isLoaded set true
                    RusticRestoreProcessUiState.stage set RusticRestoreStage.PrepareSnapshot
                    RusticRestoreProcessUiState.isProcessing set true
                }
            }
            try {
                val config = backupConfigRepo.getCurrentConfig()
                val snapshotId = restoreRepo.getSelectedSnapshotId().ifBlank { "latest" }
                val staging = rusticRestoreCoordinator.restoreSnapshot(config, snapshotId)
                stagingPath = staging
                restoreRepo.loadFromStaging(config, staging)
                updateUiState {
                    copy {
                        RusticRestoreProcessUiState.stage set RusticRestoreStage.RestoreApps
                    }
                }
                restoreProcessRepo.onStart()
                updateUiState {
                    copy {
                        RusticRestoreProcessUiState.stage set RusticRestoreStage.Finished
                        RusticRestoreProcessUiState.isProcessing set false
                    }
                }
            } catch (error: Throwable) {
                LogHelper.e(TAG, "start", "Rustic restore failed.", error)
                updateUiState {
                    copy {
                        RusticRestoreProcessUiState.isProcessing set false
                        RusticRestoreProcessUiState.errorMessage set error.message
                    }
                }
            } finally {
                stagingPath?.let { rusticRestoreCoordinator.cleanupStaging(it) }
                stagingPath = null
            }
        }
    }

    fun cancel() {
        viewModelScope.launch(Dispatchers.Default) {
            restoreProcessRepo.cancel()
        }
    }
}
