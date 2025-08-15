package net.osmand.shared.gpx.helper

import net.osmand.shared.gpx.GpxFile
import okio.Source

actual object ImportHelper {
	actual fun loadGPXFileFromArchive(source: Source): Pair<GpxFile, Long> {
		throw NotImplementedError("loadGPXFileFromZip not supported on iOS")
	}
}