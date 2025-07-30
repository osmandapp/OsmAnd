package net.osmand.shared.gpx.helper

import net.osmand.shared.gpx.GpxFile
import okio.Source

actual object ImportHelper {
	actual fun kml2Gpx(kml: Source): String? {
		throw NotImplementedError("KML to GPX conversion using XSLT not supported on iOS")
	}

	actual fun loadGPXFileFromArchive(source: Source): Pair<GpxFile, Long> {
		throw NotImplementedError("loadGPXFileFromZip not supported on iOS")
	}
}