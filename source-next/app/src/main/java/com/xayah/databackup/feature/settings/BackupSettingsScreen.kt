package com.xayah.databackup.feature.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.AutoScreenOffSwitch
import com.xayah.databackup.ui.component.Preference
import com.xayah.databackup.ui.component.PreferenceGroup
import com.xayah.databackup.ui.component.ResetBackupListSwitch
import com.xayah.databackup.ui.component.SectionHeader
import com.xayah.databackup.ui.component.SwitchablePreference
import com.xayah.databackup.feature.BackupDirectoryRoute
import com.xayah.databackup.util.AppsOptionSelectedBackup
import com.xayah.databackup.util.CallLogsOptionSelectedBackup
import com.xayah.databackup.util.ContactsOptionSelectedBackup
import com.xayah.databackup.util.MessagesOptionSelectedBackup
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.navigateSafely
import com.xayah.databackup.util.NetworksOptionSelectedBackup
import com.xayah.databackup.util.PathHelper

@Composable
fun BackupSettingsScreen(navigator: Navigator) {
    val backupPath by PathHelper.getBackupPath().collectAsStateWithLifecycle(
        initialValue = PathHelper.DEFAULT_BACKUP_PATH,
    )

    SettingsSubScreen(
        title = stringResource(R.string.backup_settings),
        navigator = navigator,
    ) {
        SectionHeader(
            modifier = Modifier.padding(16.dp),
            title = stringResource(R.string.backup_dir),
        )
        PreferenceGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            Preference(
                icon = ImageVector.vectorResource(R.drawable.ic_folder),
                title = stringResource(R.string.backup_dir),
                subtitle = backupPath,
                subtitleMaxLines = Int.MAX_VALUE,
                subtitleOverflow = TextOverflow.Visible,
                subtitleFontFamily = FontFamily.Monospace,
                onClick = { navigator.navigateSafely(BackupDirectoryRoute) },
            )
        }

        SectionHeader(
            modifier = Modifier.padding(16.dp),
            title = stringResource(R.string.backup),
        )
        PreferenceGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            AutoScreenOffSwitch()
            ResetBackupListSwitch()
        }

        SectionHeader(
            modifier = Modifier.padding(16.dp),
            title = stringResource(R.string._default),
        )
        PreferenceGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            SwitchablePreference(
                icon = ImageVector.vectorResource(R.drawable.ic_resource_package),
                title = stringResource(R.string.apps),
                subtitle = stringResource(R.string.apps),
                dataStorePair = AppsOptionSelectedBackup,
            )
            SwitchablePreference(
                icon = ImageVector.vectorResource(R.drawable.ic_wifi),
                title = stringResource(R.string.networks),
                subtitle = stringResource(R.string.networks),
                dataStorePair = NetworksOptionSelectedBackup,
            )
            SwitchablePreference(
                icon = ImageVector.vectorResource(R.drawable.ic_user),
                title = stringResource(R.string.contacts),
                subtitle = stringResource(R.string.contacts),
                dataStorePair = ContactsOptionSelectedBackup,
            )
            SwitchablePreference(
                icon = ImageVector.vectorResource(R.drawable.ic_phone),
                title = stringResource(R.string.call_logs),
                subtitle = stringResource(R.string.call_logs),
                dataStorePair = CallLogsOptionSelectedBackup,
            )
            SwitchablePreference(
                icon = ImageVector.vectorResource(R.drawable.ic_message_circle),
                title = stringResource(R.string.messages),
                subtitle = stringResource(R.string.messages),
                dataStorePair = MessagesOptionSelectedBackup,
            )
        }
    }
}
