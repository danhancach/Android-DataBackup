package com.xayah.databackup.feature

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object MainNavigationRoute : NavKey

@Serializable
data object UpdatesRoute : NavKey

@Serializable
data object AboutRoute : NavKey

@Serializable
data object TranslatorsRoute : NavKey

@Serializable
data object BackupLibraryRoute : NavKey

@Serializable
data object BackupSetupRoute : NavKey

@Serializable
data object BackupProcessRoute : NavKey

@Serializable
data object RusticBackupProcessRoute : NavKey

@Serializable
data object BackupProcessDetailsRoute : NavKey

@Serializable
data class BackupConfigRoute(val index: Int) : NavKey {
    init {
        require(index >= 0) { "BackupConfigRoute only accepts an existing backup index." }
    }
}

@Serializable
data object BackupAppsRoute : NavKey

@Serializable
data object BackupNetworksRoute : NavKey

@Serializable
data object BackupContactsRoute : NavKey

@Serializable
data object BackupCallLogsRoute : NavKey

@Serializable
data object BackupMessagesRoute : NavKey

@Serializable
data object DataMigrationRoute : NavKey

@Serializable
data object AdvancedSettingsRoute : NavKey

@Serializable
data object AppearanceSettingsRoute : NavKey

@Serializable
data object BackupSettingsRoute : NavKey

@Serializable
data object BackupDirectoryRoute : NavKey

@Serializable
data object RestoreSettingsRoute : NavKey

@Serializable
data object RestoreRoute : NavKey

@Serializable
data object RestoreSetupRoute : NavKey

@Serializable
data object RestoreProcessRoute : NavKey

@Serializable
data object RestoreSnapshotRoute : NavKey

@Serializable
data object RusticRestoreProcessRoute : NavKey

@Serializable
data object RestoreAppsRoute : NavKey
