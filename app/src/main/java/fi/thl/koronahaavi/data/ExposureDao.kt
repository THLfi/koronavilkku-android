package fi.thl.koronahaavi.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExposureDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exposure: Exposure)

    @Query("SELECT * FROM exposure")
    suspend fun getAll(): List<Exposure>

    @Query("SELECT * FROM exposure WHERE id = :id")
    suspend fun get(id: Long): Exposure?

    @Query("SELECT * FROM exposure")
    fun flowAll(): Flow<List<Exposure>>

    @Query("SELECT count(*) FROM exposure")
    fun flowCount(): Flow<Int>

    @Query("DELETE FROM exposure")
    suspend fun deleteAll()

    @Query("DELETE FROM exposure WHERE id = :id")
    suspend fun delete(id: Long)
}