package net.osmand.shared.data

import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils

/**
 * A street of a [City] in the address section, with the buildings and crossing streets read for it.
 *
 * A copy of `net.osmand.data.Street` in OsmAnd-java, which stays there for android and tools. Left
 * out: the JSON export, which only writes test data from OsmAnd-java.
 */
class Street(private val city: City?) : MapObject() {

	private val buildings: MutableList<Building> = ArrayList()
	private var buildingsByIdCache: MutableMap<String, Building>? = null
	private var intersectedStreets: MutableList<Street>? = null

	fun addBuilding(building: Building) {
		buildings.add(building)
	}

	fun getIntersectedStreets(): List<Street> = intersectedStreets ?: emptyList()

	fun addIntersectedStreet(s: Street) {
		var streets = intersectedStreets
		if (streets == null) {
			streets = ArrayList()
			intersectedStreets = streets
		}
		streets.add(s)
	}

	override fun getBbox31(): IntArray? {
		val bb = getBboxPoints() ?: return null
		return intArrayOf(
			KMapUtils.get31TileNumberX(bb.left), KMapUtils.get31TileNumberY(bb.top),
			KMapUtils.get31TileNumberX(bb.right), KMapUtils.get31TileNumberY(bb.bottom)
		)
	}

	fun getBboxPoints(): KQuadRect? {
		val ll = getLocation() ?: return null
		val qr = getMinBbox(ll)
		if (buildings.isEmpty()) {
			// use intersected streets however it's much larger
			for (s in getIntersectedStreets()) {
				val l2 = s.getLocation()
				if (l2 != null) {
					qr.include(l2.longitude, l2.latitude)
				}
			}
		}
		for (b in buildings) {
			val l2 = b.getLocation()
			if (l2 != null) {
				qr.include(l2.longitude, l2.latitude)
			}
		}
		return qr
	}

	fun addBuildingCheckById(building: Building) {
		var cache = buildingsByIdCache
		if (cache == null) {
			cache = HashMap()
			for (b in buildings) {
				cache[b.getId().toString() + " " + b.getFullName()] = b
			}
			buildingsByIdCache = cache
		}
		val key = building.getId().toString() + " " + building.getFullName()
		if (cache.containsKey(key)) {
			return
		}
		cache[key] = building
		buildings.add(building)
	}

	fun getBuildings(): MutableList<Building> = buildings

	fun getCity(): City? = city

	fun sortBuildings() {
		buildings.sortWith { o1, o2 ->
			val s1 = o1.getName()
			val s2 = o2.getName()
			val i1 = KAlgorithms.extractFirstIntegerNumber(s1)
			val i2 = KAlgorithms.extractFirstIntegerNumber(s2)
			if (i1 == i2) {
				val t1 = KAlgorithms.extractIntegerSuffix(s1)
				val t2 = KAlgorithms.extractIntegerSuffix(s2)
				t1.compareTo(t2)
			} else {
				i1 - i2
			}
		}
	}

	/// GENERATION

	fun mergeWith(street: Street) {
		for (b in street.getBuildings()) {
			addBuildingCheckById(b)
		}
		copyNames(street)
	}

	fun getNameWithoutCityPart(lang: String?, transliterate: Boolean): String {
		val nm = getName(lang, transliterate)
		val t = nm.lastIndexOf('(')
		if (t > 0) {
			return nm.substring(0, t)
		}
		return nm
	}

	fun getNameCityPart(lang: String?, transliterate: Boolean): String {
		val nm = getName(lang, transliterate)
		var t = nm.lastIndexOf('(')
		if (t > 0) {
			var cityPart = nm.substring(t + 1)
			t = cityPart.lastIndexOf(')')
			if (t != -1) {
				cityPart = cityPart.substring(0, t)
			}
			return cityPart
		}
		return ""
	}
}
