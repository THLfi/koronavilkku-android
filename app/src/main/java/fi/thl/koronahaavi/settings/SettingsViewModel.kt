package fi.thl.koronahaavi.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import fi.thl.koronahaavi.device.SystemState
import fi.thl.koronahaavi.device.SystemStateProvider
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val systemStateProvider: SystemStateProvider
) : ViewModel() {

    fun systemState(): LiveData<SystemState?> = systemStateProvider.systemState()
}