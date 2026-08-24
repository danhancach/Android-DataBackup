package com.xayah.databackup

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.xayah.databackup.data.AppRepository
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.data.BackupProcessRepository
import com.xayah.databackup.data.CallLogRepository
import com.xayah.databackup.data.ContactRepository
import com.xayah.databackup.data.FileRepository
import com.xayah.databackup.data.GitHubReleaseRepository
import com.xayah.databackup.data.MessageRepository
import com.xayah.databackup.data.NetworkRepository
import com.xayah.databackup.data.TranslatorRepository
import com.xayah.databackup.data.rustic.RusticAppSourcePlanner
import com.xayah.databackup.data.rustic.RusticBackupCoordinator
import com.xayah.databackup.data.rustic.RusticBackupGateway
import com.xayah.databackup.data.rustic.RusticBackupSelectionProvider
import com.xayah.databackup.data.rustic.RusticBackupSourceCollector
import com.xayah.databackup.data.rustic.RusticStructuredDataSerializer
import com.xayah.databackup.feature.dashboard.DashboardViewModel
import com.xayah.databackup.feature.about.TranslatorsViewModel
import com.xayah.databackup.feature.backup.BackupConfigViewModel
import com.xayah.databackup.feature.backup.BackupLibraryViewModel
import com.xayah.databackup.feature.backup.BackupProcessViewModel
import com.xayah.databackup.feature.backup.BackupSetupViewModel
import com.xayah.databackup.feature.backup.NewBackupViewModel
import com.xayah.databackup.feature.backup.apps.AppsViewModel
import com.xayah.databackup.feature.backup.call_logs.CallLogsViewModel
import com.xayah.databackup.feature.backup.contacts.ContactsViewModel
import com.xayah.databackup.feature.backup.messages.MessagesViewModel
import com.xayah.databackup.feature.backup.networks.NetworksViewModel
import com.xayah.databackup.feature.backup.rustic.RusticBackupProcessViewModel
import com.xayah.databackup.data.RestoreRepository
import com.xayah.databackup.data.RestoreProcessRepository
import com.xayah.databackup.data.rustic.RusticRestoreCoordinator
import com.xayah.databackup.data.rustic.RusticSnapshotRepository
import com.xayah.databackup.feature.restore.RestoreHubViewModel
import com.xayah.databackup.feature.restore.RestoreProcessViewModel
import com.xayah.databackup.feature.restore.RestoreSetupViewModel
import com.xayah.databackup.feature.restore.RestoreSnapshotViewModel
import com.xayah.databackup.feature.restore.apps.RestoreAppsViewModel
import com.xayah.databackup.feature.restore.rustic.RusticRestoreProcessViewModel
import com.xayah.databackup.service.util.RestoreAppsHelper
import com.xayah.databackup.service.util.RestoreCallLogsHelper
import com.xayah.databackup.service.util.RestoreContactsHelper
import com.xayah.databackup.service.util.RestoreMessagesHelper
import com.xayah.databackup.service.util.RestoreNetworksHelper
import com.xayah.databackup.data.BackupScheduleRepository
import com.xayah.databackup.data.migration.MigrationRepository
import com.xayah.databackup.data.ScheduledBackupRunner
import com.xayah.databackup.feature.migration.DataMigrationViewModel
import com.xayah.databackup.feature.schedule.ScheduleViewModel
import com.xayah.databackup.feature.settings.BackupDirectoryViewModel
import com.xayah.databackup.feature.update.UpdatesViewModel
import com.xayah.databackup.service.util.BackupAppsHelper
import com.xayah.databackup.service.util.BackupCallLogsHelper
import com.xayah.databackup.service.util.BackupContactsHelper
import com.xayah.databackup.service.util.BackupMessagesHelper
import com.xayah.databackup.service.util.BackupNetworksHelper
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

class App : Application(), SingletonImageLoader.Factory {
    companion object {
        lateinit var application: Application
    }

    private val appModule = module {
        singleOf(::BackupConfigRepository) bind BackupConfigRepository::class
        singleOf(::AppRepository) bind AppRepository::class
        singleOf(::FileRepository) bind FileRepository::class
        singleOf(::NetworkRepository) bind NetworkRepository::class
        singleOf(::ContactRepository) bind ContactRepository::class
        singleOf(::CallLogRepository) bind CallLogRepository::class
        singleOf(::MessageRepository) bind MessageRepository::class
        singleOf(::BackupProcessRepository) bind BackupProcessRepository::class
        singleOf(::GitHubReleaseRepository) bind GitHubReleaseRepository::class
        singleOf(::TranslatorRepository) bind TranslatorRepository::class
        singleOf(::BackupAppsHelper) bind BackupAppsHelper::class
        singleOf(::BackupNetworksHelper) bind BackupNetworksHelper::class
        singleOf(::BackupContactsHelper) bind BackupContactsHelper::class
        singleOf(::BackupCallLogsHelper) bind BackupCallLogsHelper::class
        singleOf(::BackupMessagesHelper) bind BackupMessagesHelper::class
        singleOf(::RusticAppSourcePlanner)
        singleOf(::RusticStructuredDataSerializer)
        singleOf(::RusticBackupGateway)
        singleOf(::RusticBackupSelectionProvider)
        singleOf(::RusticBackupSourceCollector)
        singleOf(::RusticBackupCoordinator)

        singleOf(::RestoreRepository)
        singleOf(::RestoreProcessRepository)
        singleOf(::RusticRestoreCoordinator)
        singleOf(::RusticSnapshotRepository)
        singleOf(::RestoreAppsHelper)
        singleOf(::RestoreNetworksHelper)
        singleOf(::RestoreContactsHelper)
        singleOf(::RestoreCallLogsHelper)
        singleOf(::RestoreMessagesHelper)

        singleOf(::BackupScheduleRepository)
        singleOf(::ScheduledBackupRunner)
        singleOf(::MigrationRepository)
        viewModelOf(::DataMigrationViewModel)
        viewModelOf(::BackupDirectoryViewModel)
        viewModelOf(::ScheduleViewModel)
        viewModelOf(::DashboardViewModel)
        viewModelOf(::BackupSetupViewModel)
        viewModelOf(::BackupLibraryViewModel)
        viewModelOf(::NewBackupViewModel)
        viewModelOf(::BackupProcessViewModel)
        viewModelOf(::RusticBackupProcessViewModel)
        viewModelOf(::RestoreHubViewModel)
        viewModelOf(::RestoreSetupViewModel)
        viewModelOf(::RestoreSnapshotViewModel)
        viewModelOf(::RestoreProcessViewModel)
        viewModelOf(::RusticRestoreProcessViewModel)
        viewModelOf(::RestoreAppsViewModel)
        viewModelOf(::BackupConfigViewModel)
        viewModelOf(::AppsViewModel)
        viewModelOf(::NetworksViewModel)
        viewModelOf(::ContactsViewModel)
        viewModelOf(::CallLogsViewModel)
        viewModelOf(::MessagesViewModel)
        viewModelOf(::UpdatesViewModel)
        viewModelOf(::TranslatorsViewModel)
    }

    override fun onCreate() {
        super.onCreate()
        application = this

        startKoin {
            androidLogger()
            androidContext(application)
            modules(appModule)
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(HttpClient(CIO)))
            }
            .build()
    }
}
