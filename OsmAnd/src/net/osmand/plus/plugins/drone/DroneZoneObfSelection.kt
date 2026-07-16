package net.osmand.plus.plugins.drone

import net.osmand.NativeLibrary.RenderedObject
import net.osmand.binary.BinaryMapDataObject
import net.osmand.binary.BinaryMapIndexReader
import net.osmand.data.LatLon
import net.osmand.plus.OsmandApplication
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.views.layers.MapSelectionResult
import net.osmand.util.MapUtils
import java.util.LinkedHashMap

/** Renderer-independent hit testing for UAS polygons stored in an OBF. */
object DroneZoneObfSelection {
	private const val MIN_ZOOM = 9

	@JvmStatic
	fun collect(app: OsmandApplication, result: MapSelectionResult, zoom: Int) {
		if (zoom < MIN_ZOOM || PluginsHelper.getEnabledPlugin(DroneZonesPlugin::class.java) == null) return
		if (!app.settings.getCustomRenderBooleanProperty(DroneZonesPlugin.RENDER_PROPERTY).get()) return

		val point = result.pointLatLon
		val x31 = MapUtils.get31TileNumberX(point.longitude)
		val y31 = MapUtils.get31TileNumberY(point.latitude)
		val filter = BinaryMapIndexReader.SearchFilter { types, mapIndex ->
			var droneZone = false
			for (i in 0 until types.size()) {
				val type = mapIndex.decodeType(types[i])
				if (type?.tag == DroneZoneTags.ZONE && type.value == "yes") {
					droneZone = true
					break
				}
			}
			droneZone
		}

		val existing = result.allObjects.mapNotNull { selected ->
			(selected.`object`() as? RenderedObject)?.takeIf { DroneZoneTags.isDroneZone(it.tags) }
				?.let { zoneKey(it.id, it.tags) }
		}.toMutableSet()

		for (mapObject in app.resourceManager.renderer.searchMapObjectsAt(x31, y31, zoom, filter)) {
			if (!contains(mapObject, x31, y31)) continue
			val renderedObject = toRenderedObject(mapObject, point)
			val key = zoneKey(renderedObject.id, renderedObject.tags)
			if (existing.add(key)) {
				result.collect(renderedObject, null)
				result.objectLatLon = point
			}
		}
	}

	private fun zoneKey(id: Long?, tags: Map<String, String>): String =
		"${tags[DroneZoneTags.SOURCE_ID].orEmpty()}|${id ?: 0L}"

	private fun contains(mapObject: BinaryMapDataObject, x31: Int, y31: Int): Boolean {
		if (!mapObject.isArea || !containsRing(mapObject.coordinates, x31, y31)) return false
		return mapObject.polygonInnerCoordinates?.none { containsRing(it, x31, y31) } ?: true
	}

	private fun containsRing(coordinates: IntArray?, x31: Int, y31: Int): Boolean {
		if (coordinates == null || coordinates.size < 6) return false
		var inside = false
		var j = coordinates.size - 2
		var i = 0
		while (i < coordinates.size) {
			val xi = coordinates[i]
			val yi = coordinates[i + 1]
			val xj = coordinates[j]
			val yj = coordinates[j + 1]
			if ((yi > y31) != (yj > y31)) {
				val intersectionX = (xj - xi).toDouble() * (y31 - yi) / (yj - yi) + xi
				if (x31 < intersectionX) inside = !inside
			}
			j = i
			i += 2
		}
		return inside
	}

	private fun toRenderedObject(mapObject: BinaryMapDataObject, selectedPoint: LatLon): RenderedObject {
		val tags = decodeTags(mapObject)
		return RenderedObject().apply {
			setNativeId(mapObject.id)
			for (i in 0 until mapObject.pointsLength) {
				addLocation(mapObject.getPoint31XTile(i), mapObject.getPoint31YTile(i))
			}
			for ((tag, value) in tags) {
				putTag(tag, value)
				when {
					tag == "name" -> setName(value)
					tag.startsWith("name:") -> setName(tag.substringAfter(':'), value)
				}
			}
			setLabelX(mapObject.labelX)
			setLabelY(mapObject.labelY)
			labelLatLon = selectedPoint
			markAsPolygon(true)
			setVisible(true)
			order = 7
		}
	}

	private fun decodeTags(mapObject: BinaryMapDataObject): Map<String, String> {
		val tags = LinkedHashMap<String, String>()
		fun decode(types: IntArray?) {
			if (types == null) return
			for (type in types) {
				mapObject.mapIndex.decodeType(type)?.let { tags[it.tag] = it.value }
			}
		}
		decode(mapObject.types)
		decode(mapObject.additionalTypes)
		mapObject.orderedObjectNames?.forEach { (type, value) ->
			mapObject.mapIndex.decodeType(type)?.let { tags[it.tag] = value }
		}
		return tags
	}
}
