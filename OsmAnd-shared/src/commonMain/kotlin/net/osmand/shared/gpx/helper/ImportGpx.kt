package net.osmand.shared.gpx.helper

import net.osmand.shared.IndexConstants
import net.osmand.shared.KException
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.GpxUtilities.loadGpxFile
import net.osmand.shared.xml.XmlPullParser
import net.osmand.shared.xml.XmlSerializer
import okio.Buffer
import okio.IOException
import okio.Source


object ImportGpx {

	private val folderNameStack = ArrayDeque<String>()
	private val placemarks = mutableListOf<Placemark>()
	private var documentName: String? = null

	@Throws(IOException::class)
	fun loadGpxWithFileSize(source: Source, fileName: String): Pair<GpxFile, Long> {
		folderNameStack.clear()
		placemarks.clear()
		documentName = null
		val gpxInfo: Pair<GpxFile, Long> = when {
			fileName.endsWith(IndexConstants.KML_SUFFIX) -> loadGPXFileFromKml(source)
			fileName.endsWith(IndexConstants.KMZ_SUFFIX) -> ImportHelper.loadGPXFileFromArchive(source)
			fileName.endsWith(IndexConstants.ZIP_EXT) -> ImportHelper.loadGPXFileFromArchive(source)
			else -> errorImport("Unsupported file extension")
		}
		return gpxInfo
	}

	@Throws(IOException::class)
	// Used by the web server
	fun importFile(source: Source, fileName: String): GpxFile {
		val gpxInfo = loadGpxWithFileSize(source, fileName)
		return gpxInfo.first
	}

	@Throws(IOException::class)
	fun loadGPXFileFromKml(kml: Source): Pair<GpxFile, Long> {
		try {
			val res = convertKmlToGpx(kml)
			val byteStr = res.encodeToByteArray()
			val gpxStream: Source = Buffer().write(byteStr)
			val fileSize = byteStr.size.toLong()
			return Pair(loadGpxFile(gpxStream), fileSize)
		} catch (e: Exception) {
			return errorImport("Klm to Gpx transform error")
		}
	}

	fun errorImport(msg: String): Pair<GpxFile, Long> {
		val gpxFile = GpxFile(null)
		gpxFile.error = KException(msg)
		return Pair(gpxFile, 0)
	}

	private fun convertKmlToGpx(kml: Source): String {
		parseKml(kml)
		return buildGpx()
	}

	private const val FEATURE_PROCESS_NAMESPACES: String = "http://xmlpull.org/v1/doc/features.html#process-namespaces"

	private fun parseKml(inputStream: Source) {
		val parser: XmlPullParser = XmlPullParser().apply {
			setFeature(FEATURE_PROCESS_NAMESPACES, false)
			setInput(inputStream, null)
		}

		var currentPlacemarkName: String? = null
		var currentPlacemarkDesc: String? = null
		var currentCoordinates: String? = null
		var isGxTrack = false
		val gxCoords = mutableListOf<String>()
		val gxWhens = mutableListOf<String>()

		var eventType = parser.getEventType()
		while (eventType != XmlPullParser.END_DOCUMENT) {
			when (eventType) {
				XmlPullParser.START_TAG -> {
					when (parser.getName()) {
						"Folder" -> folderNameStack.addLast("")
						"Document" -> documentName = ""
						"Placemark" -> {
							currentPlacemarkName = null
							currentPlacemarkDesc = null
							currentCoordinates = null
						}

						"gx:Track" -> isGxTrack = true
						"name" -> {
							if (documentName == "") documentName = parser.nextText()
							else if (folderNameStack.isNotEmpty()) folderNameStack[folderNameStack.lastIndex] =
								parser.nextText()
							else currentPlacemarkName = parser.nextText()
						}

						"description" -> currentPlacemarkDesc = parser.nextText()
						"coordinates" -> currentCoordinates = parser.nextText()
						"gx:coord" -> if (isGxTrack) gxCoords.add(parser.nextText())
						"when" -> if (isGxTrack) gxWhens.add(parser.nextText())
					}
				}

				XmlPullParser.END_TAG -> {
					when (parser.getName()) {
						"Folder" -> if (folderNameStack.isNotEmpty()) folderNameStack.removeLast()
						"Placemark" -> {
							if (currentCoordinates != null) {
								// This placemark represents a point or a linestring
								processPlacemark(currentPlacemarkName, currentPlacemarkDesc, currentCoordinates)
							}
						}

						"gx:Track" -> {
							// A gx:Track contains its own set of points and times
							processGxTrack(currentPlacemarkName, gxCoords, gxWhens)
							isGxTrack = false
							gxCoords.clear()
							gxWhens.clear()
						}
					}
				}
			}
			eventType = parser.next()
		}
	}

	private fun processPlacemark(name: String?, desc: String?, coords: String) {
		val coordinatePoints = parseCoordinates(coords)
		if (coordinatePoints.isEmpty()) return

		val placemark = if (coordinatePoints.size == 1) {
			// It's a Waypoint (<wpt>)
			Placemark(name, desc, folderNameStack.lastOrNull() ?: documentName, point = coordinatePoints.first())
		} else {
			// It's a Track (<trk>)
			Placemark(name, desc, folderNameStack.lastOrNull(), track = coordinatePoints)
		}
		placemarks.add(placemark)
	}

	private fun processGxTrack(name: String?, coords: List<String>, times: List<String>) {
		val trackPoints = coords.mapIndexed { index, coordString ->
			val parts = coordString.trim().split(" ")
			if (parts.size >= 2) {
				PointData(
					lon = parts[0],
					lat = parts[1],
					ele = if (parts.size > 2) parts[2] else null,
					time = times.getOrNull(index)
				)
			} else {
				null
			}
		}.filterNotNull()

		if (trackPoints.isNotEmpty()) {
			placemarks.add(Placemark(name, null, folderNameStack.lastOrNull(), track = trackPoints))
		}
	}

	private fun parseCoordinates(coords: String): List<PointData> {
		val pointStrings = coords.trim().split(" ")
		return pointStrings.mapNotNull { pointString ->
			val parts = pointString.trim().split(",")
			if (parts.size >= 2) {
				PointData(lon = parts[0], lat = parts[1], ele = if (parts.size > 2) parts[2] else null)
			} else {
				null // Invalid coordinate tuple
			}
		}
	}

	private fun buildGpx(): String {
		val serializer = XmlSerializer()
		val writer = Buffer()

		serializer.setOutput(writer)
		serializer.startDocument("UTF-8", true)
		serializer.setPrefix("gpx", "http://www.topografix.com/GPX/1/1")
		serializer.startTag(null, "gpx").apply {
			attribute(null, "version", "1.1")
			attribute(null, "creator", "KML to GPX Converter")
		}

		serializer.startTag(null, "metadata")
		documentName?.let { serializer.startTag(null, "name").text(it).endTag(null, "name") }
		serializer.endTag(null, "metadata")


		placemarks.filter { it.point != null }.forEach { p ->
			p.point?.let { point ->
				serializer.startTag(null, "wpt").apply {
					attribute(null, "lat", point.lat)
					attribute(null, "lon", point.lon)
				}
				point.ele?.let { serializer.startTag(null, "ele").text(it).endTag(null, "ele") }
			}
			p.name?.let { serializer.startTag(null, "name").text(it).endTag(null, "name") }
			p.description?.let { serializer.startTag(null, "desc").text(it).endTag(null, "desc") }
			p.parentName?.let { serializer.startTag(null, "type").text(it).endTag(null, "type") }
			serializer.endTag(null, "wpt")
		}

		placemarks.filter { it.track != null }.forEach { p ->
			serializer.startTag(null, "trk")
			p.name?.let { serializer.startTag(null, "name").text(it).endTag(null, "name") }
			serializer.startTag(null, "trkseg")
			p.track?.let { track ->
				track.forEach { point ->
					serializer.startTag(null, "trkpt").apply {
						attribute(null, "lat", point.lat)
						attribute(null, "lon", point.lon)
					}
					point.ele?.let { serializer.startTag(null, "ele").text(it).endTag(null, "ele") }
					point.time?.let { serializer.startTag(null, "time").text(it).endTag(null, "time") }
					serializer.endTag(null, "trkpt")
				}
			}
			serializer.endTag(null, "trkseg")
			serializer.endTag(null, "trk")
		}

		serializer.endTag(null, "gpx")
		serializer.endDocument()
		val toString = writer.buffer.readUtf8()
		return toString
	}

	data class PointData(
		val lon: String,
		val lat: String,
		val ele: String? = null,
		val time: String? = null
	)

	data class Placemark(
		val name: String?,
		val description: String?,
		val parentName: String?,
		val point: PointData? = null,
		val track: List<PointData>? = null
	)
}