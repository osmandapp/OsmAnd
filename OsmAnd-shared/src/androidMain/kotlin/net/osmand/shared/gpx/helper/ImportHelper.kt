package net.osmand.shared.gpx.helper

import net.osmand.shared.IndexConstants
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.GpxUtilities.loadGpxFile
import net.osmand.shared.gpx.helper.ImportGpx.errorImport
import net.osmand.shared.gpx.helper.ImportGpx.loadGPXFileFromKml
import net.osmand.shared.io.SourceInputStream
import okio.Source
import okio.source
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

actual object ImportHelper {

	@Throws(IOException::class)
	actual fun loadGPXFileFromArchive(source: Source): Pair<GpxFile, Long> {
		val stream = ZipInputStream(SourceInputStream(source))
		var entry: ZipEntry? = stream.nextEntry
		while (entry != null) {
			val name = entry.name.lowercase()
			if (name.endsWith(IndexConstants.GPX_FILE_EXT)) {
				return Pair(loadGpxFile(stream.source()), entry.size)
			}
			if (name.endsWith(IndexConstants.KML_SUFFIX)) {
				return loadGPXFileFromKml(stream.source())
			}
			entry = stream.nextEntry
		}
		return errorImport("Archive doesn't have GPX/KLM files")
	}
}