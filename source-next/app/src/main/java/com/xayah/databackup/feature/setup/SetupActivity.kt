package com.xayah.databackup.feature.setup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.xayah.databackup.ui.navigation.navForwardTransitionSpec
import com.xayah.databackup.ui.navigation.navPopTransitionSpec
import com.xayah.databackup.ui.navigation.navPredictivePopTransitionSpec
import com.xayah.databackup.ui.theme.DataBackupTheme
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.NotificationHelper
import kotlinx.serialization.Serializable

const val NoPermKey = "NoPerm"

@Serializable
data object Welcome : NavKey

@Serializable
data class Permissions(
    val enableBackBtn: Boolean
) : NavKey

class SetupActivity : ComponentActivity() {
    private lateinit var mPermissionsViewModel: PermissionsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            DataBackupTheme {
                mPermissionsViewModel = viewModel()
                val noPerm = intent.getBooleanExtra(NoPermKey, false)
                val startRoute = if (noPerm) Permissions(false) else Welcome
                val backStack = rememberNavBackStack(startRoute)
                val navigator = remember(backStack) { Navigator(backStack) }

                Surface {
                    NavDisplay(
                        backStack = backStack,
                        onBack = navigator::goBack,
                        transitionSpec = navForwardTransitionSpec(),
                        popTransitionSpec = navPopTransitionSpec(),
                        predictivePopTransitionSpec = navPredictivePopTransitionSpec(),
                        entryProvider = entryProvider {
                            entry<Welcome> { WelcomeScreen(navigator) }
                            entry<Permissions> { permissions ->
                                PermissionsScreen(navigator, mPermissionsViewModel, permissions)
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray, deviceId: Int) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        when (requestCode) {
            NotificationHelper.REQUEST_CODE -> {
                mPermissionsViewModel.withLock {
                    mPermissionsViewModel.checkNotification(this)
                }
            }
        }
    }
}
