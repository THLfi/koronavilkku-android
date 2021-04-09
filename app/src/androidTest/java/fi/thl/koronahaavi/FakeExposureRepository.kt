package fi.thl.koronahaavi

import fi.thl.koronahaavi.data.Exposure
import fi.thl.koronahaavi.data.ExposureNotification
import fi.thl.koronahaavi.data.ExposureRepository
import fi.thl.koronahaavi.data.KeyGroupToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class FakeExposureRepository : ExposureRepository {
    override suspend fun getAllKeyGroupTokens(): List<KeyGroupToken> = listOf()

    override suspend fun saveKeyGroupToken(token: KeyGroupToken) {
    }

    override suspend fun deleteKeyGroupToken(token: KeyGroupToken) {
    }

    override suspend fun deleteAllKeyGroupTokens() {
    }

    override suspend fun saveExposure(exposure: Exposure) {
    }

    override suspend fun getExposure(id: Long): Exposure? = null
    override suspend fun getAllExposures(): List<Exposure> = listOf()

    override suspend fun deleteAllExposures() {
    }

    override suspend fun deleteExposure(id: Long) {
    }

    override suspend fun deleteExpiredExposuresAndTokens() {
    }

    override fun getIsExposedFlow(): Flow<Boolean> = flowOf(false)
    override fun getExposureNotificationsFlow(): Flow<List<ExposureNotification>> = flowOf(listOf())
}