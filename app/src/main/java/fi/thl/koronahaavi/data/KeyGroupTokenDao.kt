package fi.thl.koronahaavi.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyGroupTokenDao {

    @Query("SELECT * FROM key_group_token")
    suspend fun getAll(): List<KeyGroupToken>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(token: KeyGroupToken)

    @Query("SELECT * FROM key_group_token WHERE matched_key_count NOT NULL ORDER BY updated_date ")
    fun flowHandled(): Flow<List<KeyGroupToken>>

    @Delete
    fun delete(vararg tokens: KeyGroupToken)
}