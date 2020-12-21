package fi.thl.koronahaavi.diagnosis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.FormatExtensions.convertToCountryName
import fi.thl.koronahaavi.common.navigateSafe
import fi.thl.koronahaavi.databinding.FragmentCountrySelectionListBinding
import fi.thl.koronahaavi.databinding.ItemCountryListFooterBinding
import fi.thl.koronahaavi.databinding.ItemCountrySelectBinding
import timber.log.Timber

@AndroidEntryPoint
class CountrySelectionListFragment : Fragment(), CountryItemListener {
    private lateinit var binding: FragmentCountrySelectionListBinding

    private val viewModel by navGraphViewModels<CodeEntryViewModel>(R.id.diagnosis_share_navigation) {
        defaultViewModelProviderFactory
    }

    private val listAdapter by lazy { CountryAdapter(this, viewModel.shareData) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_country_selection_list, container, false)
        binding = FragmentCountrySelectionListBinding.bind(root).apply {
            this.model = viewModel.shareData
        }

        binding.lifecycleOwner = this.viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with (binding) {
            layoutToolbar.toolbar.setupWithNavController(findNavController())

            recyclerviewCountrySelection.apply {
                adapter = listAdapter
                layoutManager = LinearLayoutManager(context)
                setHasFixedSize(true)
            }
        }

        viewModel.shareData.countries.observe(viewLifecycleOwner, Observer {
            updateList(it)
        })
    }

    override fun onSelectedChanged(c: CountryItemData.Country, isSelected: Boolean) {
        Timber.d("onSelectedChanged ${c.name} is $isSelected")
        viewModel.shareData.setCountrySelection(c.code, isSelected)
    }

    override fun onContinue() {
        findNavController().navigateSafe(CountrySelectionListFragmentDirections.toSummaryConsent())
    }

    private fun updateList(countries: List<CountryData>) {
        // convert to display name and sort here instead of view model, so that list is
        // updated if fragment recreated due to language change
        val sortedCountries = countries.map { c -> c.toItemData() }.sortedBy { c -> c.name }

        val displayList = mutableListOf<CountryItemData>(CountryItemData.Header)
        displayList.addAll(sortedCountries)
        displayList.add(CountryItemData.Footer)

        listAdapter.submitList(displayList.toList())
    }

    private fun CountryData.toItemData(): CountryItemData.Country =
        CountryItemData.Country(
            code = code,
            name = code.convertToCountryName(),
            isSelected = isSelected
        )
}

sealed class CountryItemData {
    object Header : CountryItemData()
    object Footer : CountryItemData()
    data class Country(
            val code: String,
            val name: String,
            val isSelected: Boolean
    ) : CountryItemData()
}

interface CountryItemListener {
    fun onSelectedChanged(c: CountryItemData.Country, isSelected: Boolean)
    fun onContinue()
}

class CountryAdapter(
    private val itemListener: CountryItemListener,
    private val data: ShareTravelChoiceData
) : ListAdapter<CountryItemData, RecyclerView.ViewHolder>(CountryItemDiff()) {

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        CountryItemData.Header -> R.layout.item_country_list_header
        CountryItemData.Footer -> R.layout.item_country_list_footer
        is CountryItemData.Country -> R.layout.item_country_select
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        Timber.d("onCreateViewHolder")
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            R.layout.item_country_list_header -> {
                val view = inflater.inflate(viewType, parent, false)
                CountryListHeaderHolder(view)
            }
            R.layout.item_country_list_footer -> {
                val binding = ItemCountryListFooterBinding.inflate(inflater, parent, false)
                binding.buttonCountrySelectionContinue.setOnClickListener {
                    itemListener.onContinue()
                }
                CountryListFooterHolder(binding)
            }
            else -> {
                val binding = ItemCountrySelectBinding.inflate(inflater, parent, false)
                val holder = CountryViewHolder(binding)

                binding.checkboxCountry.setOnClickListener {
                    (getItem(holder.adapterPosition) as? CountryItemData.Country)?.let { data ->
                        itemListener.onSelectedChanged(data, binding.checkboxCountry.isChecked)
                    }
                }

                holder
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        Timber.d("onViewRecycled")
        (holder as? CountryViewHolder)?.clear()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        Timber.d("onBindViewHolder $position")
        when (holder) {
            is CountryViewHolder -> (getItem(position) as? CountryItemData.Country)?.let {
                holder.bind(it)
            }
            is CountryListFooterHolder -> holder.bind(data)
        }
    }

    class CountryItemDiff : DiffUtil.ItemCallback<CountryItemData>() {
        override fun areItemsTheSame(oldItem: CountryItemData, newItem: CountryItemData): Boolean {
            // this matches header and footer, and countries with same contents
            if (oldItem == newItem) return true

            // this matches same countries but different content
            return (oldItem as? CountryItemData.Country)?.let { oldCountry ->
                (newItem as? CountryItemData.Country)?.let { newCountry ->
                    oldCountry.code == newCountry.code
                }
            } ?: false
        }

        override fun areContentsTheSame(oldItem: CountryItemData, newItem: CountryItemData)
                = oldItem == newItem
    }
}

class CountryListHeaderHolder(v: View) : RecyclerView.ViewHolder(v)

class CountryListFooterHolder(val binding: ItemCountryListFooterBinding)
    : RecyclerView.ViewHolder(binding.root) {

    fun bind(data: ShareTravelChoiceData) {
        binding.model = data
    }
}

class CountryViewHolder(val binding: ItemCountrySelectBinding)
    : RecyclerView.ViewHolder(binding.root) {

    fun bind(data: CountryItemData.Country) {
        binding.label = data.name
        binding.selected = data.isSelected
    }

    fun clear() {
        binding.selected = false
    }
}