package fi.thl.koronahaavi.service

import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody

class FakeMunicipalityService : MunicipalityService {
    override suspend fun getMunicipalities(url: String): ResponseBody {
        return "".toResponseBody()
    }
}