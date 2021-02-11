package fi.thl.koronahaavi.guide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.safeGetDrawable
import fi.thl.koronahaavi.common.safeGetString
import fi.thl.koronahaavi.databinding.FragmentNotificationGuidePageBinding

class NotificationGuidePageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNotificationGuidePageBinding.inflate(inflater, container, false)

        arguments?.let { args ->
            val pageNum = args.getInt(ARG_CURRENT_PAGE_ID)
            val pageCount = args.getInt(ARG_PAGE_COUNT_ID)
            binding.textNotificationGuidePageNumber.text = pageNum.toString()

            // set page number to content description to help with accessibility
            binding.textNotificationGuidePageNumber.contentDescription =
                getString(R.string.notification_guide_page_content_description, pageNum, pageCount)

            binding.textNotificationGuidePageBody.text = context?.safeGetString(args.getInt(ARG_TEXT_ID))

            binding.imageNotificationGuidePage.setImageDrawable(
                    context?.safeGetDrawable(args.getInt(ARG_IMAGE_ID))
            )
        }

        return binding.root
    }

    companion object {
        fun create(pageNum: Int, pageCount: Int, @StringRes textResId: Int, @DrawableRes imageResId: Int) =
            NotificationGuidePageFragment().apply {
                arguments = bundleOf(
                        ARG_CURRENT_PAGE_ID to pageNum,
                        ARG_PAGE_COUNT_ID to pageCount,
                        ARG_TEXT_ID to textResId,
                        ARG_IMAGE_ID to imageResId
                )
            }

        const val ARG_TEXT_ID = "text_id"
        const val ARG_IMAGE_ID = "image_id"
        const val ARG_CURRENT_PAGE_ID = "current_page_id"
        const val ARG_PAGE_COUNT_ID = "page_count_id"
    }
}