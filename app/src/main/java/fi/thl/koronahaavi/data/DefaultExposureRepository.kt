package fi.thl.koronahaavi.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.Duration
import java.time.ZonedDateTime

class DefaultExposureRepository (
    private val keyGroupTokenDao: KeyGroupTokenDao,
    private val exposureDao: ExposureDao,
    private val settingsRepository: SettingsRepository
) : ExposureRepository {

    override suspend fun getAllKeyGroupTokens(): List<KeyGroupToken> = keyGroupTokenDao.getAll()

    override suspend fun saveKeyGroupToken(token: KeyGroupToken) = keyGroupTokenDao.insert(token)

    override fun flowHandledKeyGroupTokens() = keyGroupTokenDao.flowHandled()

    override suspend fun deleteKeyGroupToken(token: KeyGroupToken) = keyGroupTokenDao.delete(token)

    override suspend fun saveExposure(exposure: Exposure) = exposureDao.insert(exposure)

    override suspend fun getExposure(id: Long) = exposureDao.get(id)

    override suspend fun getAllExposures(): List<Exposure> = exposureDao.getAll()

    override suspend fun deleteAllExposures() = exposureDao.deleteAll()

    override suspend fun deleteExposure(id: Long) = exposureDao.delete(id)

    override suspend fun deleteExpiredExposuresAndTokens() {
        val expireTime = getExpireTime()

        getAllExposures().forEach {
            if (it.detectedDate.isBefore(expireTime)) {
                Timber.d("Deleting exposure detected ${it.detectedDate}")
                deleteExposure(it.id)
            }
        }

        getAllKeyGroupTokens().forEach {
            if (it.updatedDate.isBefore(expireTime)) {
                Timber.d("Deleting key group updated ${it.updatedDate}")
                deleteKeyGroupToken(it)
            }
        }
    }

    override fun flowAllExposures(): Flow<List<Exposure>> = exposureDao.flowAll()

    override fun flowExposureNotifications(): Flow<List<ExposureNotification>> =
        exposureDao.flowAll().map { all ->
            // First filter out expired exposures, then group by created date, but because
            // created date could theoretically be on a different second, group manually
            // instead of using created date directly as a map key.

            val result = mutableMapOf<ZonedDateTime, List<Exposure>>()
            val expireTime = getExpireTime()

            all.filter { e ->
                e.detectedDate.isAfter(expireTime)
            }.forEach { e ->
                // find existing key that is close enough, or create new key
                val key = result.keys.find {
                    Duration.between(it, e.createdDate).abs().seconds < EXPOSURE_GROUPING_THRESHOLD_SECONDS
                } ?: e.createdDate

                // append to existing, or create a new entry
                result[key] = result.getOrDefault(key, listOf()).plus(e)
            }

            result.map { ExposureNotification(it.key, it.value) }
        }

    private fun getExpireTime(): ZonedDateTime {
        // Exposure detection timestamp is rounded by EN to beginning of day in UTC, so in order to keep
        // exposure valid for configured number of days, we need to keep it a day longer from detection timestamp
        val validSinceDetectionDays = settingsRepository.appConfiguration.exposureValidDays + 1

        return ZonedDateTime.now().minusDays(validSinceDetectionDays.toLong())
    }

    companion object {
        // Exposures created during same notification in earlier versions might have a very small difference
        // in creation time, so we're allowing few seconds for that. Since v1.3 we use the same timestamp
        // for all exposures within a notification, so this threshold is only for legacy support
        const val EXPOSURE_GROUPING_THRESHOLD_SECONDS = 5
    }
}