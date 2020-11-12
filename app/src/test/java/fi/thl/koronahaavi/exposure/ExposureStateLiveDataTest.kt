package fi.thl.koronahaavi.exposure

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.jraska.livedata.test
import fi.thl.koronahaavi.utils.MainCoroutineScopeRule
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.ZonedDateTime

class ExposureStateLiveDataTest {
    @get:Rule
    val coroutineScope = MainCoroutineScopeRule()
    @get:Rule
    val testRule = InstantTaskExecutorRule()

    private lateinit var hasExposures: MutableLiveData<Boolean>
    private lateinit var lastCheck: MutableLiveData<ZonedDateTime?>
    private lateinit var lockedFlow: MutableStateFlow<Boolean>
    private lateinit var data: ExposureStateLiveData
    private lateinit var enEnabled: MutableLiveData<Boolean?>

    @Before
    fun init() {
        hasExposures = MutableLiveData(false)
        lastCheck = MutableLiveData(ZonedDateTime.now().minusMinutes(1))
        lockedFlow = MutableStateFlow(false)
        enEnabled = MutableLiveData(true)

        data = ExposureStateLiveData(hasExposures, lastCheck, lockedFlow.asLiveData(), enEnabled)
    }

    @Test
    fun pendingWhenOld() {
        lastCheck.value = ZonedDateTime.now().minusDays(2)

        data.test().assertValue { it is ExposureState.Clear.Pending }
    }

    @Test
    fun disabledWhenOldDisabled() {
        lastCheck.value = ZonedDateTime.now().minusDays(2)
        enEnabled.value = false

        data.test().assertValue { it is ExposureState.Clear.Disabled }
    }

    @Test
    fun disabledWhenDisabled() {
        enEnabled.value = false

        data.test().assertValue { it is ExposureState.Clear.Disabled }
    }

    @Test
    fun clearWhenOldLocked() {
        lastCheck.value = ZonedDateTime.now().minusDays(2)
        lockedFlow.value = true

        data.test().assertValue { it is ExposureState.Clear.Updated}
    }

    @Test
    fun clearWhenUpToDate() {
        data.test().assertValue { it is ExposureState.Clear.Updated}
    }

    @Test
    fun clearWhenNoCheck() {
        lastCheck.value = null
        data.test().assertValue { it is ExposureState.Clear.Updated}
    }

    @Test
    fun hasExposures() {
        hasExposures.value = true

        data.test().assertValue { it is ExposureState.HasExposures}
    }

}