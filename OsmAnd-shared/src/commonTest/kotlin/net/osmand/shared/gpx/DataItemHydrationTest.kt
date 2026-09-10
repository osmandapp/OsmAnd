package net.osmand.shared.gpx

import net.osmand.shared.io.KFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

	@Test
	fun analysisMergePreservesAppearanceAndBackupTimestamp() {
		val file = KFile("/gpx/customized.gpx")
		val storedItem = GpxDataItem.fromDatabase(file).apply {
			setParameter(GpxParameter.COLOR, 0xFF336699.toInt())
			setParameter(GpxParameter.WIDTH, "bold")
			setParameter(GpxParameter.SHOW_ARROWS, true)
			setParameter(GpxParameter.JOIN_SEGMENTS, true)
			setParameter(GpxParameter.ADDITIONAL_EXAGGERATION, 2.5)
			setParameter(GpxParameter.APPEARANCE_LAST_MODIFIED_TIME, 1234L)
		}
		val analyzedItem = GpxDataItem.fromDatabase(file).apply {
			setAnalysis(GpxTrackAnalysis())
			setParameter(GpxParameter.COLOR, 0xFF000000.toInt())
			setParameter(GpxParameter.FILE_LAST_MODIFIED_TIME, 5678L)
			setParameter(GpxParameter.FILE_CREATION_TIME, 1000L)
			setParameter(GpxParameter.NEAREST_CITY_NAME, "Amsterdam")
			setParameter(GpxParameter.ACTIVITY_TYPE, "cycling")
			setParameter(GpxParameter.DATA_VERSION, 42)
		}

		storedItem.copyAnalysisData(analyzedItem)

		assertEquals(0xFF336699.toInt(), storedItem.getParameter(GpxParameter.COLOR))
		assertEquals("bold", storedItem.getParameter(GpxParameter.WIDTH))
		assertEquals(true, storedItem.getParameter(GpxParameter.SHOW_ARROWS))
		assertEquals(true, storedItem.getParameter(GpxParameter.JOIN_SEGMENTS))
		assertEquals(2.5, storedItem.getParameter(GpxParameter.ADDITIONAL_EXAGGERATION))
		assertEquals(1234L, storedItem.getParameter(GpxParameter.APPEARANCE_LAST_MODIFIED_TIME))
		assertEquals(5678L, storedItem.getParameter(GpxParameter.FILE_LAST_MODIFIED_TIME))
		assertEquals("Amsterdam", storedItem.getParameter(GpxParameter.NEAREST_CITY_NAME))

		val updatedParameters = storedItem.getAnalysisUpdateParameters().keys
		assertTrue(updatedParameters.any { it.analysisParameter })
		assertTrue(GpxParameter.getAppearanceParameters().none { it in updatedParameters })
		assertFalse(GpxParameter.JOIN_SEGMENTS in updatedParameters)
		assertFalse(GpxParameter.ADDITIONAL_EXAGGERATION in updatedParameters)
		assertFalse(GpxParameter.APPEARANCE_LAST_MODIFIED_TIME in updatedParameters)
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
