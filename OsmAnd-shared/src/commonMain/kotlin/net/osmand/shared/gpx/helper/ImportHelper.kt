package net.osmand.shared.gpx.helper

import net.osmand.shared.gpx.GpxFile
import okio.Source

expect object ImportHelper {
	fun kml2Gpx(kml: Source): String?
	fun loadGPXFileFromArchive(source: Source): Pair<GpxFile, Long>
	fun readFileContent(filePath: String): String
	fun writeFileContent(filePath: String, content: String)
	fun fileExists(filePath: String): Boolean
	fun printError(message: String)
}