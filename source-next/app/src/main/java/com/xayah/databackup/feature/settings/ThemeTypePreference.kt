package com.xayah.databackup.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.DataBackupDialog
import com.xayah.databackup.ui.component.DialogActionButton
import com.xayah.databackup.ui.component.DialogDismissButton
import com.xayah.databackup.ui.component.DialogIcon
import com.xayah.databackup.ui.component.Preference
import com.xayah.databackup.util.ThemeType
import com.xayah.databackup.util.readThemeType
import com.xayah.databackup.util.saveThemeType
import kotlinx.coroutines.launch

@Composable
fun ThemeTypePreference(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeType by context.readThemeType().collectAsStateWithLifecycle(initialValue = ThemeType.AUTO)
    var openDialog by remember { mutableStateOf(false) }

    val subtitle = when (themeType) {
        ThemeType.AUTO -> stringResource(R.string.theme_auto_desc)
        ThemeType.LIGHT -> stringResource(R.string.theme_light_desc)
        ThemeType.DARK -> stringResource(R.string.theme_dark_desc)
    }
    val currentLabel = when (themeType) {
        ThemeType.AUTO -> stringResource(R.string.theme_auto)
        ThemeType.LIGHT -> stringResource(R.string.theme_light)
        ThemeType.DARK -> stringResource(R.string.theme_dark)
    }

    if (openDialog) {
        var draft by remember(themeType) { mutableStateOf(themeType) }
        DataBackupDialog(
            title = stringResource(R.string.dark_theme),
            onDismissRequest = { openDialog = false },
            icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_palette)) },
            content = {
                Column {
                    ThemeType.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { draft = option }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = draft == option,
                                onClick = { draft = option },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = themeTypeTitle(option))
                                Text(text = themeTypeDescription(option))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                DialogActionButton(
                    text = stringResource(R.string.save),
                    icon = ImageVector.vectorResource(R.drawable.ic_check),
                    onClick = {
                        scope.launch {
                            context.saveThemeType(draft)
                            openDialog = false
                        }
                    },
                )
            },
            dismissButton = {
                DialogDismissButton(
                    text = stringResource(R.string.cancel),
                    onClick = { openDialog = false },
                )
            },
        )
    }

    Preference(
        modifier = modifier,
        icon = ImageVector.vectorResource(R.drawable.ic_palette),
        title = stringResource(R.string.dark_theme),
        subtitle = "$currentLabel · $subtitle",
        onClick = { openDialog = true },
    )
}

@Composable
private fun themeTypeTitle(type: ThemeType): String = when (type) {
    ThemeType.AUTO -> stringResource(R.string.theme_auto)
    ThemeType.LIGHT -> stringResource(R.string.theme_light)
    ThemeType.DARK -> stringResource(R.string.theme_dark)
}

@Composable
private fun themeTypeDescription(type: ThemeType): String = when (type) {
    ThemeType.AUTO -> stringResource(R.string.theme_auto_desc)
    ThemeType.LIGHT -> stringResource(R.string.theme_light_desc)
    ThemeType.DARK -> stringResource(R.string.theme_dark_desc)
}
