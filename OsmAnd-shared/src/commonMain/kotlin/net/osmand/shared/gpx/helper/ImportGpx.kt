package net.osmand.shared.gpx.helper

import net.osmand.shared.gpx.GpxFile
import okio.Source

expect object ImportGpx {
	fun loadGpx(source: Source, fileName: String): Pair<GpxFile, Long>?
}
