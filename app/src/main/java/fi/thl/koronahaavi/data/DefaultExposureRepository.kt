package fi.thl.koronahaavi.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultExposureRepository (
    private val keyGroupTokenDao: KeyGroupTokenDao,
    private val exposureDao: ExposureDao
) : ExposureRepository {

    override suspend fun saveKeyGroupToken(token: KeyGroupToken)
            = keyGroupTokenDao.insert(token)

    override fun flowHandledKeyGroupTokens()
            = keyGroupTokenDao.flowHandled()

    override suspend fun deleteKeyGroupToken(token: KeyGroupToken)
            = keyGroupTokenDao.delete(token)

    override suspend fun saveExposure(exposure: Exposure)
            = exposureDao.insert(exposure)

    override suspend fun getExposure(id: Long)
            = exposureDao.get(id)

    override suspend fun getAllExposures(): List<Exposure>
            = exposureDao.getAll()

    override suspend fun deleteAllExposures()
            = exposureDao.deleteAll()

    override suspend fun deleteExposure(id: Long)
            = exposureDao.delete(id)

    override fun flowAllExposures(): Flow<List<Exposure>>
            = exposureDao.flowAll()

    override fun flowHasExposures(): Flow<Boolean>
            = exposureDao.flowCount().map { it > 0 }
}