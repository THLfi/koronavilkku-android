package fi.thl.koronahaavi.data

import kotlinx.coroutines.flow.Flow

interface ExposureRepository {
    suspend fun getAllKeyGroupTokens(): List<KeyGroupToken>
    suspend fun saveKeyGroupToken(token: KeyGroupToken)
    suspend fun deleteKeyGroupToken(token: KeyGroupToken)
    suspend fun deleteAllKeyGroupTokens()
    suspend fun saveExposure(exposure: Exposure)
    suspend fun getExposure(id: Long): Exposure?
    suspend fun getAllExposures(): List<Exposure>
    suspend fun deleteAllExposures()
    suspend fun deleteExpiredExposuresAndTokens()
    suspend fun deleteExposure(id: Long)
    fun getIsExposedFlow(): Flow<Boolean>
    fun getExposureNotificationsFlow(): Flow<List<ExposureNotification>>
}