package fi.thl.koronahaavi.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.ZonedDateTime

@Entity(tableName = "exposure")
data class Exposure (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // epoch ms rounded to the day (assuming in UTC?) when EN has detected the exposure
    @ColumnInfo(name = "detected_date")
    val detectedDate: ZonedDateTime,

    // timestamp when app received the info, and when user was notified
    @ColumnInfo(name = "created_date")
    val createdDate: ZonedDateTime,

    // EN calculation result
    @ColumnInfo(name = "total_risk_score")
    val totalRiskScore: Int
)
