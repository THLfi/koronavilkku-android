package fi.thl.koronahaavi.guide

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import fi.thl.koronahaavi.R

class NotificationGuideViewModel : ViewModel() {
    var currentPage = 0

    val pages = listOf(
            PageContent(R.drawable.notification_guide_1, R.string.notification_guide_text_1),
            PageContent(R.drawable.notification_guide_2, R.string.notification_guide_text_2),
            PageContent(R.drawable.notification_guide_3, R.string.notification_guide_text_3),
            PageContent(R.drawable.notification_guide_4, R.string.notification_guide_text_4),
            PageContent(R.drawable.notification_guide_5, R.string.notification_guide_text_5)
    )
}

data class PageContent(
        @DrawableRes val imageResId: Int,
        @StringRes val textResId: Int,
)