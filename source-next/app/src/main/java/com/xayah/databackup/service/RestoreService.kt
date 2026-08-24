package com.xayah.databackup.service

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.os.RemoteException
import com.xayah.databackup.App
import com.xayah.databackup.data.RestoreProcessRepository
import com.xayah.databackup.service.util.RestoreAppsHelper
import com.xayah.databackup.service.util.RestoreCallLogsHelper
import com.xayah.databackup.service.util.RestoreContactsHelper
import com.xayah.databackup.service.util.RestoreMessagesHelper
import com.xayah.databackup.service.util.RestoreNetworksHelper
import com.xayah.databackup.util.LastRestoreTime
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.saveLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.koin.android.ext.android.inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object RestoreService {
    private const val TAG = "RestoreService"
    private const val TIMEOUT = 10000L

    private var mMutex = Mutex()
    private var mBinder: IBinder? = null
    private var mService: RestoreServiceImpl? = null
    private var mConnection: ServiceConnection? = null

    class RestoreServiceImpl : Service() {
        private val mRestoreProcessRepo: RestoreProcessRepository by inject()
        private val mRestoreAppsHelper: RestoreAppsHelper by inject()
        private val mRestoreNetworksHelper: RestoreNetworksHelper by inject()
        private val mRestoreContactsHelper: RestoreContactsHelper by inject()
        private val mRestoreCallLogsHelper: RestoreCallLogsHelper by inject()
        private val mRestoreMessagesHelper: RestoreMessagesHelper by inject()
        private val mBinder: Binder = Service()

        inner class Service : Binder() {
            fun getService(): RestoreServiceImpl = this@RestoreServiceImpl
        }

        override fun onBind(intent: Intent): IBinder = mBinder

        suspend fun restoreApps() {
            mMutex.withLock {
                mRestoreAppsHelper.start()
            }
        }

        suspend fun restoreNetworks() {
            mMutex.withLock {
                mRestoreNetworksHelper.start()
            }
        }

        suspend fun restoreContacts() {
            mMutex.withLock {
                mRestoreContactsHelper.start()
            }
        }

        suspend fun restoreCallLogs() {
            mMutex.withLock {
                mRestoreCallLogsHelper.start()
            }
        }

        suspend fun restoreMessages() {
            mMutex.withLock {
                mRestoreMessagesHelper.start()
            }
        }

        private fun ensureNotCanceled(stage: String) {
            if (mRestoreProcessRepo.mIsCanceled) {
                throw CancellationException("Restore canceled before $stage.")
            }
        }

        suspend fun start() {
            try {
                ensureNotCanceled("apps restore")
                val appsItem = mRestoreProcessRepo.getAppsItem().value
                if (appsItem.isSelected && appsItem.totalCount > 0) {
                    restoreApps()
                }

                ensureNotCanceled("networks restore")
                val networksItem = mRestoreProcessRepo.getNetworksItem().value
                if (networksItem.isSelected && networksItem.totalCount > 0) {
                    restoreNetworks()
                }

                ensureNotCanceled("contacts restore")
                val contactsItem = mRestoreProcessRepo.getContactsItem().value
                if (contactsItem.isSelected && contactsItem.totalCount > 0) {
                    restoreContacts()
                }

                ensureNotCanceled("call logs restore")
                val callLogsItem = mRestoreProcessRepo.getCallLogsItem().value
                if (callLogsItem.isSelected && callLogsItem.totalCount > 0) {
                    restoreCallLogs()
                }

                ensureNotCanceled("messages restore")
                val messagesItem = mRestoreProcessRepo.getMessagesItem().value
                if (messagesItem.isSelected && messagesItem.totalCount > 0) {
                    restoreMessages()
                }

                App.application.saveLong(LastRestoreTime.first, System.currentTimeMillis())
            } catch (e: CancellationException) {
                LogHelper.i(TAG, "start", "Restore pipeline canceled: ${e.message}")
            }
        }
    }

    private suspend fun bindService(context: Context): RestoreServiceImpl {
        return withTimeout(TIMEOUT) {
            suspendCancellableCoroutine { continuation ->
                if (mService == null) {
                    val connection = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName, service: IBinder) {
                            mBinder = service
                            (service as? RestoreServiceImpl.Service)?.getService()?.also {
                                LogHelper.i(TAG, "bindService", "Service connected.")
                                mService = it
                                if (continuation.context.isActive) continuation.resume(it)
                                return
                            }
                            val msg = "Service connected, but failed to get service instance."
                            if (continuation.context.isActive) continuation.resumeWithException(RemoteException(msg))
                        }

                        override fun onServiceDisconnected(name: ComponentName) {
                            val msg = "Service disconnected."
                            LogHelper.w(TAG, "bindService", msg)
                            if (continuation.context.isActive) continuation.resumeWithException(RemoteException(msg))
                        }

                        override fun onBindingDied(name: ComponentName) {
                            val msg = "Binding died."
                            LogHelper.e(TAG, "bindService", msg)
                            if (continuation.context.isActive) continuation.resumeWithException(RemoteException(msg))
                        }

                        override fun onNullBinding(name: ComponentName) {
                            val msg = "Null binding."
                            LogHelper.e(TAG, "bindService", msg)
                            if (continuation.context.isActive) continuation.resumeWithException(RemoteException(msg))
                        }
                    }
                    context.bindService(Intent(context, RestoreServiceImpl::class.java), connection, Context.BIND_AUTO_CREATE)
                    mConnection = connection
                } else {
                    mService
                }
            }
        }
    }

    fun destroyService(context: Context) {
        mConnection?.also { context.unbindService(it) }
        mService?.stopSelf()
        mBinder = null
        mService = null
        mConnection = null
    }

    private suspend fun getService(): RestoreServiceImpl? {
        return if (mService == null) {
            runCatching { bindService(App.application) }.getOrNull()
        } else if (mBinder?.isBinderAlive == false) {
            destroyService(App.application)
            runCatching { bindService(App.application) }.getOrNull()
        } else {
            mService
        }
    }

    suspend fun start() {
        getService()?.start()
    }
}
