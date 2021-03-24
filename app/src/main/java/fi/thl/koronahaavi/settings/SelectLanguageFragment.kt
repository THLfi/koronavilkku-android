package fi.thl.koronahaavi.settings

import android.content.res.ColorStateList
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
import fi.thl.koronahaavi.common.changeLanguage
import fi.thl.koronahaavi.common.viewScopedProperty
import fi.thl.koronahaavi.databinding.FragmentSelectLanguageBinding
import fi.thl.koronahaavi.onboarding.OnboardingActivity
import javax.inject.Inject

@AndroidEntryPoint
class SelectLanguageFragment: Fragment() {

    @Inject
    lateinit var userPreferences: UserPreferences

    private var binding by viewScopedProperty<FragmentSelectLanguageBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSelectLanguageBinding.inflate(inflater, container, false)
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

        val currentlySelected = userPreferences.language

        supportedLanguages.forEach { pair ->
            val indicator = pair.second.linkItemCard.findViewById<ImageView>(R.id.link_item_indicator)

            if (pair.first == currentlySelected) {
                indicator.visibility = View.VISIBLE
                indicator.setImageResource(R.drawable.ic_check)
                indicator.imageTintList = ColorStateList.valueOf(requireContext().getColor(R.color.mainBlue))
                indicator.contentDescription = getString(R.string.select_language_item_selected_description)
            } else {
                indicator.visibility = View.INVISIBLE // Instead of GONE so that the height remains the same w/ or w/o check mark.

                pair.second.linkItemCard.setOnClickListener {
                    selectLanguage(pair.first)
                }
            }
        }
    }

    private fun selectLanguage(language: String?) {
        activity?.apply {
            userPreferences.changeLanguage(language)

            if (activity is OnboardingActivity) {
                findNavController().popBackStack()
            }

            recreate()
        }
    }
}