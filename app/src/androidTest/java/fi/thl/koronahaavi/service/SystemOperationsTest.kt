package fi.thl.koronahaavi.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SystemOperationsTest {
    private lateinit var systemOperations: SystemOperations

    @Before
    fun init() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        systemOperations = SystemOperations(context)

        context.filesDir.listFiles()?.forEach { it.delete() }
    }

    @Test
    fun listFiles() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        File(context.filesDir, "test1.tmp").createNewFile()
        File(context.filesDir, "test2.tmp").createNewFile()
        File(context.filesDir, "test3tmp").createNewFile()
        File(context.filesDir, "test4.TMP").createNewFile()
        File(context.filesDir, "test5.tmp.txt").createNewFile()

        val files = systemOperations.listFilesInPersistedStorage(".tmp")
        assertEquals(true, files?.all { it.name == "test1.tmp" || it.name == "test2.tmp" })
    }
}