package net.osmand.plus.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileUtilsWriteErrorTest {

	@Test
	fun nestedDirectoryReportsReasonOfFirstMissingDirectory() {
		// the root of the file system is read-only or not writable for an app
		val dir = File("/osmand_write_error_test")
		val reason = FileUtils.getWriteError(dir, false)
		assertNotNull(reason)
		// not "ENOENT" of the last missing directory
		assertEquals(reason, FileUtils.getWriteError(File(dir, "nested/maps"), false))
	}

	@Test
	fun writableDirectoryHasNoErrorAndNoTestFileLeft() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		val root = File(context.cacheDir, "osmand_write_error_test")
		val dir = File(root, "nested/maps")
		try {
			assertNull(FileUtils.getWriteError(dir, true))
			assertArrayEquals(emptyArray<String>(), dir.list())
		} finally {
			root.deleteRecursively()
		}
	}
}
