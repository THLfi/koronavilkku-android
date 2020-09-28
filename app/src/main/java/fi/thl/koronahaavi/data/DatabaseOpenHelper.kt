package fi.thl.koronahaavi.data

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteException
import net.sqlcipher.database.SQLiteOpenHelper
import timber.log.Timber

/**
 * modified from net.sqlcipher.database.SupportFactory to to use custom open helper
 */
class DatabaseOpenHelperFactory(private val passphrase: ByteArray)
    : SupportSQLiteOpenHelper.Factory {

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        return DatabaseOpenHelper(configuration, passphrase)
    }
}

/**
 * modified from net.sqlcipher.database.SupportHelper to add exception handling on open failures
 */
class DatabaseOpenHelper(
    private val configuration: SupportSQLiteOpenHelper.Configuration,
    private val passphrase: ByteArray
) : SupportSQLiteOpenHelper {

    init {
        SQLiteDatabase.loadLibs(configuration.context)
    }

    private val standardHelper = object : SQLiteOpenHelper(
        configuration.context,
        configuration.name,
        null,
        configuration.callback.version,
        null
    ) {
        override fun onCreate(db: SQLiteDatabase) {
            configuration.callback.onCreate(db)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            configuration.callback.onUpgrade(db, oldVersion, newVersion)
        }

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            configuration.callback.onDowngrade(db, oldVersion, newVersion)
        }

        override fun onOpen(db: SQLiteDatabase) {
            configuration.callback.onOpen(db)
        }

        override fun onConfigure(db: SQLiteDatabase) {
            configuration.callback.onConfigure(db)
        }
    }

    override fun getWritableDatabase(): SupportSQLiteDatabase {
        return try {
            standardHelper.getWritableDatabase(passphrase)
        } catch (e: SQLiteException) {
            Timber.e(e, "could not open database, attempting to recover by recreating database")
            configuration.context.deleteDatabase(databaseName)
            standardHelper.getWritableDatabase(passphrase) // retry once
        }
    }

    override fun close() = standardHelper.close()
    override fun getDatabaseName(): String? = standardHelper.databaseName
    override fun setWriteAheadLoggingEnabled(enabled: Boolean) = standardHelper.setWriteAheadLoggingEnabled(enabled)
    override fun getReadableDatabase(): SupportSQLiteDatabase = getWritableDatabase()
}