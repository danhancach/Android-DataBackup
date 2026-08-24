package com.xayah.databackup.data

import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.entity.RestoreApp
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.RusticRestoreSnapshotId
import com.xayah.databackup.util.readString
import com.xayah.databackup.util.saveString
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RestoreRepository {
    companion object {
        private const val TAG = "RestoreRepository"
    }

    private var _sourcePath: String = ""
    private var _backupConfig: BackupConfig = BackupConfig()
    private val _apps = MutableStateFlow<List<RestoreApp>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    private var _hasNetworks = false
    private var _hasContacts = false
    private var _hasCallLogs = false
    private var _hasMessages = false

    val apps: StateFlow<List<RestoreApp>> = _apps.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun getSourcePath(): String = _sourcePath

    fun getBackupConfig(): BackupConfig = _backupConfig

    suspend fun loadFromConfig(config: BackupConfig) {
        _backupConfig = config
        _sourcePath = config.path
        loadAppsFromSource(_sourcePath)
    }

    suspend fun loadFromStaging(config: BackupConfig, stagingPath: String) {
        _backupConfig = config
        _sourcePath = stagingPath
        loadAppsFromSource(stagingPath)
    }

    private suspend fun loadAppsFromSource(sourcePath: String) {
        _isLoading.value = true
        try {
            val appsDir = "${sourcePath.trimEnd('/')}/apps"
            if (RemoteRootService.exists(appsDir).not()) {
                _apps.value = emptyList()
                return
            }
            val items = RemoteRootService.listFilePaths(appsDir, listFiles = false, listDirs = true)
                .map { it.path }
                .mapNotNull { dirPath ->
                    val dirName = PathHelper.getChildPath(dirPath)
                    if (dirName.isBlank()) return@mapNotNull null
                    val packageName = dirName.substringAfterLast('_', dirName)
                    val label = dirName.substringBeforeLast('_', dirName).ifBlank { packageName }
                    RestoreApp(
                        dirName = dirName,
                        label = label,
                        packageName = packageName,
                        hasApk = RemoteRootService.exists(PathHelper.getBackupAppsApkFilePath(sourcePath, dirName)),
                        hasInternalData = RemoteRootService.exists(PathHelper.getBackupAppsUserFilePath(sourcePath, dirName)) ||
                            RemoteRootService.exists(PathHelper.getBackupAppsUserDeFilePath(sourcePath, dirName)),
                        hasExternalData = RemoteRootService.exists(PathHelper.getBackupAppsDataFilePath(sourcePath, dirName)),
                        hasAdditionalData = RemoteRootService.exists(PathHelper.getBackupAppsObbFilePath(sourcePath, dirName)) ||
                            RemoteRootService.exists(PathHelper.getBackupAppsMediaFilePath(sourcePath, dirName)),
                    )
                }
                .sortedBy { it.label.lowercase() }
            _apps.value = items
            _hasNetworks = RemoteRootService.exists(PathHelper.getBackupNetworksConfigFilePath(sourcePath))
            _hasContacts = RemoteRootService.exists(PathHelper.getBackupContactsConfigFilePath(sourcePath))
            _hasCallLogs = RemoteRootService.exists(PathHelper.getBackupCallLogsConfigFilePath(sourcePath))
            _hasMessages = RemoteRootService.exists(PathHelper.getBackupMessagesSmsConfigFilePath(sourcePath)) ||
                RemoteRootService.exists(PathHelper.getBackupMessagesMmsConfigFilePath(sourcePath))
            LogHelper.i(TAG, "loadAppsFromSource", "Loaded ${items.size} apps from $sourcePath")
        } finally {
            _isLoading.value = false
        }
    }

    fun setAppSelected(dirName: String, selected: Boolean) {
        _apps.update { apps ->
            apps.map { if (it.dirName == dirName) it.copy(selected = selected) else it }
        }
    }

    fun selectAll(selected: Boolean) {
        _apps.update { apps -> apps.map { it.copy(selected = selected) } }
    }

    fun getSelectedApps(): List<RestoreApp> = _apps.value.filter { it.selected }

    fun hasNetworks(): Boolean = _hasNetworks

    fun hasContacts(): Boolean = _hasContacts

    fun hasCallLogs(): Boolean = _hasCallLogs

    fun hasMessages(): Boolean = _hasMessages

    fun isRusticBackend(): Boolean = _backupConfig.backupBackend is BackupBackend.Rustic

    suspend fun getSelectedSnapshotId(): String {
        return application.readString(RusticRestoreSnapshotId).first()
    }

    suspend fun setSelectedSnapshotId(snapshotId: String) {
        application.saveString(RusticRestoreSnapshotId.first, snapshotId)
    }

    fun reset() {
        _sourcePath = ""
        _backupConfig = BackupConfig()
        _apps.value = emptyList()
        _isLoading.value = false
        _hasNetworks = false
        _hasContacts = false
        _hasCallLogs = false
        _hasMessages = false
    }
}
