package fi.thl.koronahaavi.settings

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import fi.thl.koronahaavi.device.SystemState
import fi.thl.koronahaavi.device.SystemStateProvider

class SettingsViewModel @ViewModelInject constructor(
    private val systemStateProvider: SystemStateProvider
) : ViewModel() {

    fun systemState(): LiveData<SystemState?> = systemStateProvider.systemState()
}