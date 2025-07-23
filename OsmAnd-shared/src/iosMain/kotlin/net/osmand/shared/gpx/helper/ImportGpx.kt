package net.osmand.shared.gpx.helper

import net.osmand.shared.gpx.GpxFile
import okio.Source

actual object ImportGpx {
	actual fun loadGpx(source: Source, fileName: String): Pair<GpxFile, Long>? {
		throw NotImplementedError("KML to GPX conversion using XSLT not supported on iOS")
	}
}
