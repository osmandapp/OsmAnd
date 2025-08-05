package net.osmand.shared.gpx.helper

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import nl.adaptivity.xmlutil.serialization.*

internal const val GPX_NAMESPACE = "http://www.topografix.com/GPX/1/1"

@Serializable
@XmlSerialName("gpx", GPX_NAMESPACE, "")
data class Gpx(
	@XmlElement val version: String = "1.1",
	@XmlElement val creator: String = "kml2gpx.xslt",
	@XmlElement(true) val metadata: GpxMetadata? = null,
	@XmlElement(true) @XmlChildrenName("wpt", GPX_NAMESPACE) val waypoints: List<GpxWaypoint> = emptyList(),
	@XmlElement(true) @XmlChildrenName("trk", GPX_NAMESPACE) val tracks: List<GpxTrack> = emptyList()
)

@Serializable
@XmlSerialName("metadata", GPX_NAMESPACE, "")
data class GpxMetadata(
	@XmlElement(true) val name: String? = null,
	@XmlElement(true) val author: GpxAuthor? = null
)

@Serializable
@XmlSerialName("author", GPX_NAMESPACE, "")
data class GpxAuthor(@XmlElement(true) val name: String? = null)

@Serializable
@XmlSerialName("wpt", GPX_NAMESPACE, "")
data class GpxWaypoint(
	@XmlElement val lat: Double,
	@XmlElement val lon: Double,
	@XmlElement(true) val ele: Double? = null,
	@XmlElement(true) val name: String? = null,
	@XmlElement(true) val desc: String? = null,
	@XmlElement(true) val type: String? = null,
	@XmlElement(true) val time: Instant? = null
)

@Serializable
@XmlSerialName("trk", GPX_NAMESPACE, "")
data class GpxTrack(
	@XmlElement(true) val name: String? = null,
	@XmlElement(true) val desc: String? = null,
	@XmlElement(true) val trkseg: GpxTrackSegment? = null
)

@Serializable
@XmlSerialName("trkseg", GPX_NAMESPACE, "")
data class GpxTrackSegment(@XmlElement(true) @XmlChildrenName("trkpt", GPX_NAMESPACE) val trackPoints: List<GpxTrackPoint> = emptyList())

@Serializable
@XmlSerialName("trkpt", GPX_NAMESPACE, "")
data class GpxTrackPoint(
	@XmlElement val lat: Double,
	@XmlElement val lon: Double,
	@XmlElement(true) val ele: Double? = null,
	@XmlElement(true) val time: Instant? = null
)