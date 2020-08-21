package fi.thl.koronahaavi.service

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface MunicipalityService {
    @Streaming
    @GET
    suspend fun getMunicipalities(@Url url: String): ResponseBody
}