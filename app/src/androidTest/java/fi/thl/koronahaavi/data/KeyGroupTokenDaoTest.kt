package fi.thl.koronahaavi.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class KeyGroupTokenDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var keyGroupTokenDao: KeyGroupTokenDao

    @Before
    fun init() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        keyGroupTokenDao = db.keyGroupTokenDao()
    }

    @After
    fun cleanup() {
        db.close()
    }

    @Test
    fun writeAndUpdateToken() {
        val token = KeyGroupToken("a")
        runBlocking {
            keyGroupTokenDao.insert(token)

            val updateToken = token.copy(updatedDate = ZonedDateTime.now(), matchedKeyCount = 1, maximumRiskScore = 200)
            keyGroupTokenDao.insert(updateToken)

            val tokens = keyGroupTokenDao.getAll()
            Assert.assertEquals(1, tokens.size)
            Assert.assertEquals(token.token, tokens[0].token)
        }
    }

    @Test
    fun deleteTokens() {
        val tokenA = KeyGroupToken("a")
        val tokenB = KeyGroupToken("b")
        val tokenC = KeyGroupToken("c")

        runBlocking {
            keyGroupTokenDao.insert(tokenA)
            keyGroupTokenDao.insert(tokenB)
            keyGroupTokenDao.insert(tokenC)

            val updateTokenA = tokenA.copy(updatedDate = ZonedDateTime.now(), matchedKeyCount = 1, maximumRiskScore = 100)
            keyGroupTokenDao.insert(updateTokenA)

            val updateTokenB = tokenB.copy(updatedDate = ZonedDateTime.now(), matchedKeyCount = 2, maximumRiskScore = 200)
            keyGroupTokenDao.insert(updateTokenB)

            keyGroupTokenDao.delete(tokenA, tokenB)

            val tokens = keyGroupTokenDao.getAll()
            Assert.assertEquals(1, tokens.size)
            Assert.assertEquals(tokenC.token, tokens[0].token)
        }
    }
}