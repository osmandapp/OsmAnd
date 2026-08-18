package net.osmand.shared.gpx

import net.osmand.shared.io.KFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DataItemHydrationTest {

	@Test
	fun databaseItemsSkipFilesystemParameterInitialization() {
		val file = KFile("/gpx/database-only-item.gpx")

		assertFalse(GpxDataItem.fromDatabase(file).hasData())
		assertFalse(GpxDirItem.fromDatabase(file.getParentFile()!!).hasData())
	}

	@Test
	fun databaseFilePathUsesStoredParentDirectory() {
		assertResolvedPath(
			fileName = "route.gpx",
			fileDir = "new/batch_01",
			directory = false,
			expected = "$GPX_ROOT/new/batch_01/route.gpx"
		)
	}

	@Test
	fun databaseDirectoryPathsSupportCurrentAndLegacyFormats() {
		assertResolvedPath("tracks", "", true, GPX_ROOT)
		assertResolvedPath("new", "", true, "$GPX_ROOT/new")
		assertResolvedPath("batch_01", "new/batch_01", true, "$GPX_ROOT/new/batch_01")
		assertResolvedPath("batch_01", "new", true, "$GPX_ROOT/new/batch_01")
	}

	@Test
	fun databaseDirectoryPathSupportsAbsoluteStoredPath() {
		assertResolvedPath(
			fileName = "batch_01",
			fileDir = "$GPX_ROOT/new/batch_01",
			directory = true,
			expected = "$GPX_ROOT/new/batch_01"
		)
		assertResolvedPath(
			fileName = "batch_01",
			fileDir = "$APP_ROOT/tracks/new/batch_01",
			directory = true,
			expected = "$GPX_ROOT/new/batch_01"
		)
	}

	@Test
	fun databaseItemKeepsUnknownAbsolutePathOutsideGpxRoot() {
		assertResolvedPath(
			fileName = "outside",
			fileDir = "/storage/emulated/0/outside",
			directory = true,
			expected = "/storage/emulated/0/outside"
		)
	}

	private fun assertResolvedPath(
		fileName: String,
		fileDir: String,
		directory: Boolean,
		expected: String
	) {
		val file = GpxDatabase.resolveItemFile(
			appDir = KFile(APP_ROOT),
			gpxDir = KFile(GPX_ROOT),
			fileName = fileName,
			storedFileDir = fileDir,
			directory = directory
		)
		assertEquals(expected, file.path())
	}

	private companion object {
		const val APP_ROOT = "/storage/emulated/0/Android/data/net.osmand/files"
		const val GPX_ROOT = "$APP_ROOT/tracks"
	}
}
