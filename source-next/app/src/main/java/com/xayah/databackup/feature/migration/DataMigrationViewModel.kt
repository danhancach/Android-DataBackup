package com.xayah.databackup.feature.migration

import android.net.Uri
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.data.migration.MigrationAppItem
import com.xayah.databackup.data.migration.MigrationRepository
import com.xayah.databackup.util.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DataMigrationUiState(
    val apps: List<MigrationAppItem> = emptyList(),
    val selected: Set<String> = emptySet(),
    val parsedApps: List<String> = emptyList(),
    val lastSha256: String? = null,
    val message: String? = null,
    val isBusy: Boolean = false,
)

class DataMigrationViewModel(
    private val migrationRepository: MigrationRepository,
    private val backupConfigRepository: BackupConfigRepository,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(DataMigrationUiState())
    val uiState: StateFlow<DataMigrationUiState> = _uiState.asStateFlow()

    suspend fun loadApps() {
        val config = backupConfigRepository.getCurrentConfig()
        val apps = migrationRepository.listAppsInBackup(config.path)
        _uiState.update { it.copy(apps = apps) }
    }

    fun toggle(dirName: String) {
        _uiState.update { state ->
            val selected = state.selected.toMutableSet()
            if (selected.contains(dirName)) selected.remove(dirName) else selected.add(dirName)
            state.copy(selected = selected)
        }
    }

    fun export(uri: Uri) {
        withLock(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, message = null) }
            runCatching {
                val config = backupConfigRepository.getCurrentConfig()
                val result = migrationRepository.exportSelectedApps(config.path, _uiState.value.selected, uri)
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        lastSha256 = result.sha256,
                        message = "Export completed.",
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isBusy = false, message = error.message) }
            }
        }
    }

    fun parse(uri: Uri, expectedSha256: String?) {
        withLock(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, message = null) }
            runCatching {
                val apps = migrationRepository.parseMigrationPackage(uri, expectedSha256)
                _uiState.update { it.copy(isBusy = false, parsedApps = apps) }
            }.onFailure { error ->
                _uiState.update { it.copy(isBusy = false, message = error.message, parsedApps = emptyList()) }
            }
        }
    }

    fun importParsed() {
        withLock(Dispatchers.IO) {
            _uiState.update { it.copy(isBusy = true, message = null) }
            runCatching {
                val config = backupConfigRepository.getCurrentConfig()
                migrationRepository.importMigrationPackage(config.path)
                loadApps()
                _uiState.update { it.copy(isBusy = false, message = "Import completed.", parsedApps = emptyList()) }
            }.onFailure { error ->
                _uiState.update { it.copy(isBusy = false, message = error.message) }
            }
        }
    }
}
