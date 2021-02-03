package fi.thl.koronahaavi.guide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import fi.thl.koronahaavi.common.getDrawable
import fi.thl.koronahaavi.databinding.FragmentNotificationGuidePageBinding

class NotificationGuidePageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNotificationGuidePageBinding.inflate(inflater, container, false)

        arguments?.let { args ->
            binding.textNotificationGuidePageNumber.text = args.getInt(ARG_PAGE_ID).toString()
            binding.textNotificationGuidePageBody.text = getString(args.getInt(ARG_TEXT_ID))
            binding.imageNotificationGuidePage.setImageDrawable(getDrawable(args.getInt(ARG_IMAGE_ID)))
        }

        return binding.root
    }

    companion object {
        fun create(pageNum: Int, @StringRes textResId: Int, @DrawableRes imageResId: Int) =
            NotificationGuidePageFragment().apply {
                arguments = bundleOf(
                        ARG_PAGE_ID to pageNum,
                        ARG_TEXT_ID to textResId,
                        ARG_IMAGE_ID to imageResId
                )
            }

        private const val ARG_TEXT_ID = "text_id"
        private const val ARG_IMAGE_ID = "image_id"
        private const val ARG_PAGE_ID = "page_id"
    }
}