package fi.thl.koronahaavi.common

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.DefaultLifecycleObserver
import timber.log.Timber
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Delegated fragment property that observes fragment view lifecycle, and clears the property
 * value to null when view is destroyed, to avoid memory leaks.
 */
class FragmentViewScopedProperty<T>(val fragment: Fragment) : ReadWriteProperty<Fragment, T> {
    private var _value: T? = null

    init {
        fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {

                // when fragment's view is available, observe the destroy event and clear property
                fragment.viewLifecycleOwnerLiveData.observe(fragment) { viewLifecycleOwner ->
                    viewLifecycleOwner?.lifecycle?.addObserver(object: DefaultLifecycleObserver {
                        override fun onDestroy(owner: LifecycleOwner) {
                            Timber.d("Clearing property $_value")
                            _value = null
                        }
                    })
                }
            }
        })
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        return _value ?: throw IllegalStateException("Cannot access view scoped property after it has been cleared")
    }

    override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T) {
        _value = value
    }
}

/**
 * Extension to make it easier to declare properties in a fragment
 */
fun <T : Any> Fragment.viewScopedProperty(): ReadWriteProperty<Fragment, T> =
    FragmentViewScopedProperty(this)
