package fi.thl.koronahaavi.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.databinding.FragmentChoiceBinding

/**
 * Base fragment for a single boolean choice and conditional navigation
 */
@AndroidEntryPoint
abstract class ChoiceFragment : Fragment() {
    private lateinit var binding: FragmentChoiceBinding
    private lateinit var viewModel: ChoiceData

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_choice, container, false)

        viewModel = getChoiceViewModel()
        binding = FragmentChoiceBinding.bind(root).apply {
            this.model = viewModel
        }

        binding.lifecycleOwner = this.viewLifecycleOwner

        binding.textChoiceHeader.text = getString(headerTextId)
        bodyTextId?.let { binding.textChoiceBody.text = getString(it) }

        binding.radio1.text = getString(firstChoiceTextId)
        binding.radio2.text = getString(secondChoiceTextId)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutToolbar.toolbar.setupWithNavController(findNavController())

        binding.buttonChoiceContinue.setOnClickListener {
            viewModel.selectedChoice.value?.let {
                findNavController().navigateSafe(getNextDirections(it))
            }
        }
    }

    @get:StringRes abstract val headerTextId: Int
    @get:StringRes abstract val bodyTextId: Int?
    @get:StringRes abstract val firstChoiceTextId: Int
    @get:StringRes abstract val secondChoiceTextId: Int

    abstract fun getChoiceViewModel(): ChoiceData
    abstract fun getNextDirections(choice: ChoiceData.Choice): NavDirections
}