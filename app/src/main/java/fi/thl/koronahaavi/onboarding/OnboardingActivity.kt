package fi.thl.koronahaavi.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.common.RequestResolutionViewModel
import fi.thl.koronahaavi.common.withSavedLanguage
import fi.thl.koronahaavi.databinding.ActivityOnboardingBinding

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private val resolutionViewModel by viewModels<RequestResolutionViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        resolutionViewModel.handleActivityResult(requestCode, resultCode)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.withSavedLanguage())
    }
}