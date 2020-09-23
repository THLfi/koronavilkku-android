package fi.thl.koronahaavi.data

import kotlinx.coroutines.flow.Flow

interface ExposureRepository {
    suspend fun getAllKeyGroupTokens(): List<KeyGroupToken>
    suspend fun saveKeyGroupToken(token: KeyGroupToken)
    fun flowHandledKeyGroupTokens(): Flow<List<KeyGroupToken>>
    suspend fun deleteKeyGroupToken(token: KeyGroupToken)
    suspend fun saveExposure(exposure: Exposure)
    suspend fun getExposure(id: Long): Exposure?
    suspend fun getAllExposures(): List<Exposure>
    suspend fun deleteAllExposures()
    suspend fun deleteExposure(id: Long)
    fun flowAllExposures(): Flow<List<Exposure>>
    fun flowHasExposures(): Flow<Boolean>
}