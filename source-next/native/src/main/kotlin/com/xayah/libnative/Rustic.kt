package com.xayah.libnative

object Rustic {
    fun initLogger() = nativeInitLogger()

    fun initRepository(
        repositoryPath: String,
        password: String,
        optionKeys: List<String> = emptyList(),
        optionValues: List<String> = emptyList(),
    ) {
        nativeInitRepository(
            repositoryPath,
            password,
            optionKeys.toTypedArray(),
            optionValues.toTypedArray(),
        )
    }

    fun repositoryExists(repositoryPath: String): Boolean {
        return nativeRepositoryExists(repositoryPath)
    }

    fun validateRepository(repositoryPath: String, password: String) {
        nativeValidateRepository(repositoryPath, password)
    }

    fun createSnapshot(
        repositoryPath: String,
        password: String,
        sourcePaths: List<String>,
        tags: List<String> = emptyList(),
        callback: Any? = null,
        cancelId: Long = 0L,
        optionKeys: List<String> = emptyList(),
        optionValues: List<String> = emptyList(),
    ): String {
        return nativeCreateSnapshot(
            repositoryPath,
            password,
            sourcePaths.toTypedArray(),
            tags.toTypedArray(),
            callback,
            cancelId,
            optionKeys.toTypedArray(),
            optionValues.toTypedArray(),
        )
    }

    fun cancelBackup(cancelId: Long) {
        nativeCancelBackup(cancelId)
    }

    fun restoreSnapshot(repositoryPath: String, password: String, snapshotId: String, destinationPath: String) {
        nativeRestoreSnapshot(repositoryPath, password, snapshotId, destinationPath)
    }

    fun listSnapshots(repositoryPath: String, password: String, tagFilter: String? = null): String {
        return nativeListSnapshots(repositoryPath, password, tagFilter.orEmpty())
    }

    fun checkRepository(repositoryPath: String, password: String) {
        nativeCheckRepository(repositoryPath, password)
    }

    private external fun nativeInitLogger()
    private external fun nativeInitRepository(
        repositoryPath: String,
        password: String,
        optionKeys: Array<String>,
        optionValues: Array<String>,
    )

    private external fun nativeRepositoryExists(repositoryPath: String): Boolean
    private external fun nativeValidateRepository(repositoryPath: String, password: String)
    private external fun nativeCreateSnapshot(
        repositoryPath: String,
        password: String,
        sourcePaths: Array<String>,
        tags: Array<String>,
        callback: Any?,
        cancelId: Long,
        optionKeys: Array<String>,
        optionValues: Array<String>,
    ): String

    private external fun nativeRestoreSnapshot(
        repositoryPath: String,
        password: String,
        snapshotId: String,
        destinationPath: String,
    )

    private external fun nativeListSnapshots(
        repositoryPath: String,
        password: String,
        tagFilter: String,
    ): String

    private external fun nativeCheckRepository(repositoryPath: String, password: String)
    private external fun nativeCancelBackup(cancelId: Long)
}
