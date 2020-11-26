package fi.thl.koronahaavi.common

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import fi.thl.koronahaavi.R

/**
 * View model data elements to back a choice fragment
 */
class ChoiceData(private val positiveChoice: Choice?) {

    // radio button element id for direct data binding
    val selectedControl = MutableLiveData<Int?>()

    // selection enum value live data
    val selectedChoice = selectedControl.map(Choice.Companion::fromControl)

    // selection boolean value live data
    val selectedPositive = selectedChoice.map { positiveChoice == it }

    // immediate one-time values, not requiring observer
    fun getSelectedChoice(): Choice? = Choice.fromControl(selectedControl.value)
    fun isPositive(): Boolean? = positiveChoice?.let { getSelectedChoice() == it }

    enum class Choice {
        FIRST, SECOND;

        companion object {
            fun fromControl(controlId: Int?) =
                when (controlId) {
                    R.id.radio_1 -> FIRST
                    R.id.radio_2 -> SECOND
                    else -> null
                }
            }
    }
}