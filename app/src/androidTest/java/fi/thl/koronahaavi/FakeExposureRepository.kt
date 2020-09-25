package fi.thl.koronahaavi

import fi.thl.koronahaavi.data.Exposure
import fi.thl.koronahaavi.data.ExposureRepository
import fi.thl.koronahaavi.data.KeyGroupToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class FakeExposureRepository : ExposureRepository {
    override suspend fun getAllKeyGroupTokens(): List<KeyGroupToken>
            = listOf()

    override suspend fun saveKeyGroupToken(token: KeyGroupToken) {
    }

    override fun flowHandledKeyGroupTokens(): Flow<List<KeyGroupToken>> {
        return flowOf(listOf())
    }

    override suspend fun deleteKeyGroupToken(token: KeyGroupToken) {
    }

    override suspend fun saveExposure(exposure: Exposure) {
    }

    override suspend fun getExposure(id: Long): Exposure? {
        return null
    }

    override suspend fun getAllExposures(): List<Exposure> {
        return listOf()
    }

    override suspend fun deleteAllExposures() {
    }

    override suspend fun deleteExposure(id: Long) {
    }

    override fun flowAllExposures(): Flow<List<Exposure>> {
        return flowOf(listOf())
     }

    override fun flowHasExposures(): Flow<Boolean> {
        return flowOf(false)
    }
}