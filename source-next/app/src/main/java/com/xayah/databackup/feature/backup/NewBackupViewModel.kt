package com.xayah.databackup.feature.backup

import arrow.optics.copy
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.entity.S3CloudConfig
import com.xayah.databackup.entity.backupBackend
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.LogHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class NewBackupUiState(
    val isSaving: Boolean = false,
    val saveError: String? = null,
)

class NewBackupViewModel(
    private val backupConfigRepository: BackupConfigRepository,
) : BaseViewModel() {
    companion object {
        private const val TAG = "NewBackupViewModel"
    }

    private val _uiState = MutableStateFlow(NewBackupUiState())
    val uiState: StateFlow<NewBackupUiState> = _uiState.asStateFlow()

    private val _backupBackend = MutableStateFlow<BackupBackend>(BackupBackend.Archive())
    val backupBackend: StateFlow<BackupBackend> = _backupBackend.asStateFlow()

    fun selectBackupBackend(index: Int) {
        val currentRustic = backupBackend.value as? BackupBackend.Rustic
        val currentPassword = currentRustic?.password ?: BackupBackend.DEFAULT_PASSWORD
        val currentStorage = currentRustic?.storage ?: BackupBackend.RusticStorage()
        _backupBackend.value = when (index) {
            0 -> BackupBackend.Archive()
            else -> BackupBackend.Rustic(password = currentPassword, storage = currentStorage)
        }
    }

    fun toggleRusticCloud(enabled: Boolean) {
        val current = backupBackend.value as? BackupBackend.Rustic ?: return
        _backupBackend.value = if (enabled) {
            val s3 = current.storage.s3 ?: emptyS3Draft()
            current.copy(storage = BackupBackend.RusticStorage(type = BackupBackend.RusticStorage.TYPE_S3, s3 = s3))
        } else {
            current.copy(storage = BackupBackend.RusticStorage(type = BackupBackend.RusticStorage.TYPE_LOCAL))
        }
    }

    fun configureRusticCloud(s3: S3CloudConfig) {
        val current = backupBackend.value as? BackupBackend.Rustic ?: BackupBackend.Rustic()
        _backupBackend.value = current.copy(
            storage = BackupBackend.RusticStorage(type = BackupBackend.RusticStorage.TYPE_S3, s3 = s3),
        )
    }

    fun validateBackupBackend(): String? {
        val rustic = backupBackend.value as? BackupBackend.Rustic ?: return null
        if (rustic.storage.isCloud.not()) return null
        val s3 = rustic.storage.s3 ?: return App.application.getString(R.string.s3_validation_missing)
        if (s3.isConfigured().not()) {
            return App.application.getString(R.string.s3_validation_missing)
        }
        if (s3.allowInsecure.not() && s3.endpoint.startsWith("http://", ignoreCase = true)) {
            return App.application.getString(R.string.s3_validation_http)
        }
        return null
    }

    private fun emptyS3Draft(): S3CloudConfig = S3CloudConfig(
        endpoint = "",
        bucket = "",
        accessKey = "",
        secretKey = "",
    )

    fun changeRusticPassword(password: String) {
        val storage = (backupBackend.value as? BackupBackend.Rustic)?.storage ?: BackupBackend.RusticStorage()
        _backupBackend.value = BackupBackend.Rustic(password = password, storage = storage)
    }

    fun saveNewBackup(onSaved: () -> Unit) {
        withLock(Dispatchers.IO) {
            if (uiState.value.isSaving) return@withLock

            validateBackupBackend()?.let { error ->
                _uiState.value = NewBackupUiState(saveError = error)
                return@withLock
            }

            _uiState.value = NewBackupUiState(isSaving = true)
            try {
                backupConfigRepository.updateNewConfig {
                    copy {
                        BackupConfig.backupBackend set this@NewBackupViewModel.backupBackend.value
                    }
                }
                backupConfigRepository.saveNewBackup()
                try {
                    backupConfigRepository.resetNewBackup()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    LogHelper.w(TAG, "saveNewBackup", "Failed to reset the new backup draft: ${error.message}")
                }
                _uiState.value = NewBackupUiState()
                _backupBackend.value = BackupBackend.Archive()
                withContext(Dispatchers.Main) {
                    onSaved()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                LogHelper.e(TAG, "saveNewBackup", "Failed to save new backup.", error)
                _uiState.value = NewBackupUiState(saveError = error.message.orEmpty())
            }
        }
    }

    fun dismissSaveError() {
        _uiState.update { it.copy(saveError = null) }
    }

    fun discardChanges() {
        if (uiState.value.isSaving.not()) {
            _backupBackend.value = BackupBackend.Archive()
            _uiState.value = NewBackupUiState()
        }
    }
}
