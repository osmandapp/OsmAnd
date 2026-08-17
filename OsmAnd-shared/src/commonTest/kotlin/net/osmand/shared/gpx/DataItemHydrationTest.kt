package net.osmand.shared.gpx

import net.osmand.shared.io.KFile
import kotlin.test.Test
import kotlin.test.assertFalse

class DataItemHydrationTest {

	@Test
	fun databaseItemsSkipFilesystemParameterInitialization() {
		val file = KFile("/gpx/database-only-item.gpx")

		assertFalse(GpxDataItem.fromDatabase(file).hasData())
		assertFalse(GpxDirItem.fromDatabase(file.getParentFile()!!).hasData())
	}
}
