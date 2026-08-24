package com.xayah.databackup.feature.restore.apps

import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.data.RestoreRepository
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.SortsSequence
import com.xayah.databackup.util.filterRestoreApp
import com.xayah.databackup.util.sortRestoreByA2Z
import com.xayah.databackup.util.sortRestoreBySelectedFirst
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

open class RestoreAppsViewModel(
    private val restoreRepo: RestoreRepository,
) : BaseViewModel() {
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _sortSequence = MutableStateFlow(SortsSequence.ASCENDING)
    val sortSequence: StateFlow<SortsSequence> = _sortSequence.asStateFlow()

    private val _selectedFirst = MutableStateFlow(true)
    val selectedFirst: StateFlow<Boolean> = _selectedFirst.asStateFlow()

    private val _filterUserApps = MutableStateFlow(true)
    val filterUserApps: StateFlow<Boolean> = _filterUserApps.asStateFlow()

    private val _filterSystemApps = MutableStateFlow(true)
    val filterSystemApps: StateFlow<Boolean> = _filterSystemApps.asStateFlow()

    val apps: StateFlow<List<com.xayah.databackup.entity.RestoreApp>> = combine(
        combine(
            restoreRepo.apps,
            _searchText,
            _sortSequence,
        ) { apps, searchText, sortSequence ->
            Triple(apps, searchText, sortSequence)
        },
        combine(
            _selectedFirst,
            _filterUserApps,
            _filterSystemApps,
        ) { selectedFirst, filterUserApps, filterSystemApps ->
            Triple(selectedFirst, filterUserApps, filterSystemApps)
        },
    ) { sortState, filterState ->
        val (apps, searchText, sortSequence) = sortState
        val (selectedFirst, filterUserApps, filterSystemApps) = filterState
        apps
            .filterRestoreApp(filterUserApps, filterSystemApps)
            .filterRestoreApp(searchText)
            .sortRestoreByA2Z(sortSequence)
            .sortRestoreBySelectedFirst(selectedFirst)
    }.stateIn(
        scope = viewModelScope,
        initialValue = emptyList(),
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val allSelected: StateFlow<Int> = apps
        .map { list -> list.count { it.isSelected } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val apkAllSelected: StateFlow<Boolean> = restoreRepo.apps
        .map { apps ->
            val withApk = apps.filter { it.hasApk }
            withApk.isNotEmpty() && withApk.all { it.option.apk }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val dataAllSelected: StateFlow<Boolean> = restoreRepo.apps
        .map { apps ->
            val withData = apps.filter { it.hasInternalData || it.hasExternalData || it.hasAdditionalData }
            withData.isNotEmpty() && withData.all { it.isDataAllSelected }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val intDataAllSelected: StateFlow<Boolean> = restoreRepo.apps
        .map { apps ->
            val withIntData = apps.filter { it.hasInternalData }
            withIntData.isNotEmpty() && withIntData.all { it.option.internalData }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val extDataAllSelected: StateFlow<Boolean> = restoreRepo.apps
        .map { apps ->
            val withExtData = apps.filter { it.hasExternalData }
            withExtData.isNotEmpty() && withExtData.all { it.option.externalData }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val addlDataAllSelected: StateFlow<Boolean> = restoreRepo.apps
        .map { apps ->
            val withAddlData = apps.filter { it.hasAdditionalData }
            withAddlData.isNotEmpty() && withAddlData.all { it.option.additionalData }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun changeSearchText(text: String) {
        _searchText.value = text
    }

    fun changeSequence(sequence: SortsSequence) {
        _sortSequence.value = if (sequence == SortsSequence.ASCENDING) {
            SortsSequence.DESCENDING
        } else {
            SortsSequence.ASCENDING
        }
    }

    fun changeSelectedFirst(selectedFirst: Boolean) {
        _selectedFirst.value = selectedFirst
    }

    fun changeFilterUserApps(enabled: Boolean) {
        if (enabled || _filterSystemApps.value) {
            _filterUserApps.value = enabled
        }
    }

    fun changeFilterSystemApps(enabled: Boolean) {
        if (enabled || _filterUserApps.value) {
            _filterSystemApps.value = enabled
        }
    }

    fun selectAll(dirName: String, toggleableState: ToggleableState) {
        val selected = when (toggleableState) {
            ToggleableState.On -> false
            ToggleableState.Off, ToggleableState.Indeterminate -> true
        }
        withLock(Dispatchers.Default) {
            restoreRepo.selectAll(dirName, selected)
        }
    }

    fun selectApk(dirName: String, selected: Boolean) {
        withLock(Dispatchers.Default) {
            restoreRepo.selectApk(dirName, selected)
        }
    }

    fun selectInternalData(dirName: String, selected: Boolean) {
        withLock(Dispatchers.Default) {
            restoreRepo.selectInternalData(dirName, selected)
        }
    }

    fun selectExternalData(dirName: String, selected: Boolean) {
        withLock(Dispatchers.Default) {
            restoreRepo.selectExternalData(dirName, selected)
        }
    }

    fun selectAdditionalData(dirName: String, selected: Boolean) {
        withLock(Dispatchers.Default) {
            restoreRepo.selectAdditionalData(dirName, selected)
        }
    }

    fun selectAllApk() {
        withLock(Dispatchers.Default) {
            restoreRepo.selectAllApk(apkAllSelected.value.not())
        }
    }

    fun selectAllData() {
        withLock(Dispatchers.Default) {
            restoreRepo.selectAllData(dataAllSelected.value.not())
        }
    }

    fun selectAllIntData() {
        withLock(Dispatchers.Default) {
            restoreRepo.selectAllIntData(intDataAllSelected.value.not())
        }
    }

    fun selectAllExtData() {
        withLock(Dispatchers.Default) {
            restoreRepo.selectAllExtData(extDataAllSelected.value.not())
        }
    }

    fun selectAllAddlData() {
        withLock(Dispatchers.Default) {
            restoreRepo.selectAllAddlData(addlDataAllSelected.value.not())
        }
    }

    fun getSourcePath(): String = restoreRepo.getSourcePath()
}
