package fi.thl.koronahaavi.exposure

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.*
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.getInputMethodManager
import fi.thl.koronahaavi.common.navigateSafe
import fi.thl.koronahaavi.common.viewScopedProperty
import fi.thl.koronahaavi.databinding.FragmentMunicipalityListBinding
import fi.thl.koronahaavi.databinding.ItemSimpleTextBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MunicipalityListFragment : Fragment(), SearchView.OnQueryTextListener {
    private var binding by viewScopedProperty<FragmentMunicipalityListBinding>()

    // activity scope because shared data between list and contact fragments
    private val viewModel by activityViewModels<MunicipalityListViewModel>()

    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.reloadMunicipalities()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMunicipalityListBinding.inflate(inflater, container, false).apply {
            this.model = viewModel
        }

        binding.lifecycleOwner = this.viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutToolbar.toolbar.setupWithNavController(findNavController())

        binding.searchMunicipality.apply {
            setOnQueryTextListener(this@MunicipalityListFragment)
            if (requestFocus()) {
                requireActivity().getInputMethodManager().toggleSoftInput(
                    InputMethodManager.SHOW_IMPLICIT,
                    InputMethodManager.HIDE_IMPLICIT_ONLY
                )
            }
        }

        val listAdapter = MunicipalityNameAdapter()
        binding.recyclerviewMunicipality.apply {
            adapter = listAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        viewModel.filteredNameList().observe(viewLifecycleOwner, {
            listAdapter.submitList(it)
        })

        binding.buttonMunicipalityRetryLoad.setOnClickListener {
            viewModel.reloadMunicipalities()
        }

        viewModel.search(null) // clear previous search
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        debounceSearch(query)
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        debounceSearch(newText)
        return true
    }

    // prevent search from being executed too frequently if user types fast
    private fun debounceSearch(term: String?) {
        searchJob?.cancel()
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(300)
            viewModel.search(term)
        }
    }
}

class MunicipalityNameAdapter : ListAdapter<MunicipalityNameData, MunicipalityViewHolder>(MunicipalityItemDiff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MunicipalityViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemSimpleTextBinding.inflate(inflater, parent, false)
        return MunicipalityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MunicipalityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MunicipalityItemDiff : DiffUtil.ItemCallback<MunicipalityNameData>() {
        override fun areItemsTheSame(oldItem: MunicipalityNameData, newItem: MunicipalityNameData)
                = oldItem.code == newItem.code

        override fun areContentsTheSame(oldItem: MunicipalityNameData, newItem: MunicipalityNameData)
                = oldItem == newItem
    }
}

class MunicipalityViewHolder(val binding: ItemSimpleTextBinding)
    : RecyclerView.ViewHolder(binding.root) {

    fun bind(m: MunicipalityNameData) {
        binding.text1.text = m.name

        binding.root.setOnClickListener {
            Timber.d("${m.name} clicked")
            it.findNavController().navigateSafe(MunicipalityListFragmentDirections.toContact(
                titleName = m.name,
                code = m.code
            ))
        }
    }
}