package com.xayah.databackup.feature.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.PreferenceGroup
import com.xayah.databackup.ui.component.SwitchablePreference
import com.xayah.databackup.util.CleanRestoring
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.RestorePermissions
import com.xayah.databackup.util.RestoreSsaid

@Composable
fun RestoreSettingsScreen(navigator: Navigator) {
    SettingsSubScreen(
        title = stringResource(R.string.restore_settings),
        navigator = navigator,
    ) {
        PreferenceGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            item {
                SwitchablePreference(
                    icon = ImageVector.vectorResource(R.drawable.ic_brush_cleaning),
                    title = stringResource(R.string.clean_restoring),
                    subtitle = stringResource(R.string.clean_restoring_desc),
                    dataStorePair = CleanRestoring,
                )
            }
            item {
                SwitchablePreference(
                    icon = ImageVector.vectorResource(R.drawable.ic_key_round),
                    title = stringResource(R.string.restore_permissions),
                    subtitle = stringResource(R.string.restore_permissions_desc),
                    dataStorePair = RestorePermissions,
                )
            }
            item {
                SwitchablePreference(
                    icon = ImageVector.vectorResource(R.drawable.ic_id_card),
                    title = stringResource(R.string.restore_ssaid),
                    subtitle = stringResource(R.string.restore_ssaid_desc),
                    dataStorePair = RestoreSsaid,
                )
            }
        }
    }
}
