package net.osmand.shared.gpx.helper

import net.osmand.shared.gpx.GpxFile
import okio.Source

expect object ImportHelper {
	fun loadGPXFileFromArchive(source: Source): Pair<GpxFile, Long>
}