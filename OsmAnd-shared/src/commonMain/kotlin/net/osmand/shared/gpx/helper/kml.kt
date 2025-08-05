package net.osmand.shared.gpx.helper

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.*

internal const val KML_NAMESPACE = "http://www.opengis.net/kml/2.2"
internal const val GX_NAMESPACE = "http://www.google.com/kml/ext/2.2"
internal const val ATOM_NAMESPACE = "http://www.w3.org/2005/Atom"

@Serializable
@XmlSerialName("kml", KML_NAMESPACE, "")
data class KmlRoot(
	@XmlElement(true) val document: KmlDocument? = null
)

@Serializable
@XmlSerialName("Document", KML_NAMESPACE, "kml")
data class KmlDocument(
	@XmlElement(true) @XmlSerialName("name", KML_NAMESPACE, "kml") val name: String? = null,
	@XmlElement(true) @XmlSerialName("author", ATOM_NAMESPACE, "atom") val author: AtomAuthorWrapper? = null,
	@XmlElement(true) @XmlChildrenName("Placemark", KML_NAMESPACE, "kml") val placemarks: List<KmlPlacemark> = emptyList(),
	@XmlElement(true) @XmlChildrenName("Folder", KML_NAMESPACE, "kml") val folders: List<KmlFolder> = emptyList()
)

@Serializable
@XmlSerialName("author", ATOM_NAMESPACE, "atom")
data class AtomAuthorWrapper(
	@XmlElement(true) @XmlSerialName("author", ATOM_NAMESPACE, "atom") val authorName: String? = null
)

@Serializable
@XmlSerialName("Folder", KML_NAMESPACE, "kml")
data class KmlFolder(
	@XmlElement(true) @XmlSerialName("name", KML_NAMESPACE, "kml") val name: String? = null,
	@XmlElement(true) @XmlChildrenName("Placemark", KML_NAMESPACE, "kml") val placemarks: List<KmlPlacemark> = emptyList()
)

@Serializable
@XmlSerialName("Placemark", KML_NAMESPACE, "kml")
data class KmlPlacemark(
	@XmlElement(true) @XmlSerialName("name", KML_NAMESPACE, "kml") val name: String? = null,
	@XmlElement(true) @XmlSerialName("description", KML_NAMESPACE, "kml") val description: String? = null,
	@XmlElement(true) @XmlSerialName("Point", KML_NAMESPACE, "kml") val point: KmlPoint? = null,
	@XmlElement(true) @XmlSerialName("LineString", KML_NAMESPACE, "kml") val lineString: KmlLineString? = null,
	@XmlElement(true) @XmlSerialName("MultiGeometry", KML_NAMESPACE, "kml") val multiGeometry: KmlMultiGeometry? = null,
	@XmlElement(true) @XmlSerialName("Track", GX_NAMESPACE, "gx") val track: GxTrack? = null
)

@Serializable
@XmlSerialName("Point", KML_NAMESPACE, "kml")
data class KmlPoint(@XmlElement(true) @XmlSerialName("coordinates", KML_NAMESPACE, "kml") @XmlValue val coordinates: String)

@Serializable
@XmlSerialName("LineString", KML_NAMESPACE, "kml")
data class KmlLineString(@XmlElement(true) @XmlSerialName("coordinates", KML_NAMESPACE, "kml") @XmlValue val coordinates: String)

@Serializable
@XmlSerialName("MultiGeometry", KML_NAMESPACE, "kml")
data class KmlMultiGeometry(@XmlElement(true) @XmlSerialName("LineString", KML_NAMESPACE, "kml") val lineString: KmlLineString?)

@Serializable
@XmlSerialName("Track", GX_NAMESPACE, "gx")
data class GxTrack(
	@XmlElement(true) @XmlSerialName("when", KML_NAMESPACE, "kml") val whenTimestamps: List<String> = emptyList(),
	@XmlElement(true) @XmlSerialName("coord", GX_NAMESPACE, "gx") val coords: List<String> = emptyList()
)

// Helper to get a flat list of all Placemarks with their parent folder's name
fun KmlDocument.getAllPlacemarks(): List<Pair<KmlPlacemark, String?>> {
	return placemarks.map { it to name } + folders.flatMap { folder ->
		folder.placemarks.map { placemark -> placemark to folder.name }
	}
}