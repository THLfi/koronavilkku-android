package fi.thl.koronahaavi.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.ZonedDateTime

class DefaultExposureRepository (
    private val keyGroupTokenDao: KeyGroupTokenDao,
    private val exposureDao: ExposureDao,
    private val settingsRepository: SettingsRepository
) : ExposureRepository {

    override suspend fun getAllKeyGroupTokens(): List<KeyGroupToken> = keyGroupTokenDao.getAll()

    override suspend fun saveKeyGroupToken(token: KeyGroupToken) = keyGroupTokenDao.insert(token)

    override fun flowHandledKeyGroupTokens() = keyGroupTokenDao.getHandledFlow()

    override suspend fun deleteKeyGroupToken(token: KeyGroupToken) = keyGroupTokenDao.delete(token)

    override suspend fun deleteAllKeyGroupTokens() = keyGroupTokenDao.deleteAll()

    override suspend fun saveExposure(exposure: Exposure) = exposureDao.insert(exposure)

    override suspend fun getExposure(id: Long) = exposureDao.get(id)

    override suspend fun getAllExposures(): List<Exposure> = exposureDao.getAll()

    override suspend fun deleteAllExposures() = exposureDao.deleteAll()

    override suspend fun deleteExposure(id: Long) = exposureDao.delete(id)

    override suspend fun deleteExpiredExposuresAndTokens() {
        val detectionStart = getDetectionStart()

        getAllExposures().forEach {
            if (it.detectedDate.isBefore(detectionStart)) {
                Timber.d("Deleting exposure detected ${it.detectedDate}")
                deleteExposure(it.id)
            }
        }

        getAllKeyGroupTokens().forEach {
            // also check updatedDate for legacy data that does not have latestExposureDate
            if (it.latestExposureDate?.isBefore(detectionStart) == true || it.updatedDate.isBefore(detectionStart)) {
                Timber.d("Deleting key group updated ${it.updatedDate}")
                deleteKeyGroupToken(it)
            }
        }
    }

    override fun getIsExposedFlow(): Flow<Boolean> =
        exposureDao.flowAll().map { exposures ->
            val detectionStart = getDetectionStart()

            exposures.any {
                it.detectedDate.isAfter(detectionStart)
            }
        }

    override fun getExposureNotificationsFlow(): Flow<List<ExposureNotification>> =
        keyGroupTokenDao.getExposureTokensFlow().map { groupTokens ->
            val detectionStart = getDetectionStart()

            groupTokens.filter {
                it.latestExposureDate?.isAfter(detectionStart) == true
            }.mapNotNull {
                it.exposureCount?.let { count ->
                    ExposureNotification(
                        createdDate = it.updatedDate,
                        exposureCount = count
                    )
                }
            }
        }

    private fun getDetectionStart(): ZonedDateTime {
        // Exposure detection timestamp is rounded by EN to beginning of day in UTC, so in order to keep
        // exposure valid for configured number of days, we need to keep it a day longer from detection timestamp
        val validSinceDetectionDays = settingsRepository.appConfiguration.exposureValidDays + 1

        return ZonedDateTime.now().minusDays(validSinceDetectionDays.toLong())
    }
}