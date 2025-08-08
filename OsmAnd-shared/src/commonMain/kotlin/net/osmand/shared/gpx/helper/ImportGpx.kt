package net.osmand.shared.gpx.helper

import net.osmand.shared.IndexConstants
import net.osmand.shared.KException
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.GpxUtilities.loadGpxFile
import net.osmand.shared.xml.XmlPullParser
import net.osmand.shared.xml.XmlSerializer
import okio.*

object ImportGpx {

	private const val FEATURE_PROCESS_NAMESPACES =
		"http://xmlpull.org/v1/doc/features.html#process-namespaces"

	private const val TAG_FOLDER = "Folder"
	private const val TAG_DOCUMENT = "Document"
	private const val TAG_PLACEMARK = "Placemark"
	private const val TAG_GX_TRACK = "gx:Track"
	private const val TAG_NAME = "name"
	private const val TAG_DESC = "description"
	private const val TAG_COORDS = "coordinates"
	private const val TAG_GX_COORD = "gx:coord"
	private const val TAG_WHEN = "when"

	@Throws(IOException::class)
	fun loadGpxWithFileSize(source: Source, fileName: String): Pair<GpxFile, Long> {
		return when {
			fileName.endsWith(IndexConstants.KML_SUFFIX) -> loadGPXFileFromKml(source)
			fileName.endsWith(IndexConstants.KMZ_SUFFIX) -> ImportHelper.loadGPXFileFromArchive(source)
			fileName.endsWith(IndexConstants.ZIP_EXT) -> ImportHelper.loadGPXFileFromArchive(source)
			else -> errorImport("Unsupported file extension: $fileName")
		}
	}

	@Throws(IOException::class)
	// Used by the web server
	fun importFile(source: Source, fileName: String): GpxFile {
		return loadGpxWithFileSize(source, fileName).first
	}

	@Throws(IOException::class)
	fun loadGPXFileFromKml(kml: Source): Pair<GpxFile, Long> {
		return try {
			val res = convertKmlToGpxString(kml)
			val byteStr = res.encodeToByteArray()
			val gpxStream: Source = Buffer().write(byteStr)
			Pair(loadGpxFile(gpxStream), byteStr.size.toLong())
		} catch (e: Exception) {
			errorImport("KML to GPX transform error: ${e.message}")
		}
	}

	fun errorImport(msg: String): Pair<GpxFile, Long> {
		val gpxFile = GpxFile(null).apply {
			error = KException(msg)
		}
		return Pair(gpxFile, 0)
	}

	private fun convertKmlToGpxString(kml: Source): String {
		val writer = Buffer()
		val serializer = startGpxDocument(writer)
		val documentName = parseKmlStreaming(kml, serializer, writer, flushEvery = Int.MAX_VALUE)
		writeMetadata(serializer, documentName)
		endGpxDocument(serializer)
		return writer.readUtf8()
	}

	fun convertKmlToGpxStream(
		kml: Source,
		outputSink: BufferedSink,
		flushEvery: Int = 100
	): String? {
		val serializer = startGpxDocument(outputSink)
		val documentName = parseKmlStreaming(kml, serializer, outputSink, flushEvery)
		writeMetadata(serializer, documentName)
		endGpxDocument(serializer)
		outputSink.flush()
		return documentName
	}

	private fun parseKmlStreaming(
		inputStream: Source,
		serializer: XmlSerializer,
		sink: Sink,
		flushEvery: Int
	): String? {
		val parser = XmlPullParser().apply {
			setFeature(FEATURE_PROCESS_NAMESPACES, false)
			setInput(inputStream, null)
		}

		val folderNameStack = ArrayDeque<String>()
		var documentName: String? = null
		var currentPlacemarkName: String? = null
		var currentPlacemarkDesc: String? = null
		var currentCoordinates: String? = null
		var isGxTrack = false
		val gxCoords = mutableListOf<String>()
		val gxWhens = mutableListOf<String>()
		var elementCount = 0

		while (true) {
			when (parser.getEventType()) {
				XmlPullParser.START_TAG -> when (parser.getName()) {
					TAG_FOLDER -> folderNameStack.addLast("")
					TAG_DOCUMENT -> documentName = ""
					TAG_PLACEMARK -> {
						currentPlacemarkName = null
						currentPlacemarkDesc = null
						currentCoordinates = null
					}
					TAG_GX_TRACK -> isGxTrack = true
					TAG_NAME -> {
						val text = parser.nextText()
						when {
							documentName == "" -> documentName = text
							folderNameStack.isNotEmpty() -> folderNameStack[folderNameStack.lastIndex] = text
							else -> currentPlacemarkName = text
						}
					}
					TAG_DESC -> currentPlacemarkDesc = parser.nextText()
					TAG_COORDS -> currentCoordinates = parser.nextText()
					TAG_GX_COORD -> if (isGxTrack) gxCoords.add(parser.nextText())
					TAG_WHEN -> if (isGxTrack) gxWhens.add(parser.nextText())
				}

				XmlPullParser.END_TAG -> when (parser.getName()) {
					TAG_FOLDER -> folderNameStack.removeLastOrNull()
					TAG_PLACEMARK -> {
						if (currentCoordinates != null) {
							writePlacemarkAsGpx(
								serializer,
								currentPlacemarkName,
								currentPlacemarkDesc,
								currentCoordinates,
								folderNameStack.lastOrNull() ?: documentName
							)
							elementCount++
						}
					}
					TAG_GX_TRACK -> {
						writeGxTrackAsGpx(
							serializer,
							currentPlacemarkName,
							gxCoords,
							gxWhens
						)
						isGxTrack = false
						gxCoords.clear()
						gxWhens.clear()
						elementCount++
					}
				}
				XmlPullParser.END_DOCUMENT -> return documentName
			}

			if (elementCount >= flushEvery) {
				sink.flush()
				elementCount = 0
			}

			parser.next()
		}
	}

	private fun startGpxDocument(output: Sink): XmlSerializer {
		val serializer = XmlSerializer()
		when (output) {
			is Buffer, is BufferedSink -> serializer.setOutput(output)
			else -> throw IllegalArgumentException("Unsupported output type: $output")
		}
		serializer.startDocument("UTF-8", true)
		serializer.setPrefix("gpx", "http://www.topografix.com/GPX/1/1")
		serializer.startTag(null, "gpx")
			.attribute(null, "version", "1.1")
			.attribute(null, "creator", "KML to GPX Converter")
		return serializer
	}

	private fun writeMetadata(serializer: XmlSerializer, documentName: String?) {
		documentName?.let {
			serializer.startTag(null, "metadata")
				.startTag(null, "name").text(it).endTag(null, "name")
				.endTag(null, "metadata")
		}
	}

	private fun endGpxDocument(serializer: XmlSerializer) {
		serializer.endTag(null, "gpx")
		serializer.endDocument()
	}

	private fun writePlacemarkAsGpx(
		serializer: XmlSerializer,
		name: String?,
		desc: String?,
		coords: String,
		parentName: String?
	) {
		val points = parseCoordinates(coords)
		if (points.isEmpty()) return

		if (points.size == 1) {
			val point = points.first()
			serializer.startTag(null, "wpt")
				.attribute(null, "lat", point.lat)
				.attribute(null, "lon", point.lon)
			point.ele?.let { serializer.startTag(null, "ele").text(it).endTag(null, "ele") }
			name?.let { serializer.startTag(null, "name").text(it).endTag(null, "name") }
			desc?.let { serializer.startTag(null, "desc").text(it).endTag(null, "desc") }
			parentName?.let { serializer.startTag(null, "type").text(it).endTag(null, "type") }
			serializer.endTag(null, "wpt")
		} else {
			serializer.startTag(null, "trk")
			name?.let { serializer.startTag(null, "name").text(it).endTag(null, "name") }
			serializer.startTag(null, "trkseg")
			points.forEach { pt ->
				serializer.startTag(null, "trkpt")
					.attribute(null, "lat", pt.lat)
					.attribute(null, "lon", pt.lon)
				pt.ele?.let { serializer.startTag(null, "ele").text(it).endTag(null, "ele") }
				serializer.endTag(null, "trkpt")
			}
			serializer.endTag(null, "trkseg")
			serializer.endTag(null, "trk")
		}
	}

	private fun writeGxTrackAsGpx(
		serializer: XmlSerializer,
		name: String?,
		coords: List<String>,
		times: List<String>
	) {
		val trackPoints = coords.mapIndexedNotNull { index, coordString ->
			val parts = coordString.trim().split(" ")
			if (parts.size >= 2) {
				PointData(
					lon = parts[0],
					lat = parts[1],
					ele = parts.getOrNull(2),
					time = times.getOrNull(index)
				)
			} else null
		}
		if (trackPoints.isEmpty()) return

		serializer.startTag(null, "trk")
		name?.let { serializer.startTag(null, "name").text(it).endTag(null, "name") }
		serializer.startTag(null, "trkseg")
		trackPoints.forEach { point ->
			serializer.startTag(null, "trkpt")
				.attribute(null, "lat", point.lat)
				.attribute(null, "lon", point.lon)
			point.ele?.let { serializer.startTag(null, "ele").text(it).endTag(null, "ele") }
			point.time?.let { serializer.startTag(null, "time").text(it).endTag(null, "time") }
			serializer.endTag(null, "trkpt")
		}
		serializer.endTag(null, "trkseg")
		serializer.endTag(null, "trk")
	}

	private fun parseCoordinates(coords: String): List<PointData> =
		coords.trim().split(" ").mapNotNull { pointString ->
			val parts = pointString.trim().split(",")
			if (parts.size >= 2) {
				PointData(lon = parts[0], lat = parts[1], ele = parts.getOrNull(2))
			} else null
		}

	data class PointData(
		val lon: String,
		val lat: String,
		val ele: String? = null,
		val time: String? = null
	)
}