package fi.thl.koronahaavi.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.ZonedDateTime

/**
 * Identifies a set of diagnosis keys retrieved from backend server, and the possible exposure
 * notification shown to user based on that set of keys.
 */
@Entity(tableName = "key_group_token")
data class KeyGroupToken (
    @PrimaryKey val token: String,
    @ColumnInfo(name = "updated_date") val updatedDate: ZonedDateTime = ZonedDateTime.now(),
    @ColumnInfo(name = "matched_key_count") val matchedKeyCount: Int? = null,
    @ColumnInfo(name = "maximum_risk_score") val maximumRiskScore: Int? = null,

    // Count of exposures
    @ColumnInfo(name = "exposure_count") val exposureCount: Int? = null,

    // Detection timestamp for the latest exposure in this group
    @ColumnInfo(name = "latest_exposure_date") val latestExposureDate: ZonedDateTime? = null
)