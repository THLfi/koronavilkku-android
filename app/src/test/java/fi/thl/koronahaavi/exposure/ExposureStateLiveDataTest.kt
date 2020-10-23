package fi.thl.koronahaavi.exposure

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jraska.livedata.test
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.ZonedDateTime

class ExposureStateLiveDataTest {
    @get:Rule
    val testRule = InstantTaskExecutorRule()

    private lateinit var hasExposures: MutableLiveData<Boolean>
    private lateinit var lastCheck: MutableLiveData<ZonedDateTime?>

    @Before
    fun init() {
        hasExposures = MutableLiveData(false)
        lastCheck = MutableLiveData()
    }

    @Test
    fun pendingWhenOld() {
        val data = ExposureStateLiveData(hasExposures, lastCheck)
        lastCheck.value = ZonedDateTime.now().minusDays(2)

        data.test().assertValue { it is ExposureState.Pending}
    }

    @Test
    fun clearWhenUpToDate() {
        val data = ExposureStateLiveData(hasExposures, lastCheck)
        lastCheck.value = ZonedDateTime.now().minusHours(4)

        data.test().assertValue { it is ExposureState.Clear}
    }

    @Test
    fun hasExposures() {
        val data = ExposureStateLiveData(hasExposures, lastCheck)
        lastCheck.value = ZonedDateTime.now().minusHours(4)
        hasExposures.value = true

        data.test().assertValue { it is ExposureState.HasExposures}
    }

}