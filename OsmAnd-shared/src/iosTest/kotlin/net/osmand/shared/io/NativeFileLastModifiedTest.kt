package net.osmand.shared.io

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.timeIntervalSince1970
import platform.posix.AT_FDCWD
import platform.posix.timespec
import platform.posix.utimensat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// GpxDbHelper.reconcileFilesystem() compares FILE_LAST_MODIFIED_TIME with != against the value
// stored in the database, and every stored value was written by the NSDate-based implementation
// that KFile.lastModified() replaced. A millisecond of drift re-reads and re-analyses the track,
// so the two must agree for every timestamp, including the nanosecond values that sit on a
// millisecond boundary, where the two roundings can part ways.
@OptIn(ExperimentalForeignApi::class)
class NativeFileLastModifiedTest {

	private fun setModificationTime(path: String, seconds: Long, nanoseconds: Long) = memScoped {
		val times = allocArray<timespec>(2)
		for (i in 0..1) {
			times[i].tv_sec = seconds
			times[i].tv_nsec = nanoseconds
		}
		assertEquals(0, utimensat(AT_FDCWD, path, times, 0), "utimensat failed for $path")
	}

	// The implementation that was replaced: (NSDate.timeIntervalSince1970 * 1000.0).toLong().
	private fun nsDateMillis(path: String): Long {
		val attributes = NSFileManager.defaultManager.attributesOfItemAtPath(path, null)
		val date = attributes?.get(NSFileModificationDate) as? NSDate
		assertTrue(date != null, "no modification date for $path")
		return (date.timeIntervalSince1970 * 1000.0).toLong()
	}

	@Test
	fun matchesTheReplacedNsDateFormula() {
		val file = KFile(NSTemporaryDirectory() + "native-file-last-modified-test")
		try {
			file.writeText("x")
			val path = file.absolutePath()
			for (seconds in listOf(1_600_000_000L, 1_700_000_000L, 1_767_225_600L)) {
				for (ms in 0 until 1000) {
					val boundary = ms * 1_000_000L
					for (nanoseconds in listOf(boundary, boundary + 1, maxOf(0L, boundary - 1), boundary + 500_000L)) {
						setModificationTime(path, seconds, nanoseconds)
						assertEquals(
							nsDateMillis(path),
							file.lastModified(),
							"seconds=$seconds nanoseconds=$nanoseconds"
						)
					}
				}
			}
		} finally {
			assertTrue(file.delete())
		}
	}

	@Test
	fun matchesTheReplacedNsDateFormulaForDatesAssignedByTheApp() {
		val file = KFile(NSTemporaryDirectory() + "native-file-last-modified-date-test")
		try {
			file.writeText("x")
			val path = file.absolutePath()
			// Backup restore assigns NSFileModificationDate from a timestamp, which lands the
			// nanosecond part exactly on a millisecond boundary - the worst case for rounding.
			for (millis in 1_500_000_000_000L until 1_500_000_010_000L step 137L) {
				val date = NSDate(timeIntervalSinceReferenceDate = millis / 1000.0 - 978_307_200.0)
				NSFileManager.defaultManager.setAttributes(
					mapOf<Any?, Any>(NSFileModificationDate to date), path, null
				)
				assertEquals(nsDateMillis(path), file.lastModified(), "millis=$millis")
			}
		} finally {
			assertTrue(file.delete())
		}
	}
}
