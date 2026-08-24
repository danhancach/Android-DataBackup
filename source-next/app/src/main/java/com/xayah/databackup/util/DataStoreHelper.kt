package com.xayah.databackup.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class SortsType {
    A2Z,
    DATA_SIZE,
    INSTALL_TIME,
    UPDATE_TIME,
}

enum class SortsSequence {
    ASCENDING,
    DESCENDING
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
suspend fun Context.preloadingDataStore() = dataStore.data.first()

fun Context.readString(pair: Pair<Preferences.Key<String>, String>) = dataStore.data.map { preferences -> preferences[pair.first] ?: pair.second }
fun Context.readThemeType() = readString(ThemeTypeSetting).map { ThemeType.fromStored(it) }
inline fun <reified T : Enum<T>> Context.readEnum(pair: Pair<Preferences.Key<String>, T>) =
    dataStore.data.map { preferences -> enumValueOf<T>(preferences[pair.first] ?: pair.second.name) }
fun Context.readBoolean(pair: Pair<Preferences.Key<Boolean>, Boolean>) = dataStore.data.map { preferences -> preferences[pair.first] ?: pair.second }
fun Context.readInt(pair: Pair<Preferences.Key<Int>, Int>) = dataStore.data.map { preferences -> preferences[pair.first] ?: pair.second }
fun Context.readLong(pair: Pair<Preferences.Key<Long>, Long>) = dataStore.data.map { preferences -> preferences[pair.first] ?: pair.second }

suspend fun Context.saveString(key: Preferences.Key<String>, value: String) = dataStore.edit { settings -> settings[key] = value }
suspend fun Context.saveThemeType(value: ThemeType) = saveString(KeyThemeType, value.name)
suspend inline fun <reified T : Enum<T>> Context.saveEnum(key: Preferences.Key<String>, value: T) =
    dataStore.edit { settings -> settings[key] = value.name }
suspend fun Context.saveBoolean(key: Preferences.Key<Boolean>, value: Boolean) = dataStore.edit { settings -> settings[key] = value }
suspend fun Context.saveInt(key: Preferences.Key<Int>, value: Int) = dataStore.edit { settings -> settings[key] = value }
suspend fun Context.saveLong(key: Preferences.Key<Long>, value: Long) = dataStore.edit { settings -> settings[key] = value }

// ----------------------------------------------------------------------------------------------------------------------------Setup

// Key to defValue
val KeyBackupPath = stringPreferencesKey("backup_path")
val BackupPath = Pair(KeyBackupPath, PathHelper.DEFAULT_BACKUP_PATH)

val KeyThemeType = stringPreferencesKey("theme_type")
val ThemeTypeSetting = Pair(KeyThemeType, ThemeType.AUTO.name)

val KeyFirstLaunch = booleanPreferencesKey("first_launch")
const val DefFirstLaunch = true
val FirstLaunch = Pair(KeyFirstLaunch, DefFirstLaunch)

val KeyCustomSuFile = stringPreferencesKey("custom_su_file")
const val DefCustomSuFile = "su"
val CustomSuFile = Pair(KeyCustomSuFile, DefCustomSuFile)

// ----------------------------------------------------------------------------------------------------------------------------List

val KeyFilterBackupUser = intPreferencesKey("filter_backup_user")
const val DefFilterBackupUser = 0
val FilterBackupUser = Pair(KeyFilterBackupUser, DefFilterBackupUser)

val KeySortsTypeBackup = stringPreferencesKey("sorts_type_backup")
val DefSortsTypeBackup = SortsType.A2Z
val SortsTypeBackup = Pair(KeySortsTypeBackup, DefSortsTypeBackup)

val KeySortsSequenceBackup = stringPreferencesKey("sorts_sequence_backup")
val DefSortsSequenceBackup = SortsSequence.ASCENDING
val SortsSequenceBackup = Pair(KeySortsSequenceBackup, DefSortsSequenceBackup)

val KeyFiltersUserAppsBackup = booleanPreferencesKey("filters_user_apps_backup")
const val DefFiltersUserAppsBackup = true
val FiltersUserAppsBackup = Pair(KeyFiltersUserAppsBackup, DefFiltersUserAppsBackup)

val KeyFiltersSystemAppsBackup = booleanPreferencesKey("filters_system_apps_backup")
const val DefFiltersSystemAppsBackup = false
val FiltersSystemAppsBackup = Pair(KeyFiltersSystemAppsBackup, DefFiltersSystemAppsBackup)

val KeySortsSelectedFirstBackup = booleanPreferencesKey("sorts_selected_first_backup")
const val DefSortsSelectedFirstBackup = false
val SortsSelectedFirstBackup = Pair(KeySortsSelectedFirstBackup, DefSortsSelectedFirstBackup)

// ----------------------------------------------------------------------------------------------------------------------------Settings

val KeyAutoScreenOff = booleanPreferencesKey("auto_screen_off")
const val DefAutoScreenOff = true
val AutoScreenOff = Pair(KeyAutoScreenOff, DefAutoScreenOff)

val KeyResetBackupList = booleanPreferencesKey("reset_backup_list")
const val DefResetBackupList = false
val ResetBackupList = Pair(KeyResetBackupList, DefResetBackupList)

val KeyResetRestoreList = booleanPreferencesKey("reset_restore_list")
const val DefResetRestoreList = false
val ResetRestoreList = Pair(KeyResetRestoreList, DefResetRestoreList)

val KeyLastRestoreTime = longPreferencesKey("last_restore_time")
const val DefLastRestoreTime = 0L
val LastRestoreTime = Pair(KeyLastRestoreTime, DefLastRestoreTime)

val KeyDynamicColor = booleanPreferencesKey("dynamic_color")
const val DefDynamicColor = true
val DynamicColor = Pair(KeyDynamicColor, DefDynamicColor)

val KeyCleanRestoring = booleanPreferencesKey("clean_restoring")
const val DefCleanRestoring = false
val CleanRestoring = Pair(KeyCleanRestoring, DefCleanRestoring)

val KeyRestorePermissions = booleanPreferencesKey("restore_permissions")
const val DefRestorePermissions = true
val RestorePermissions = Pair(KeyRestorePermissions, DefRestorePermissions)

val KeyRestoreSsaid = booleanPreferencesKey("restore_ssaid")
const val DefRestoreSsaid = true
val RestoreSsaid = Pair(KeyRestoreSsaid, DefRestoreSsaid)

// ----------------------------------------------------------------------------------------------------------------------------Backup

val KeyAppsOptionSelectedBackup = booleanPreferencesKey("apps_option_selected_backup")
const val DefAppsOptionSelectedBackup = true
val AppsOptionSelectedBackup = Pair(KeyAppsOptionSelectedBackup, DefAppsOptionSelectedBackup)

val KeyNetworksOptionSelectedBackup = booleanPreferencesKey("networks_option_selected_backup")
const val DefNetworksOptionSelectedBackup = true
val NetworksOptionSelectedBackup = Pair(KeyNetworksOptionSelectedBackup, DefNetworksOptionSelectedBackup)

val KeyContactsOptionSelectedBackup = booleanPreferencesKey("contacts_option_selected_backup")
const val DefContactsOptionSelectedBackup = true
val ContactsOptionSelectedBackup = Pair(KeyContactsOptionSelectedBackup, DefContactsOptionSelectedBackup)

val KeyCallLogsOptionSelectedBackup = booleanPreferencesKey("call_logs_option_selected_backup")
const val DefCallLogsOptionSelectedBackup = true
val CallLogsOptionSelectedBackup = Pair(KeyCallLogsOptionSelectedBackup, DefCallLogsOptionSelectedBackup)

val KeyMessagesOptionSelectedBackup = booleanPreferencesKey("messages_option_selected_backup")
const val DefMessagesOptionSelectedBackup = true
val MessagesOptionSelectedBackup = Pair(KeyMessagesOptionSelectedBackup, DefMessagesOptionSelectedBackup)

val KeyAppsOptionSelectedRestore = booleanPreferencesKey("apps_option_selected_restore")
const val DefAppsOptionSelectedRestore = true
val AppsOptionSelectedRestore = Pair(KeyAppsOptionSelectedRestore, DefAppsOptionSelectedRestore)

val KeyNetworksOptionSelectedRestore = booleanPreferencesKey("networks_option_selected_restore")
const val DefNetworksOptionSelectedRestore = false
val NetworksOptionSelectedRestore = Pair(KeyNetworksOptionSelectedRestore, DefNetworksOptionSelectedRestore)

val KeyContactsOptionSelectedRestore = booleanPreferencesKey("contacts_option_selected_restore")
const val DefContactsOptionSelectedRestore = false
val ContactsOptionSelectedRestore = Pair(KeyContactsOptionSelectedRestore, DefContactsOptionSelectedRestore)

val KeyCallLogsOptionSelectedRestore = booleanPreferencesKey("call_logs_option_selected_restore")
const val DefCallLogsOptionSelectedRestore = false
val CallLogsOptionSelectedRestore = Pair(KeyCallLogsOptionSelectedRestore, DefCallLogsOptionSelectedRestore)

val KeyMessagesOptionSelectedRestore = booleanPreferencesKey("messages_option_selected_restore")
const val DefMessagesOptionSelectedRestore = false
val MessagesOptionSelectedRestore = Pair(KeyMessagesOptionSelectedRestore, DefMessagesOptionSelectedRestore)

val KeyRusticRestoreSnapshotId = stringPreferencesKey("rustic_restore_snapshot_id")
const val DefRusticRestoreSnapshotId = ""
val RusticRestoreSnapshotId = Pair(KeyRusticRestoreSnapshotId, DefRusticRestoreSnapshotId)

val KeyBackupConfigSelectedUuid = stringPreferencesKey("backup_config_selected_uuid")
const val DefBackupConfigSelectedUuid = ""
val BackupConfigSelectedUuid = Pair(KeyBackupConfigSelectedUuid, DefBackupConfigSelectedUuid)

// ----------------------------------------------------------------------------------------------------------------------------Schedule

val KeyScheduleEnabled = booleanPreferencesKey("schedule_enabled")
const val DefScheduleEnabled = false
val ScheduleEnabled = Pair(KeyScheduleEnabled, DefScheduleEnabled)

val KeyScheduleHour = intPreferencesKey("schedule_hour")
const val DefScheduleHour = 2
val ScheduleHour = Pair(KeyScheduleHour, DefScheduleHour)

val KeyScheduleMinute = intPreferencesKey("schedule_minute")
const val DefScheduleMinute = 0
val ScheduleMinute = Pair(KeyScheduleMinute, DefScheduleMinute)

val KeyScheduleLastRunAt = longPreferencesKey("schedule_last_run_at")
const val DefScheduleLastRunAt = 0L
val ScheduleLastRunAt = Pair(KeyScheduleLastRunAt, DefScheduleLastRunAt)
