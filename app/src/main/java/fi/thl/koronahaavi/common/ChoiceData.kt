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
    val selectedChoice = selectedControl.map { it?.toChoice() }

    // selection boolean value live data
    val selectedPositive = selectedChoice.map { positiveChoice == it }

    // immediate one-time values, not requiring observer
    fun getSelectedChoice(): Choice? = selectedControl.value?.toChoice()
    fun isPositive(): Boolean? = positiveChoice?.let { getSelectedChoice() == it }
    fun setPositive() = positiveChoice?.let { selectedControl.postValue(it.toControl()) }

    enum class Choice { FIRST, SECOND }

    private fun Int.toChoice(): Choice? = when (this) {
        R.id.radio_1 -> Choice.FIRST
        R.id.radio_2 -> Choice.SECOND
        else -> null
    }

    private fun Choice.toControl(): Int = when (this) {
        Choice.FIRST ->R.id.radio_1
        Choice.SECOND ->R.id.radio_2
    }
}