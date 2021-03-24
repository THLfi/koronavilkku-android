package fi.thl.koronahaavi.exposure

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.FormatExtensions
import fi.thl.koronahaavi.common.FormatExtensions.formatDateRange
import fi.thl.koronahaavi.common.FormatExtensions.formatLastCheckTime
import fi.thl.koronahaavi.common.viewScopedProperty
import fi.thl.koronahaavi.databinding.FragmentExposureNotificationListBinding
import fi.thl.koronahaavi.databinding.ItemNotificationInfoBinding

@AndroidEntryPoint
class ExposureNotificationListFragment : BottomSheetDialogFragment() {
    private var binding by viewScopedProperty<FragmentExposureNotificationListBinding>()

    private val viewModel: ExposureDetailViewModel by hiltNavGraphViewModels(R.id.exposure_navigation)
    private val listAdapter by lazy { NotificationAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // custom theme to set background color
        setStyle(DialogFragment.STYLE_NORMAL, R.style.ThemeOverlay_Vilkku_BottomSheetDialog_WithBackground)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExposureNotificationListBinding.inflate(inflater, container, false).apply {
            this.model = viewModel
        }

        binding.lifecycleOwner = this.viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with (binding) {
            buttonNotificationListClose.setOnClickListener { dismiss() }

            recyclerviewNotificationList.apply {
                adapter = listAdapter
                layoutManager = LinearLayoutManager(context)
            }
        }

        viewModel.lastCheckTime.observe(viewLifecycleOwner, {
            binding.textNotificationListLastCheck.text = requireContext().formatLastCheckTime(it)
        })

        viewModel.notifications.observe(viewLifecycleOwner, {
            listAdapter.submitList(it)
        })
    }
}

class NotificationAdapter : ListAdapter<NotificationData, NotificationViewHolder>(NotificationItemDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemNotificationInfoBinding.inflate(inflater, parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationItemDiff : DiffUtil.ItemCallback<NotificationData>() {
        override fun areItemsTheSame(oldItem: NotificationData, newItem: NotificationData)
                = oldItem == newItem

        override fun areContentsTheSame(oldItem: NotificationData, newItem: NotificationData)
                = oldItem == newItem
    }
}

class NotificationViewHolder(val binding: ItemNotificationInfoBinding)
    : RecyclerView.ViewHolder(binding.root) {

    fun bind(data: NotificationData) {
        val dateString = FormatExtensions.formatDate(data.dateTime)

        binding.apply {
            title = itemView.context.getString(R.string.notification_item_title, dateString)
            count = data.exposureCount
            range = itemView.context.formatDateRange(data.exposureRangeStart, data.exposureRangeEnd)
        }
    }
}