package fi.thl.koronahaavi.exposure

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.*
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.navigateSafe
import fi.thl.koronahaavi.databinding.FragmentContactBinding
import fi.thl.koronahaavi.databinding.ItemContactInfoBinding

@AndroidEntryPoint
class ContactFragment : Fragment(), ContactItemListener {
    private lateinit var binding: FragmentContactBinding

    private val viewModel by activityViewModels<MunicipalityListViewModel>()
    private val args by navArgs<ContactFragmentArgs>()
    private val listAdapter by lazy { ContactAdapter(this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_contact, container, false)
        binding = FragmentContactBinding.bind(root).apply {
            this.municipality = viewModel.getMunicipality(args.code)
        }

        binding.lifecycleOwner = this.viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutToolbar.toolbar.setupWithNavController(findNavController())

        binding.recyclerviewContact.apply {
            adapter = listAdapter
            layoutManager = LinearLayoutManager(context)
        }

        binding.buttonContactEvaluationStart.setOnClickListener {
            findNavController().navigateSafe(ContactFragmentDirections.toPreWebContact(
                startEvaluation = true,
                municipalityCode = args.code
            ))
        }

        binding.cardContactWeb.linkItemCard.setOnClickListener {
            findNavController().navigateSafe(ContactFragmentDirections.toPreWebContact(
                startEvaluation = false,
                municipalityCode = args.code
            ))
        }

        listAdapter.submitList(binding.municipality?.contacts)
    }

    override fun onClicked(c: MunicipalityContact) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${c.phoneNumber}")
        }
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        }
    }
}

interface ContactItemListener {
    fun onClicked(c: MunicipalityContact)
}

class ContactAdapter(private val itemListener: ContactItemListener? = null)
    : ListAdapter<MunicipalityContact, ContactViewHolder>(ContactItemDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemContactInfoBinding.inflate(inflater, parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position), itemListener)
    }

    class ContactItemDiff : DiffUtil.ItemCallback<MunicipalityContact>() {
        override fun areItemsTheSame(oldItem: MunicipalityContact, newItem: MunicipalityContact)
                = oldItem == newItem

        override fun areContentsTheSame(oldItem: MunicipalityContact, newItem: MunicipalityContact)
                = oldItem == newItem
    }
}

class ContactViewHolder(val binding: ItemContactInfoBinding)
    : RecyclerView.ViewHolder(binding.root) {

    fun bind(c: MunicipalityContact, itemListener: ContactItemListener?) {
        binding.apply {
            header = c.title
            label = c.phoneNumber
            footer = c.info
            icon = ContextCompat.getDrawable(itemView.context, R.drawable.ic_phone)
        }

        binding.root.setOnClickListener {
            itemListener?.onClicked(c)
        }
    }
}