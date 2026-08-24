package com.xayah.databackup.service.util

import android.net.wifi.WifiConfiguration
import arrow.optics.copy
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.R
import com.xayah.databackup.adapter.WifiConfigurationAdapter
import com.xayah.databackup.data.ProcessItem
import com.xayah.databackup.data.RestoreProcessRepository
import com.xayah.databackup.data.currentIndex
import com.xayah.databackup.data.msg
import com.xayah.databackup.data.progress
import com.xayah.databackup.database.entity.Network
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.PathHelper
import kotlinx.coroutines.CancellationException

class RestoreNetworksHelper(private val restoreProcessRepo: RestoreProcessRepository) {
    companion object {
        private const val TAG = "RestoreNetworksHelper"
    }

    private val moshi = Moshi.Builder().add(WifiConfigurationAdapter()).build()

    private fun ensureNotCanceled() {
        if (restoreProcessRepo.mIsCanceled) throw CancellationException("Restore networks canceled.")
    }

    suspend fun start() {
        val sourcePath = restoreProcessRepo.getSourcePath()
        val configPath = PathHelper.getBackupNetworksConfigFilePath(sourcePath)
        if (RemoteRootService.exists(configPath).not()) {
            restoreProcessRepo.updateNetworksItem {
                copy { ProcessItem.msg set application.getString(R.string.not_exist) }
            }
            return
        }

        val networks = runCatching {
            val json = RemoteRootService.readText(configPath)
            moshi.adapter<List<Network>>().fromJson(json).orEmpty()
        }.onFailure {
            LogHelper.e(TAG, "start", "Failed to parse networks json.", it)
        }.getOrDefault(emptyList())

        val wifiConfigs = networks.flatMap { network ->
            listOfNotNull(network.config1, network.config2).mapNotNull { configJson ->
                runCatching { moshi.adapter<WifiConfiguration>().fromJson(configJson) }.getOrNull()
            }
        }

        wifiConfigs.forEachIndexed { index, config ->
            ensureNotCanceled()
            restoreProcessRepo.updateNetworksItem {
                copy {
                    ProcessItem.currentIndex set index + 1
                    ProcessItem.msg set config.SSID
                    ProcessItem.progress set (index + 1).toFloat() / wifiConfigs.size.coerceAtLeast(1)
                }
            }
        }

        if (wifiConfigs.isNotEmpty()) {
            RemoteRootService.addNetworks(wifiConfigs)
        }

        restoreProcessRepo.updateNetworksItem {
            copy {
                ProcessItem.currentIndex set wifiConfigs.size
                ProcessItem.msg set application.getString(R.string.finished)
                ProcessItem.progress set 1f
            }
        }
    }
}
