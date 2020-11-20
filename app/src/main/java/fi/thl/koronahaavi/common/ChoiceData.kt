package fi.thl.koronahaavi.common

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import fi.thl.koronahaavi.R

/**
 * View model data elements to back a choice fragment
 */
class ChoiceData {
    val selectedControl = MutableLiveData<Int?>()

    val selectedChoice = selectedControl.map {
        when (it) {
            R.id.radio_1 -> Choice.FIRST
            R.id.radio_2 -> Choice.SECOND
            else -> null
        }
    }

    enum class Choice {FIRST, SECOND}
}