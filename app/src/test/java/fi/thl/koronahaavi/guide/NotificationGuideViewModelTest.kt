package fi.thl.koronahaavi.guide

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.jraska.livedata.test
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NotificationGuideViewModelTest {
    @get:Rule
    val testRule = InstantTaskExecutorRule()

    private lateinit var viewModel: NotificationGuideViewModel

    @Before
    fun init() {
        viewModel = NotificationGuideViewModel()
    }

    @Test
    fun updatePage() {
        viewModel.updatePage(2)
        viewModel.currentPage().test().assertValue(2)
    }

    @Test
    fun updatePageOutOfBoundsIgnored() {
        viewModel.updatePage(-1)
        viewModel.currentPage().test().assertValue(0)

        viewModel.updatePage(10)
        viewModel.currentPage().test().assertValue(0)
    }

    @Test
    fun selectNext() {
        viewModel.selectNext()
        viewModel.currentPage().test().assertValue(1)
    }

    @Test
    fun selectNextAtEnd() {
        viewModel.updatePage(viewModel.pages.lastIndex)
        viewModel.selectNext()
        viewModel.currentPage().test().assertValue(viewModel.pages.lastIndex)
    }

    @Test
    fun selectPrevious() {
        viewModel.updatePage(2)
        viewModel.selectPrevious()
        viewModel.currentPage().test().assertValue(1)
    }

    @Test
    fun selectPreviousAtStart() {
        viewModel.selectPrevious()
        viewModel.currentPage().test().assertValue(0)
    }

    @Test
    fun showNext() {
        viewModel.showNext.test().assertValue(true)
    }

    @Test
    fun hideNext() {
        viewModel.updatePage(viewModel.pages.lastIndex)
        viewModel.showNext.test().assertValue(false)
    }

    @Test
    fun showPrevious() {
        viewModel.updatePage(2)
        viewModel.showPrevious.test().assertValue(true)
    }

    @Test
    fun hidePrevious() {
        viewModel.showPrevious.test().assertValue(false)
    }
}