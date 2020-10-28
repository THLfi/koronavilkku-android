package fi.thl.koronahaavi.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.Duration
import java.time.ZonedDateTime

class DefaultExposureRepository (
    private val keyGroupTokenDao: KeyGroupTokenDao,
    private val exposureDao: ExposureDao
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

    //override fun flowHasExposures(): Flow<Boolean> = exposureDao.flowCount().map { it > 0 }

    override fun flowExposureNotifications(): Flow<List<ExposureNotification>> =
        exposureDao.flowAll().map { all ->
            // first filter out expired exposures, then group by created date, but because
            // created date could theoretically be on a different second, group manually
            // instead of using created date directly as a map key

            val result = mutableMapOf<ZonedDateTime, List<Exposure>>()
            val expireTime = getExpireTime()

            all.filter { e ->
                e.detectedDate.isAfter(expireTime)
            }.forEach { e ->
                // find existing key that is close enough, or create new key
                val key = result.keys.find {
                    Duration.between(it, e.createdDate).abs().seconds < 2
                } ?: e.createdDate

                // append to existing, or create a new entry
                result[key] = result.getOrDefault(key, listOf()).plus(e)
            }

            result.map { ExposureNotification(it.key, it.value) }
        }

    private fun getExpireTime() = ZonedDateTime.now().minusDays(EXPOSURE_VALID_SINCE_DETECTION_DAYS.toLong())

    companion object {
        // Exposure detection timestamp is rounded by EN to beginning of day in UTC, so in order to keep
        // exposure valid for 10 days, we keep it for 11 days from detection timestamp
        const val EXPOSURE_VALID_SINCE_DETECTION_DAYS = 11
    }
}