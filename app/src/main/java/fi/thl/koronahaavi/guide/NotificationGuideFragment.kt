package fi.thl.koronahaavi.guide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.databinding.FragmentNotificationGuideBinding
import timber.log.Timber

class NotificationGuideFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentNotificationGuideBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<NotificationGuideViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationGuideBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with (binding) {
            layoutToolbar.toolbar.setupWithNavController(findNavController())

            viewpagerNotificationGuide.adapter = GuidePagerAdapter(this@NotificationGuideFragment, viewModel)
            viewpagerNotificationGuide.setCurrentItem(viewModel.currentPage, false)

            viewpagerNotificationGuide.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    Timber.d("page selected $position")
                    viewModel.currentPage = position
                }
            } )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class GuidePagerAdapter(
        fragment: Fragment,
        private val viewModel: NotificationGuideViewModel
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = viewModel.pages.size

    override fun createFragment(position: Int): Fragment =
            NotificationGuidePageFragment(viewModel.pages.getOrNull(position)
                ?: throw Exception("Invalid fragment position $position"))
}

