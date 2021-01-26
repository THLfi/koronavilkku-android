package fi.thl.koronahaavi.guide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import fi.thl.koronahaavi.common.getDrawable
import fi.thl.koronahaavi.databinding.FragmentNotificationGuidePageBinding

class NotificationGuidePageFragment(private val pageContent: PageContent) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNotificationGuidePageBinding.inflate(inflater, container, false)

        binding.textNotificationGuidePage.text = getString(pageContent.textResId)
        binding.imageNotificationGuidePage.setImageDrawable(getDrawable(pageContent.imageResId))

        return binding.root
    }

}