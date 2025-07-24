package net.osmand.shared.gpx.helper

import net.osmand.shared.gpx.GpxFile
import okio.Source

expect object ImportHelper {
	fun kml2Gpx(kml: Source): String?
	fun loadGPXFileFromZip(source: Source): Pair<GpxFile, Long>?
}