package net.osmand.shared.data

import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.osm.PoiType
import net.osmand.shared.util.KAlgorithms

internal class SocketTagGroup(private val poiTypes: MapPoiTypes) {

	class Item(val type: PoiType, val value: String?)

	private class Socket(val type: PoiType) {
		var count: String? = null
		val attributes = LinkedHashMap<String, String>()
	}

	private val sockets = LinkedHashMap<String, Socket>()

	// socket:type2 is the count of the socket, socket:type2:output is its attribute "output"
	fun add(type: PoiType?, value: String?): Boolean {
		if (type == null || value == null || !type.isText()) {
			return false
		}
		val parts = type.getRawOsmTag()?.split(':', limit = 3) ?: return false
		if (parts.size < 2 || parts[0] != SOCKET_KEY) {
			return false
		}
		val groupType = getSocketGroupType(parts[1]) ?: return false
		val socket = sockets.getOrPut(groupType.getKeyName()) { Socket(groupType) }
		val attribute = parts.getOrNull(2)
		if (attribute == null) {
			socket.count = value.trim()
		} else {
			socket.attributes[attribute] = value
		}
		return true
	}

	fun getItems(): List<Item> =
		sockets.values.filter { !ABSENT_COUNTS.contains(it.count?.lowercase()) }
			.sortedBy { it.type.getParentType()?.getPoiAdditionals()?.indexOf(it.type) }
			.map { Item(it.type, formatSocketValue(it)) }

	// the map creator adds osmand_socket_type2=yes to socket:type2 for the search filter of the socket category
	private fun getSocketGroupType(socket: String): PoiType? =
		poiTypes.getPoiTypeByTagValue("$OSMAND_PREFIX${SOCKET_KEY}_$socket", FILTER_VALUE)
			?.takeIf { !KAlgorithms.isEmpty(it.getPoiAdditionalCategory()) }

	// "4 × 22 kW · 400 V · 32 A": a bare number gets the unit the OSM wiki implies for it
	private fun formatSocketValue(socket: Socket): String? {
		val count = socket.count?.takeIf { it.toIntOrNull() != null }
		val output = socket.attributes[OUTPUT_ATTRIBUTE]?.let { withDefaultUnit(it, OUTPUT_UNIT) }
		val head = listOfNotNull(count, output).joinToString(COUNT_SEPARATOR).ifEmpty { null }
		val voltage = socket.attributes[VOLTAGE_ATTRIBUTE]?.let { withDefaultUnit(it, VOLTAGE_UNIT) }
		val current = socket.attributes[CURRENT_ATTRIBUTE]?.let { withDefaultUnit(it, CURRENT_UNIT) }
		return listOfNotNull(head, voltage, current).joinToString(ATTRIBUTE_SEPARATOR).ifEmpty { null }
	}

	// "22;11" -> "22 kW; 11 kW", "3x32" -> "3x32 A"
	private fun withDefaultUnit(value: String, unit: String): String =
		value.split(Amenity.SEPARATOR).joinToString(VALUES_SEPARATOR) {
			val number = it.trim()
			if (BARE_NUMBER.matches(number)) "$number $unit" else number
		}

	companion object {
		private const val OSMAND_PREFIX = "osmand_"
		private const val SOCKET_KEY = "socket"
		private const val FILTER_VALUE = "yes"
		private val ABSENT_COUNTS = setOf("no", "0")
		private const val OUTPUT_ATTRIBUTE = "output"
		private const val VOLTAGE_ATTRIBUTE = "voltage"
		private const val CURRENT_ATTRIBUTE = "current"
		private const val OUTPUT_UNIT = "kW"
		private const val VOLTAGE_UNIT = "V"
		private const val CURRENT_UNIT = "A"
		private const val COUNT_SEPARATOR = " × "
		private const val ATTRIBUTE_SEPARATOR = " · "
		private const val VALUES_SEPARATOR = "; "
		private val BARE_NUMBER = Regex("(\\d+\\s*[x×]\\s*)?\\d+([.,]\\d+)?")
	}
}
