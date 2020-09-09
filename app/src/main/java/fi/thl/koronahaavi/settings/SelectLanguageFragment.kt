package fi.thl.koronahaavi.settings

import android.content.res.ColorStateList
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.getSavedLanguage
import fi.thl.koronahaavi.common.primaryLocale
import fi.thl.koronahaavi.common.setSavedLanguage
import fi.thl.koronahaavi.databinding.FragmentSelectLanguageBinding

@AndroidEntryPoint
class SelectLanguageFragment: Fragment() {

    private lateinit var binding: FragmentSelectLanguageBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_select_language, container, false)
        binding = FragmentSelectLanguageBinding.bind(root)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.layoutToolbar.toolbar.setupWithNavController(findNavController())

        val supportedLanguages = listOf(
            Pair("fi", binding.selectLanguageItemFinnish),
            Pair("sv", binding.selectLanguageItemSwedish),
            Pair("en", binding.selectLanguageItemEnglish),
            Pair(null, binding.selectLanguageItemSystemDefault)
        )

        val currentlySelected = requireContext().getSavedLanguage()

        supportedLanguages.forEach { pair ->
            val indicator = pair.second.linkItemCard.findViewById<ImageView>(R.id.link_item_indicator)

            if (pair.first == currentlySelected) {
                indicator.visibility = View.VISIBLE
                indicator.setImageResource(R.drawable.ic_check)
                indicator.imageTintList = ColorStateList.valueOf(requireContext().getColor(R.color.mainBlue))
                indicator.contentDescription = getString(R.string.select_language_item_selected_description)
            } else {
                indicator.visibility = View.GONE
            }

            pair.second.linkItemCard.setOnClickListener {
                selectLanguage(pair.first)
            }
        }
    }

    private fun selectLanguage(language: String?) {
        activity?.apply {

            if (language == null) {
                // Restore system primary locale as Context.withSavedLanguage() doesn't do anything
                // if saved language is null. Without this the previously set locale (fi/sv/en)
                // would be used (for formatting - the UI language would be correct).
                Locale.setDefault(Resources.getSystem().primaryLocale())
            }

            setSavedLanguage(language)
            recreate()
        }
    }
}