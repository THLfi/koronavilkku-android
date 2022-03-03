package fi.thl.koronahaavi.shutdown

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.data.SettingsRepository
import fi.thl.koronahaavi.databinding.ActivityShutdownBinding
import fi.thl.koronahaavi.databinding.ItemLabeledValueBinding
import fi.thl.koronahaavi.service.ExposureNotificationService
import fi.thl.koronahaavi.service.LabeledStringValue
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ShutdownActivity : AppCompatActivity() {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var exposureNotificationService: ExposureNotificationService

    private val listAdapter by lazy { LabeledValueAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityShutdownBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.recyclerviewShutdownMessages.apply {
            adapter = listAdapter
            layoutManager = LinearLayoutManager(context)
        }

        lifecycleScope.launch {
            settingsRepository.getExposureConfiguration()?.endOfLifeStatistics?.let {
                listAdapter.submitList(it)
            }

            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // make sure EN API is disabled, in case it has been turned on in settings
                exposureNotificationService.disable()
            }
        }
    }
}

class LabeledValueAdapter : ListAdapter<LabeledStringValue, LabeledValueViewHolder>(LabeledStringValueItemDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabeledValueViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemLabeledValueBinding.inflate(inflater, parent, false)
        return LabeledValueViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LabeledValueViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LabeledStringValueItemDiff : DiffUtil.ItemCallback<LabeledStringValue>() {
        override fun areItemsTheSame(oldItem: LabeledStringValue, newItem: LabeledStringValue)
                = oldItem == newItem

        override fun areContentsTheSame(oldItem: LabeledStringValue, newItem: LabeledStringValue)
                = oldItem == newItem
    }
}

class LabeledValueViewHolder(val binding: ItemLabeledValueBinding)
    : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: LabeledStringValue) {
        binding.label = item.label.getLocal()
        binding.value = item.value
    }
}