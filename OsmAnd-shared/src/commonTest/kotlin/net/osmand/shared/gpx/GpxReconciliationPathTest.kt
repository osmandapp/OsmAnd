package net.osmand.shared.gpx

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GpxReconciliationPathTest {

	@Test
	fun membershipUsesNormalizedPathSegments() {
		val root = "/storage/emulated/0/Android/data/net.osmand/files/tracks"

		assertTrue(GpxDbHelper.isPathInsideGpxRoot("$root/route.gpx", root))
		assertTrue(GpxDbHelper.isPathInsideGpxRoot("$root/day/../route.gpx", root))
		assertFalse(GpxDbHelper.isPathInsideGpxRoot("${root}_backup/route.gpx", root))
		assertFalse(GpxDbHelper.isPathInsideGpxRoot("$root/../outside.gpx", root))
	}
}
