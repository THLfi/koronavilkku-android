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

    @Before
    fun init() {
        hasExposures = MutableLiveData(false)
        lastCheck = MutableLiveData()
        lockedFlow = MutableStateFlow(false)

        data = ExposureStateLiveData(hasExposures, lastCheck, lockedFlow.asLiveData())
    }

    @Test
    fun pendingWhenOld() {
        lastCheck.value = ZonedDateTime.now().minusDays(2)

        data.test().assertValue { it is ExposureState.Pending}
    }

    @Test
    fun clearWhenLocked() {
        lastCheck.value = ZonedDateTime.now().minusDays(2)
        lockedFlow.value = true

        data.test().assertValue { it is ExposureState.Clear}
    }

    @Test
    fun clearWhenUpToDate() {
        lastCheck.value = ZonedDateTime.now().minusHours(4)

        data.test().assertValue { it is ExposureState.Clear}
    }

    @Test
    fun hasExposures() {
        lastCheck.value = ZonedDateTime.now().minusHours(4)
        hasExposures.value = true

        data.test().assertValue { it is ExposureState.HasExposures}
    }

}