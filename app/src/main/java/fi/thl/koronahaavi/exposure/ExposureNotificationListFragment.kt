package fi.thl.koronahaavi.exposure

import android.os.Bundle
import android.text.format.DateUtils
import android.text.format.DateUtils.FORMAT_NUMERIC_DATE
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.FormatExtensions
import fi.thl.koronahaavi.common.FormatExtensions.formatLastCheckTime
import fi.thl.koronahaavi.databinding.FragmentExposureNotificationListBinding
import fi.thl.koronahaavi.databinding.ItemNotificationInfoBinding
import timber.log.Timber

@AndroidEntryPoint
class ExposureNotificationListFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentExposureNotificationListBinding

    private val viewModel: ExposureDetailViewModel by navGraphViewModels(R.id.exposure_navigation) {
        defaultViewModelProviderFactory
    }
    private val listAdapter by lazy { NotificationAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Using activity.layoutInflater instead of inflater because otherwise the button style
        // isn't the one defined in styles - https://issuetracker.google.com/issues/37042151
        val root = requireActivity().layoutInflater.inflate(R.layout.fragment_exposure_notification_list, container, false)
        binding = FragmentExposureNotificationListBinding.bind(root).apply {
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

        viewModel.lastCheckTime.observe(viewLifecycleOwner, Observer {
            binding.textNotificationListLastCheck.text = requireContext().formatLastCheckTime(it)
        })

        viewModel.notifications.observe(viewLifecycleOwner, Observer {
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

    fun bind(notification: NotificationData) {
        val dateString = FormatExtensions.formatDate(notification.dateTime)

        binding.apply {
            title = itemView.context.getString(R.string.notification_item_title, dateString)
            count = notification.notificationCount
            range = DateUtils.formatDateRange(itemView.context,
                notification.dateTime.minusDays(10).toInstant().toEpochMilli(),
                notification.dateTime.toInstant().toEpochMilli(),
                FORMAT_NUMERIC_DATE
            )
        }
    }
}