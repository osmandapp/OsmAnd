package net.osmand.shared.gpx.helper

import net.osmand.shared.IndexConstants
import net.osmand.shared.KException
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.GpxUtilities.loadGpxFile
import net.osmand.shared.util.LoggerFactory
import okio.Buffer
import okio.IOException
import okio.Source

object ImportGpx {

	val LOG = LoggerFactory.getLogger(this::class.simpleName!!)

	@Throws(IOException::class)
	fun loadGpx(source: Source, fileName: String): Pair<GpxFile, Long> {
		val gpxInfo: Pair<GpxFile, Long> = when {
			fileName.endsWith(IndexConstants.KML_SUFFIX) -> loadGPXFileFromKml(source)
			fileName.endsWith(IndexConstants.KMZ_SUFFIX) -> ImportHelper.loadGPXFileFromArchive(source)
			fileName.endsWith(IndexConstants.ZIP_EXT) -> ImportHelper.loadGPXFileFromArchive(source)
			else -> errorImport("Unsupported file extension")
		}
		return gpxInfo
	}

	@Throws(IOException::class)
	fun loadGPXFileFromKml(source: Source): Pair<GpxFile, Long> {
		val string = convertKmlToGpxStream(source)
		if (string != null) {
			val byteStr = string.encodeToByteArray()
			val gpxStream: Source = Buffer().write(byteStr)
			val fileSize = byteStr.size.toLong()
			return Pair(loadGpxFile(gpxStream), fileSize)
		}
		return errorImport("Klm to Gpx transform error")
	}

	private fun convertKmlToGpxStream(source: Source): String? {
		return ImportHelper.kml2Gpx(source)
	}

	fun errorImport(msg: String): Pair<GpxFile, Long> {
		val gpxFile = GpxFile(null)
		gpxFile.error = KException(msg)
		return Pair(gpxFile, 0)
	}
}
