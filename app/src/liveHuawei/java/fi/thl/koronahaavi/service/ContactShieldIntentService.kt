package fi.thl.koronahaavi.service

import android.app.IntentService
import android.content.Intent
import com.huawei.hms.contactshield.ContactShield
import com.huawei.hms.contactshield.ContactShieldCallback
import fi.thl.koronahaavi.exposure.ExposureUpdateWorker
import timber.log.Timber

class ContactShieldIntentService : IntentService("ContactShield_BackgroundContactCheckingIntentService") {
    private val engine by lazy {
        ContactShield.getContactShieldEngine(this)
    }

    override fun onHandleIntent(intent: Intent?) {
        Timber.d("onHandleIntent $intent")

        intent?.let {
            engine.handleIntent(it, object : ContactShieldCallback {
                override fun onHasContact(p0: String?) {
                    Timber.d("onHasContact")
                    p0?.let { token ->
                        Timber.d("start ExposureUpdateWorker with $token")
                        ExposureUpdateWorker.start(this@ContactShieldIntentService, token)
                    }
                }

                override fun onNoContact(p0: String?) {
                    Timber.d("onNoContact")
                }
            })
        }
    }
}
