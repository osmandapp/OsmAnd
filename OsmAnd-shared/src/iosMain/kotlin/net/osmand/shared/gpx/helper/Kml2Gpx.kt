package net.osmand.shared.gpx.helper

import okio.Source

actual object Kml2Gpx {
		actual fun toGpx(kml: Source): String? {
			throw NotImplementedError("KML to GPX conversion using XSLT not supported on iOS")
	}
}
