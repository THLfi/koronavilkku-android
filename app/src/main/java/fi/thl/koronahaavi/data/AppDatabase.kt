package fi.thl.koronahaavi.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Database(
    entities = [KeyGroupToken::class, Exposure::class],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun keyGroupTokenDao(): KeyGroupTokenDao
    abstract fun exposureDao(): ExposureDao
}

class Converters {
    @TypeConverter
    fun longToZonedDateTime(value: Long?): ZonedDateTime? {
        return value?.let { ZonedDateTime.from(
            Instant.ofEpochSecond(it).atOffset(ZoneOffset.UTC))
        }
    }

    @TypeConverter
    fun zonedDateTimeToLong(time: ZonedDateTime?): Long? {
        return time?.toInstant()?.epochSecond
    }
}