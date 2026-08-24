package com.xayah.databackup.feature.backup

import androidx.lifecycle.viewModelScope
import arrow.optics.copy
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.service.util.IntegrityChecker
import com.xayah.databackup.service.util.IntegrityReport
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.entity.name
import com.xayah.databackup.feature.BackupConfigRoute
import com.xayah.databackup.util.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

open class BackupConfigViewModel(
    private val route: BackupConfigRoute,
    private val backupConfigRepo: BackupConfigRepository,
) : BaseViewModel() {
    companion object {
        private val sharingStarted = SharingStarted.WhileSubscribed(5_000)
    }

    private val currentConfig: BackupConfig?
        get() = backupConfigRepo.configs.value.getOrNull(route.index)

    val backupConfig: StateFlow<BackupConfig?> =
        backupConfigRepo.configs.map { configs ->
            configs.getOrNull(route.index)
        }.stateIn(
            scope = viewModelScope,
            initialValue = currentConfig,
            started = sharingStarted,
        )

    fun changeName(name: String) {
        withLock(Dispatchers.Default) {
            currentConfig?.let { config ->
                backupConfigRepo.updateConfig(config.uuidString) {
                    copy {
                        BackupConfig.name set name
                    }
                }
            }
        }
    }

    fun deleteConfig(onDeleted: suspend () -> Unit) {
        withLock(Dispatchers.Default) {
            currentConfig?.let { config ->
                backupConfigRepo.deleteConfig(config.uuidString)
            }
            onDeleted()
        }
    }

    fun selectBackup(onSelected: () -> Unit) {
        withLock(Dispatchers.IO) {
            backupConfigRepo.selectBackup(route.index)
            withContext(Dispatchers.Main) {
                onSelected()
            }
        }
    }

    suspend fun runIntegrityCheck(): IntegrityReport {
        val config = currentConfig ?: return IntegrityReport(emptyList())
        val appsDir = "${config.path}/apps"
        if (RemoteRootService.exists(appsDir).not()) return IntegrityReport(emptyList())
        val issues = RemoteRootService.listFilePaths(appsDir, listFiles = false, listDirs = true)
            .map { it.path }
            .mapNotNull { dirPath ->
                val dirName = PathHelper.getChildPath(dirPath)
                val packageName = dirName.substringAfterLast('_', dirName)
                IntegrityChecker.checkDir(dirPath, dirName, packageName)
            }
        return IntegrityReport(issues)
    }
}
