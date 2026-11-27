package net.osmand.aidl

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.plus.OsmandApplication
import net.osmand.plus.auto.CarTestUtils
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImportGpxDestinationTest {

	@Test
	fun destinationOutsideTracksIsRejected() {
		val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
		CarTestUtils.waitForAppInitialization(app)
		val outside = app.getAppPath("test_import_gpx_traversal.gpx")
		try {
			assertFalse(app.aidlApi.importGpxFromData("<gpx/>", "../" + outside.name, null, false))
			assertFalse(outside.exists())
		} finally {
			outside.delete()
		}
	}
}
