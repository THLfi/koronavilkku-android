package fi.thl.koronahaavi.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.ZonedDateTime

/**
 * A string token that identifies a group of diagnosis keys, retrieved from backend server
 * and provided to exposure api using this string token as identifier
 */
@Entity(tableName = "key_group_token")
data class KeyGroupToken (
    @PrimaryKey val token: String,
    @ColumnInfo(name = "updated_date") val updatedDate: ZonedDateTime = ZonedDateTime.now(),
    @ColumnInfo(name = "matched_key_count") val matchedKeyCount: Int? = null,
    @ColumnInfo(name = "maximum_risk_score") val maximumRiskScore: Int? = null
)