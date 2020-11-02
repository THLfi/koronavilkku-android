package fi.thl.koronahaavi.exposure

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.*
import android.view.WindowManager.LayoutParams
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.*
import fi.thl.koronahaavi.common.FormatExtensions.formatLastCheckTime
import fi.thl.koronahaavi.databinding.FragmentExposureDetailBinding
import java.time.ZonedDateTime

@AndroidEntryPoint
class ExposureDetailFragment : Fragment() {
    private lateinit var binding: FragmentExposureDetailBinding

    private val viewModel: ExposureDetailViewModel by navGraphViewModels(R.id.exposure_navigation) {
        defaultViewModelProviderFactory
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_exposure_detail, container, false)
        binding = FragmentExposureDetailBinding.bind(root).apply {
            this.model = viewModel
        }

        binding.lifecycleOwner = this.viewLifecycleOwner
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        requireActivity().window.apply {
            clearFlags(LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with (binding) {
            toolbar.setupWithNavController(findNavController())

            buttonExposureDetailContactStart.setOnClickListener {
                findNavController().navigateSafe(ExposureDetailFragmentDirections.toSelectMunicipality())
            }

            layoutButtonExposureDetailCheck.button.setOnClickListener {
                startManualExposureCheck()
            }

            // icon color in included layout is not parameterized, so set it directly
            layoutExposureDetailNotifications.linkItemCard.findViewById<ImageView>(R.id.link_item_indicator)?.imageTintList =
                ColorStateList.valueOf(resources.getColor(R.color.mainBlue, null))

            layoutExposureDetailNotifications.linkItemCard.setOnClickListener {
                findNavController().navigateSafe(ExposureDetailFragmentDirections.toNotificationList())
            }
        }

        viewModel.hasExposures.observe(viewLifecycleOwner, Observer { exposed ->
            updateToolbar(exposed)
        })

        viewModel.lastCheckTime.observe(viewLifecycleOwner, Observer {
            updateLastCheckTimeLabel(it)
        })

        viewModel.exposureCheckState.observe(viewLifecycleOwner,
            ExposureCheckObserver(requireContext(), viewModel.checkInProgress)
        )
    }

    private fun updateLastCheckTimeLabel(dateTime: ZonedDateTime?) {
        binding.textExposureDetailLastCheck.text = requireContext().formatLastCheckTime(dateTime)
    }

    private fun updateToolbar(exposed: Boolean) {
        if (exposed) {
            val errorColor = requireContext().themeColor(R.attr.colorError)
            binding.toolbar.apply {
                title = getString(R.string.exposure_detail_title)
                setBackgroundColor(errorColor)
                navigationIcon?.colorFilter = null
            }
            requireActivity().window.statusBarColor = errorColor
        }
        else {
            val onSurfaceColor = resources.getColor(R.color.textDarkGrey, null)

            binding.toolbar.apply {
                title = null
                setBackgroundColor(resources.getColor(android.R.color.transparent, null))
                navigationIcon?.colorFilter = PorterDuffColorFilter(onSurfaceColor, PorterDuff.Mode.SRC_IN)
            }
            requireActivity().window.clearFlags(LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }
    }

    override fun onStop() {
        super.onStop()

        requireActivity().window.clearFlags(LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    }

    private fun startManualExposureCheck() = viewModel.startExposureCheck()
}