package com.xayah.databackup.feature.settings

import android.os.Build
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
import com.xayah.databackup.util.DynamicColor
import com.xayah.databackup.util.Navigator

@Composable
fun AppearanceSettingsScreen(navigator: Navigator) {
    SettingsSubScreen(
        title = stringResource(R.string.appearance),
        navigator = navigator,
    ) {
        PreferenceGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            ThemeTypePreference()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchablePreference(
                    icon = ImageVector.vectorResource(R.drawable.ic_palette),
                    title = stringResource(R.string.monet),
                    subtitle = stringResource(R.string.monet_desc),
                    dataStorePair = DynamicColor,
                )
            }
        }
    }
}
