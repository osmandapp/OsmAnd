package net.osmand.shared.gpx.helper


import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.osmand.shared.gpx.helper.ImportHelper.fileExists
import net.osmand.shared.gpx.helper.ImportHelper.printError
import net.osmand.shared.gpx.helper.ImportHelper.readFileContent
import net.osmand.shared.gpx.helper.ImportHelper.writeFileContent
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML

class KmlToGpxConverter {

	private val xml = XML {
		indentString = "  "
		xmlDeclMode = XmlDeclMode.Charset // Add <?xml version="1.0" encoding="UTF-8"?>
		autoPolymorphic = true
	}

	fun convert(kmlFilePath: String, gpxFilePath: String) {
		if (!fileExists(kmlFilePath)) {
			printError("KML input file not found: $kmlFilePath")
			return
		}

		val kmlContent = readFileContent(kmlFilePath)
		val kmlRoot: KmlRoot = try {
			xml.decodeFromString(kmlContent)
		} catch (e: Exception) {
			printError("Error parsing KML file: ${e.message}")
			e.printStackTrace()
			return
		}

		val waypoints = mutableListOf<GpxWaypoint>()
		val tracks = mutableListOf<GpxTrack>()
		val kmlDocument = kmlRoot.document ?: return

		val metadata = GpxMetadata(
			name = kmlDocument.name,
			author = kmlDocument.author?.authorName?.let { GpxAuthor(it) }
		)

		val allPlacemarks = kmlDocument.getAllPlacemarks()
		for ((placemark, folderName) in allPlacemarks) {
			// 1. Process Points -> Waypoints
			placemark.point?.let { point ->
				parseKmlCoordinates(point.coordinates).firstOrNull()?.let { (lon, lat, ele) ->
					waypoints.add(
						GpxWaypoint(
							lat = lat,
							lon = lon,
							ele = ele,
							name = placemark.name,
							desc = placemark.description,
							type = folderName
						)
					)
				}
			}

			// 2. Process LineStrings -> Tracks
			val lineStringCoords = placemark.lineString?.coordinates ?: placemark.multiGeometry?.lineString?.coordinates
			lineStringCoords?.let { coords ->
				val trackPoints = parseKmlCoordinates(coords).map { (lon, lat, ele) ->
					GpxTrackPoint(lat = lat, lon = lon, ele = ele)
				}
				if (trackPoints.isNotEmpty()) {
					tracks.add(GpxTrack(
						name = placemark.name,
						desc = placemark.description,
						trkseg = GpxTrackSegment(trackPoints)
					))
				}
			}

			// 3. Process gx:Track -> Time-stamped Tracks
			placemark.track?.let { gxTrack ->
				val trackPoints = gxTrack.coords.mapIndexedNotNull { index, coordStr ->
					val (lon, lat, ele) = parseGxCoord(coordStr)
					val time = gxTrack.whenTimestamps.getOrNull(index)?.let { Instant.parse(it) }
					GpxTrackPoint(lat, lon, ele, time)
				}
				if (trackPoints.isNotEmpty()) {
					tracks.add(GpxTrack(
						name = placemark.name,
						desc = placemark.description,
						trkseg = GpxTrackSegment(trackPoints)
					))
				}
			}
		}

		val gpx = Gpx(metadata = metadata, waypoints = waypoints, tracks = tracks)
		val gpxContent = xml.encodeToString(gpx)
		writeFileContent(gpxFilePath, gpxContent)
		println("Conversion successful! GPX file saved to $gpxFilePath")
	}

	// Parses KML coordinate strings like "lon,lat,alt"
	private fun parseKmlCoordinates(coordinatesString: String): List<Triple<Double, Double, Double?>> {
		return coordinatesString.trim().split(Regex("\\s+")).mapNotNull { part ->
			val components = part.split(',')
			try {
				Triple(components[0].toDouble(), components[1].toDouble(), components.getOrNull(2)?.toDoubleOrNull())
			} catch (e: Exception) {
				printError("Could not parse KML coordinate part: $part")
				null
			}
		}
	}

	// Parses gx:coord strings like "lon lat alt"
	private fun parseGxCoord(coordString: String): Triple<Double, Double, Double?> {
		val components = coordString.trim().split(Regex("\\s+"))
		return Triple(components[0].toDouble(), components[1].toDouble(), components.getOrNull(2)?.toDoubleOrNull())
	}
}