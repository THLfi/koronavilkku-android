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
import fi.thl.koronahaavi.common.navigateSafe
import fi.thl.koronahaavi.databinding.FragmentCountrySelectionListBinding
import fi.thl.koronahaavi.databinding.ItemCountrySelectBinding

@AndroidEntryPoint
class CountrySelectionListFragment : Fragment(), CountryItemListener {
    private lateinit var binding: FragmentCountrySelectionListBinding

    private val viewModel by navGraphViewModels<CodeEntryViewModel>(R.id.diagnosis_share_navigation) {
        defaultViewModelProviderFactory
    }

    private val listAdapter by lazy { CountryAdapter(this) }

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
            }

            buttonCountrySelectionContinue.setOnClickListener {
                findNavController().navigateSafe(CountrySelectionListFragmentDirections.toSummaryConsent())
            }
        }

        viewModel.shareData.countries.observe(viewLifecycleOwner, Observer {
            listAdapter.submitList(it)
        })
    }

    override fun onSelectedChanged(c: CountryData, isSelected: Boolean) {
        viewModel.shareData.setCountrySelection(c.code, isSelected)
    }
}

interface CountryItemListener {
    fun onSelectedChanged(c: CountryData, isSelected: Boolean)
}

class CountryAdapter(private val itemListener: CountryItemListener)
    : ListAdapter<CountryData, CountryViewHolder>(CountryItemDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCountrySelectBinding.inflate(inflater, parent, false)
        return CountryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CountryViewHolder, position: Int) {
        holder.bind(getItem(position), itemListener)
    }

    class CountryItemDiff : DiffUtil.ItemCallback<CountryData>() {
        override fun areItemsTheSame(oldItem: CountryData, newItem: CountryData)
                = oldItem.code == newItem.code

        override fun areContentsTheSame(oldItem: CountryData, newItem: CountryData)
                = oldItem == newItem
    }
}

class CountryViewHolder(val binding: ItemCountrySelectBinding)
    : RecyclerView.ViewHolder(binding.root) {

    fun bind(data: CountryData, itemListener: CountryItemListener) {
        with (binding) {
            label = data.name
            selected = data.isSelected

            checkboxCountry.setOnClickListener {
                itemListener.onSelectedChanged(data, checkboxCountry.isChecked)
            }
        }
    }
}