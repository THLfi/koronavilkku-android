package fi.thl.koronahaavi.exposure

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fi.thl.koronahaavi.BuildConfig
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.databinding.FragmentPreWebContactBinding

class PreWebContactFragment : Fragment() {
    private lateinit var binding: FragmentPreWebContactBinding
    private val viewModel by activityViewModels<MunicipalityListViewModel>()
    private val args by navArgs<PreWebContactFragmentArgs>()
    private var municipality: Municipality? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_pre_web_contact, container, false)
        municipality = viewModel.getMunicipality(args.municipalityCode)

        binding = FragmentPreWebContactBinding.bind(root).apply {
            this.model = PreWebContactData.from(args.startEvaluation, municipality)
        }

        binding.lifecycleOwner = this.viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutToolbar.toolbar.setupWithNavController(findNavController())

        binding.cardPreWebLangFi.linkItemCard.setOnClickListener {
            openOmaolo("fi")
        }

        binding.cardPreWebLangSv.linkItemCard.setOnClickListener {
            openOmaolo("sv")
        }

        binding.cardPreWebLangEn.linkItemCard.setOnClickListener {
            openOmaolo("en")
        }
    }

    private fun openOmaolo(lang: String) {
        val targetPath = if (args.startEvaluation) {
            "/palvelut/oirearviot/649?lang=$lang"
        }
        else {
            "/yhteydenotto?lang=$lang"
        }

        val uri = Uri.parse(BuildConfig.OMAOLO_URL)
            .buildUpon()
            .appendQueryParameter(OMAOLO_MUNICIPALITY_PARAM, municipality?.code)
            .appendQueryParameter(OMAOLO_TARGET_PARAM, targetPath)
            .build()

        val intent = Intent(Intent.ACTION_VIEW).apply { data = uri }

        activity?.let {
            if (intent.resolveActivity(it.packageManager) != null) {
                startActivity(intent)
            }
            else {
                // device does not have any apps that can open a web link, maybe
                // browser app disabled, or profile restrictions in place
                MaterialAlertDialogBuilder(it)
                    .setTitle(R.string.omaolo_link_failed_title)
                    .setMessage(R.string.omaolo_link_failed_message)
                    .setPositiveButton(R.string.all_ok, null)
                    .show()
            }
        }
    }

    companion object {
        const val OMAOLO_MUNICIPALITY_PARAM = "municipalityCode"
        const val OMAOLO_TARGET_PARAM = "returnUrl"
    }
}

data class PreWebContactData(
    val startEvaluation: Boolean,
    val showFinnish: Boolean,
    val showSwedish: Boolean,
    val showEnglish: Boolean
) {
    companion object {
        fun from(startEvaluation: Boolean, m: Municipality?): PreWebContactData {
            val languages = m?.omaoloFeatures?.serviceLanguages
            return PreWebContactData(
                startEvaluation = startEvaluation,
                showFinnish = languages?.fi ?: false,
                showSwedish = languages?.sv ?: false,
                showEnglish = languages?.en ?: false
            )
        }
    }
}