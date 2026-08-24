package com.xayah.databackup.service.util

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object IntegrityConfirmation {
    private val _request = MutableStateFlow<IntegrityReport?>(null)
    val request: StateFlow<IntegrityReport?> = _request

    @Volatile
    private var deferred: CompletableDeferred<Boolean>? = null

    suspend fun awaitDecision(report: IntegrityReport): Boolean {
        val pending = CompletableDeferred<Boolean>()
        deferred = pending
        _request.value = report
        return pending.await()
    }

    fun decide(continueRestore: Boolean) {
        _request.value = null
        deferred?.complete(continueRestore)
        deferred = null
    }
}
