package fi.thl.koronahaavi.service

import android.os.Build
import fi.thl.koronahaavi.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor: Interceptor {

    private val userAgent: String = generateUserAgentString()

    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(chain.request().newBuilder()
            .addHeader("User-Agent", userAgent)
            .build())
    }
}

private fun generateUserAgentString(): String {
    var tmp = "Koronavilkku/${BuildConfig.VERSION_NAME}"

    if (BuildConfig.DEBUG) {
        tmp += " (Android ${Build.VERSION.RELEASE}; ${Build.MANUFACTURER} ${Build.MODEL})"
    }

    return tmp
}
