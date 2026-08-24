package com.xayah.databackup.service.util

import arrow.optics.copy
import arrow.optics.dsl.index
import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.R
import com.xayah.databackup.data.ProcessAppDataDetailItem
import com.xayah.databackup.data.ProcessAppDataItem
import com.xayah.databackup.data.ProcessAppItem
import com.xayah.databackup.data.ProcessItem
import com.xayah.databackup.data.RestoreProcessRepository
import com.xayah.databackup.data.STATUS_CANCEL
import com.xayah.databackup.data.STATUS_ERROR
import com.xayah.databackup.data.STATUS_SKIP
import com.xayah.databackup.data.STATUS_SUCCESS
import com.xayah.databackup.data.addlDataItem
import com.xayah.databackup.data.apkItem
import com.xayah.databackup.data.bytes
import com.xayah.databackup.data.currentIndex
import com.xayah.databackup.data.details
import com.xayah.databackup.data.enabled
import com.xayah.databackup.data.extDataItem
import com.xayah.databackup.data.info
import com.xayah.databackup.data.intDataItem
import com.xayah.databackup.data.msg
import com.xayah.databackup.data.progress
import com.xayah.databackup.data.speed
import com.xayah.databackup.data.status
import com.xayah.databackup.data.subtitle
import com.xayah.databackup.entity.RestoreApp
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.CleanRestoring
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.RootShellHelper
import com.xayah.databackup.util.SymbolHelper
import com.xayah.databackup.util.formatToStorageSize
import com.xayah.databackup.util.readBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class RestoreAppsHelper(private val mRestoreProcessRepo: RestoreProcessRepository) {
    companion object {
        private const val TAG = "RestoreAppsHelper"
    }

    private fun getMsgByStatus(status: Int): String {
        return when (status) {
            STATUS_SUCCESS -> application.getString(R.string.succeed)
            STATUS_SKIP -> application.getString(R.string.skip)
            STATUS_CANCEL -> application.getString(R.string.cancel)
            else -> application.getString(R.string.error)
        }
    }

    private fun getSubtitleByStatus(status: Int, subtitle: String): String {
        return when (status) {
            STATUS_SUCCESS -> subtitle
            STATUS_SKIP -> application.getString(R.string.not_exist)
            STATUS_CANCEL -> application.getString(R.string.cancel)
            else -> subtitle
        }
    }

    private fun getEnabledByStatus(status: Int): Boolean {
        return when (status) {
            STATUS_SUCCESS -> true
            STATUS_SKIP, STATUS_CANCEL -> false
            else -> true
        }
    }

    private fun getFinalStatusByResult(result: List<Pair<Int, String>>): Int {
        return when {
            result.all { it.first == STATUS_SUCCESS } -> STATUS_SUCCESS
            result.all { it.first == STATUS_SKIP } -> STATUS_SKIP
            result.all { it.first == STATUS_CANCEL } -> STATUS_CANCEL
            else -> STATUS_ERROR
        }
    }

    private fun ensureNotCanceled() {
        if (mRestoreProcessRepo.mIsCanceled) {
            throw CancellationException("Restore apps canceled.")
        }
    }

    private suspend fun restoreArchive(
        src: String,
        dst: String,
        clean: Boolean,
    ): Pair<Int, String> {
        ensureNotCanceled()
        if (RemoteRootService.exists(src).not()) {
            return STATUS_SKIP to "Archive not found: $src"
        }
        if (clean && RemoteRootService.exists(dst)) {
            RemoteRootService.deleteRecursively(dst)
        }
        if (RemoteRootService.mkdirs(dst).not()) {
            return STATUS_ERROR to "Failed to mkdirs: $dst"
        }
        val (code, info) = TarHelper.decompress(src, dst)
        return if (code == 0) STATUS_SUCCESS to info else STATUS_ERROR to info
    }

    private suspend fun installApks(userId: Int, packageName: String, apkDir: String): Pair<Int, String> {
        ensureNotCanceled()
        val apkPaths = RemoteRootService.listFilePaths(apkDir, listFiles = true, listDirs = false)
            .map { it.path }
            .filter { it.endsWith(".apk", ignoreCase = true) }
        if (apkPaths.isEmpty()) {
            return STATUS_ERROR to "No APK files in $apkDir"
        }
        return when (apkPaths.size) {
            1 -> {
                val shell = RootShellHelper.executeQuoted(
                    "pm", "install", "--user", userId.toString(), apkPaths.first(),
                )
                if (shell.code == 0) STATUS_SUCCESS to shell.out.joinToString("\n")
                else STATUS_ERROR to shell.out.joinToString("\n")
            }
            else -> {
                val createShell = RootShellHelper.executeQuoted(
                    "pm", "install-create", "--user", userId.toString(),
                )
                val session = createShell.out.lastOrNull()?.trim().orEmpty()
                if (session.isBlank()) {
                    return STATUS_ERROR to "Failed to create install session."
                }
                var success = true
                val out = mutableListOf<String>()
                apkPaths.forEach { apkPath ->
                    val name = PathHelper.getChildPath(apkPath)
                    val sizeShell = RootShellHelper.executeQuoted("stat", "-c", "%s", apkPath)
                    val size = sizeShell.out.firstOrNull()?.toLongOrNull() ?: 0L
                    val writeShell = RootShellHelper.executeQuoted(
                        "pm", "install-write", "-S", size.toString(),
                        session, name, apkPath,
                    )
                    success = success && writeShell.code == 0
                    out.addAll(writeShell.out)
                }
                val commitShell = RootShellHelper.executeQuoted("pm", "install-commit", session)
                success = success && commitShell.code == 0
                out.addAll(commitShell.out)
                if (success) STATUS_SUCCESS to out.joinToString("\n")
                else STATUS_ERROR to out.joinToString("\n")
            }
        }
    }

    private suspend fun restoreApk(app: RestoreApp, sourcePath: String): Pair<Int, String> {
        if (app.hasApk.not()) return STATUS_SKIP to application.getString(R.string.not_exist)
        val apkArchive = PathHelper.getBackupAppsApkFilePath(sourcePath, app.dirName)
        val tmpDir = "${application.cacheDir.path}/restore_apk_${app.packageName}_${System.currentTimeMillis()}"
        RemoteRootService.deleteRecursively(tmpDir)
        RemoteRootService.mkdirs(tmpDir)
        val decompressResult = restoreArchive(apkArchive, tmpDir, clean = true)
        if (decompressResult.first != STATUS_SUCCESS) {
            RemoteRootService.deleteRecursively(tmpDir)
            return decompressResult
        }
        val result = installApks(app.userId, app.packageName, tmpDir)
        RemoteRootService.deleteRecursively(tmpDir)
        return result
    }

    private suspend fun restoreInternalData(app: RestoreApp, sourcePath: String, clean: Boolean): List<Pair<Int, String>> {
        val results = mutableListOf<Pair<Int, String>>()
        if (app.hasInternalData) {
            val userArchive = PathHelper.getBackupAppsUserFilePath(sourcePath, app.dirName)
            if (RemoteRootService.exists(userArchive)) {
                results.add(
                    restoreArchive(
                        userArchive,
                        PathHelper.getAppUserDir(app.userId, app.packageName),
                        clean,
                    )
                )
            } else {
                results.add(STATUS_SKIP to application.getString(R.string.not_exist))
            }
            val userDeArchive = PathHelper.getBackupAppsUserDeFilePath(sourcePath, app.dirName)
            if (RemoteRootService.exists(userDeArchive)) {
                results.add(
                    restoreArchive(
                        userDeArchive,
                        PathHelper.getAppUserDeDir(app.userId, app.packageName),
                        clean,
                    )
                )
            } else {
                results.add(STATUS_SKIP to application.getString(R.string.not_exist))
            }
        } else {
            results.add(STATUS_SKIP to application.getString(R.string.not_selected))
        }
        return results
    }

    private suspend fun restoreExternalData(app: RestoreApp, sourcePath: String, clean: Boolean): Pair<Int, String> {
        if (app.hasExternalData.not()) return STATUS_SKIP to application.getString(R.string.not_selected)
        val archive = PathHelper.getBackupAppsDataFilePath(sourcePath, app.dirName)
        return restoreArchive(
            archive,
            PathHelper.getAppDataDir(app.userId, app.packageName),
            clean,
        )
    }

    private suspend fun restoreAdditionalData(app: RestoreApp, sourcePath: String, clean: Boolean): List<Pair<Int, String>> {
        val results = mutableListOf<Pair<Int, String>>()
        if (app.hasAdditionalData) {
            val obbArchive = PathHelper.getBackupAppsObbFilePath(sourcePath, app.dirName)
            if (RemoteRootService.exists(obbArchive)) {
                results.add(
                    restoreArchive(
                        obbArchive,
                        PathHelper.getAppObbDir(app.userId, app.packageName),
                        clean,
                    )
                )
            } else {
                results.add(STATUS_SKIP to application.getString(R.string.not_exist))
            }
            val mediaArchive = PathHelper.getBackupAppsMediaFilePath(sourcePath, app.dirName)
            if (RemoteRootService.exists(mediaArchive)) {
                results.add(
                    restoreArchive(
                        mediaArchive,
                        PathHelper.getAppMediaDir(app.userId, app.packageName),
                        clean,
                    )
                )
            } else {
                results.add(STATUS_SKIP to application.getString(R.string.not_exist))
            }
        } else {
            results.add(STATUS_SKIP to application.getString(R.string.not_selected))
        }
        return results
    }

  suspend fun start() {
        val apps = mRestoreProcessRepo.getApps()
        val sourcePath = mRestoreProcessRepo.getSourcePath()
        val clean = application.readBoolean(CleanRestoring).first()

        apps.forEachIndexed { index, app ->
            ensureNotCanceled()
            mRestoreProcessRepo.updateAppsItem {
                copy {
                    ProcessItem.currentIndex set index + 1
                    ProcessItem.msg set app.label
                }
            }

            val processAppItem = ProcessAppItem(
                label = app.label,
                packageName = app.packageName,
                userId = app.userId,
            )
            mRestoreProcessRepo.addProcessAppItem(processAppItem)

            // APK
            restoreApk(app, sourcePath).also { (status, info) ->
                mRestoreProcessRepo.updateProcessAppItem {
                    copy {
                        ProcessAppItem.apkItem.enabled set getEnabledByStatus(status)
                        ProcessAppItem.apkItem.subtitle set getSubtitleByStatus(status, apkItem.subtitle)
                        ProcessAppItem.apkItem.msg set getMsgByStatus(status)
                        inside(ProcessAppItem.apkItem.details.index(0)) {
                            ProcessAppDataDetailItem.status set status
                            ProcessAppDataDetailItem.info set info
                        }
                    }
                }
            }

            // Internal data
            restoreInternalData(app, sourcePath, clean).also { results ->
                val finalStatus = getFinalStatusByResult(results)
                mRestoreProcessRepo.updateProcessAppItem {
                    copy {
                        ProcessAppItem.intDataItem.enabled set getEnabledByStatus(finalStatus)
                        ProcessAppItem.intDataItem.subtitle set getSubtitleByStatus(finalStatus, intDataItem.subtitle)
                        ProcessAppItem.intDataItem.msg set getMsgByStatus(finalStatus)
                        results.forEachIndexed { i, (status, info) ->
                            inside(ProcessAppItem.intDataItem.details.index(i)) {
                                ProcessAppDataDetailItem.status set status
                                ProcessAppDataDetailItem.info set info
                            }
                        }
                    }
                }
            }

            // External data
            restoreExternalData(app, sourcePath, clean).also { (status, info) ->
                mRestoreProcessRepo.updateProcessAppItem {
                    copy {
                        ProcessAppItem.extDataItem.enabled set getEnabledByStatus(status)
                        ProcessAppItem.extDataItem.subtitle set getSubtitleByStatus(status, extDataItem.subtitle)
                        ProcessAppItem.extDataItem.msg set getMsgByStatus(status)
                        inside(ProcessAppItem.extDataItem.details.index(0)) {
                            ProcessAppDataDetailItem.status set status
                            ProcessAppDataDetailItem.info set info
                        }
                    }
                }
            }

            // Additional data
            restoreAdditionalData(app, sourcePath, clean).also { results ->
                val finalStatus = getFinalStatusByResult(results)
                mRestoreProcessRepo.updateProcessAppItem {
                    copy {
                        ProcessAppItem.addlDataItem.enabled set getEnabledByStatus(finalStatus)
                        ProcessAppItem.addlDataItem.subtitle set getSubtitleByStatus(finalStatus, addlDataItem.subtitle)
                        ProcessAppItem.addlDataItem.msg set getMsgByStatus(finalStatus)
                        results.forEachIndexed { i, (status, info) ->
                            inside(ProcessAppItem.addlDataItem.details.index(i)) {
                                ProcessAppDataDetailItem.status set status
                                ProcessAppDataDetailItem.info set info
                            }
                        }
                    }
                }
            }

            mRestoreProcessRepo.updateAppsItem {
                copy {
                    ProcessItem.progress set (index + 1).toFloat() / apps.size.toFloat()
                }
            }
        }
    }
}
