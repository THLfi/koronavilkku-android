package fi.thl.koronahaavi.guide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import fi.thl.koronahaavi.databinding.FragmentNotificationGuideBinding

class NotificationGuideFragment : Fragment() {

    private var _binding: FragmentNotificationGuideBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<NotificationGuideViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.onBackPressedDispatcher?.addCallback(owner = this) {
            if (viewModel.currentPage().value != 0) {
                // override back press to keep fragment and select previous page
                viewModel.selectPrevious()
            }
            else {
                // on first page, let activity close the fragment
                isEnabled = false
                activity?.onBackPressed()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationGuideBinding.inflate(inflater, container, false).apply {
            model = viewModel
        }

        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with (binding) {
            layoutToolbar.toolbar.setupWithNavController(findNavController())
            pageIndicatorNotificationGuide.setNumSteps(viewModel.pages.size)
            viewpagerNotificationGuide.adapter = GuidePagerAdapter(this@NotificationGuideFragment, viewModel)

            viewpagerNotificationGuide.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    // This is required to update view model after user swipes to a new page
                    // It's also triggered when calling setCurrentItem, but updatePage will ignore if already on correct page
                    viewModel.updatePage(position)
                }
            } )

            buttonNotificationGuideNext.setOnClickListener { viewModel.selectNext() }
            buttonNotificationGuidePrevious.setOnClickListener { viewModel.selectPrevious() }

            viewModel.currentPage().observe(viewLifecycleOwner, { page ->
                // setCurrentItem is ignored if animation already started to target page
                viewpagerNotificationGuide.setCurrentItem(page, true)
                pageIndicatorNotificationGuide.setStep(page)
            })
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
        viewModel.pages.getOrNull(position)?.let {
            NotificationGuidePageFragment.create(
                    pageNum = position + 1,
                    pageCount = itemCount,
                    textResId = it.textResId,
                    imageResId = it.imageResId
            )
        } ?: throw Exception("Invalid fragment position $position")
}

