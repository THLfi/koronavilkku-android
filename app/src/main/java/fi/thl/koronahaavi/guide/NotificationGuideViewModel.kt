package fi.thl.koronahaavi.guide

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import fi.thl.koronahaavi.R

class NotificationGuideViewModel : ViewModel() {
    private val currentPage = MutableLiveData(0)
    fun currentPage(): LiveData<Int> = currentPage

    private fun currentPageValue(): Int = currentPage.value ?: 0

    fun updatePage(index: Int) {
        if (index != currentPage.value && index in pages.indices) {
            currentPage.postValue(index)
        }
    }

    fun selectNext() = updatePage(currentPageValue() + 1)
    fun selectPrevious() = updatePage(currentPageValue() - 1)

    val showNext: LiveData<Boolean> = currentPage.map { it < pages.size - 1 }
    val showPrevious: LiveData<Boolean> = currentPage.map { it > 0 }

    val pages = listOf(
            PageContent(
                    imageResId = R.drawable.notification_guide_1,
                    bodyTextResId = R.string.notification_guide_text_1,
                    titleTextResId = R.string.notification_guide_title
            ),
            PageContent(
                    imageResId = R.drawable.notification_guide_2,
                    bodyTextResId = R.string.notification_guide_text_2
            ),
            PageContent(
                    imageResId = R.drawable.notification_guide_3,
                    bodyTextResId = R.string.notification_guide_text_3
            ),
            PageContent(
                    imageResId = R.drawable.notification_guide_4,
                    bodyTextResId = R.string.notification_guide_text_4
            ),
            PageContent(
                    imageResId = R.drawable.notification_guide_5,
                    bodyTextResId = R.string.notification_guide_text_5
            )
    )
}

data class PageContent(
        @DrawableRes val imageResId: Int,
        @StringRes val bodyTextResId: Int,
        @StringRes val titleTextResId: Int? = null
)