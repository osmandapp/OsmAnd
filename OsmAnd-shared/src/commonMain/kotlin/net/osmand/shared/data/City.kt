package net.osmand.shared.data

import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import kotlin.math.max
import kotlin.math.min

/**
 * A settlement, postcode or boundary of the address section: what a street belongs to.
 *
 * A copy of `net.osmand.data.City` in OsmAnd-java, which stays there for android and tools. Its
 * [CityType] was copied earlier, out of the class, for the amenity model. Left out: the JSON
 * export, which only writes test data from OsmAnd-java, and the reading of an OSM entity, which is
 * the map generator's.
 */
class City : MapObject {

	private val type: CityType
	private val listOfStreets: MutableList<Street> = ArrayList()
	private var postcode: String? = null
	private var closestCity: City? = null
	private var bbox31: IntArray? = null

	// Be attentive ! Working with street names ignoring case
	private var isin: Set<String>? = null

	constructor(type: CityType) {
		this.type = type
	}

	constructor(postcode: String, id: Long) {
		this.type = CityType.POSTCODE
		this.name = postcode
		this.enName = postcode
		this.id = id
	}

	fun isInCityByName(name: String): Boolean {
		val isin = this.isin ?: return false
		return isin.contains(name.lowercase())
	}

	override fun getBbox31(): IntArray? = bbox31

	fun calculateBbox31FromStreets() {
		for (s in getStreets()) {
			// could be more precise with min max
			updateBbox31WithLoc(s.getBboxPoints()!!)
		}
	}

	fun updateBbox31WithLoc(location: KLatLon): Boolean = updateBbox31WithLoc(getMinBbox(location))

	fun updateBbox31WithLoc(quadRect: KQuadRect): Boolean {
		val lx = KMapUtils.get31TileNumberX(quadRect.left)
		val rx = KMapUtils.get31TileNumberX(quadRect.right)
		val ty = KMapUtils.get31TileNumberY(quadRect.top)
		val by = KMapUtils.get31TileNumberY(quadRect.bottom)
		val bbox31 = this.bbox31
		if (bbox31 != null) {
			if (by > bbox31[3] || ty < bbox31[1] || rx > bbox31[2] || lx < bbox31[0]) {
				bbox31[0] = min(lx, bbox31[0])
				bbox31[1] = min(ty, bbox31[1])
				bbox31[2] = max(rx, bbox31[2])
				bbox31[3] = max(by, bbox31[3])
				return true
			}
		} else {
			val location = getLocation()!!
			val cx = KMapUtils.get31TileNumberX(location.longitude)
			val cy = KMapUtils.get31TileNumberY(location.latitude)
			this.bbox31 = intArrayOf(min(lx, cx), min(ty, cy), max(rx, cx), max(by, cy))
			return true
		}
		return false
	}

	fun setBbox31(bbox: KQuadRect) {
		this.bbox31 = intArrayOf(
			KMapUtils.get31TileNumberX(bbox.left), KMapUtils.get31TileNumberY(bbox.top),
			KMapUtils.get31TileNumberX(bbox.right), KMapUtils.get31TileNumberY(bbox.bottom)
		)
	}

	fun setBbox31(bbox31: IntArray?) {
		this.bbox31 = bbox31
	}

	fun isPostcode(): Boolean = type == CityType.POSTCODE

	fun getPostcode(): String? = postcode

	fun setPostcode(postcode: String?) {
		this.postcode = postcode
	}

	fun getClosestCity(): City? = closestCity

	fun setClosestCity(closestCity: City?) {
		this.closestCity = closestCity
	}

	fun registerStreet(street: Street) {
		listOfStreets.add(street)
	}

	fun unregisterStreet(candidate: Street) {
		listOfStreets.remove(candidate)
	}

	fun getType(): CityType = type

	fun getStreets(): MutableList<Street> = listOfStreets

	override fun toString(): String {
		if (isPostcode()) {
			return "Postcode : " + getName() + " " + getLocation()
		}
		return "City [" + type + "] " + getName() + " " + getLocation()
	}

	fun getStreetByName(name: String): Street? {
		for (s in listOfStreets) {
			if (s.getName().equals(name, ignoreCase = true)) {
				return s
			}
		}
		return null
	}

	fun getIsin(): Set<String>? = isin

	/** Java keeps these in a `TreeSet`; here they are sorted once, into a set that keeps the order. */
	fun setIsin(value: String) {
		val names = ArrayList<String>()
		val vls = value.lowercase().split(",")
		for (v1 in vls) {
			val v2s = v1.trim().split(";")
			for (v in v2s) {
				val v2 = v.trim()
				if (!KAlgorithms.isEmpty(v2)) {
					names.add(v2)
				}
			}
		}
		this.isin = LinkedHashSet(names.distinct().sorted())
	}

	fun mergeWith(city: City): Map<Street, Street> {
		val m = LinkedHashMap<Street, Street>()
		for (street in city.listOfStreets) {
			if (listOfStreets.contains(street)) {
				listOfStreets[listOfStreets.indexOf(street)].mergeWith(street)
			} else {
				val s = Street(this)
				s.copyNames(street)
				val location = street.getLocation()!!
				s.setLocation(location.latitude, location.longitude)
				s.setId(street.getId())
				s.getBuildings().addAll(street.getBuildings())
				m[street] = s
				listOfStreets.add(s)
			}
		}
		copyNames(city)
		return m
	}

	companion object {

		private var POSTCODE_INTERNAL_ID = -1000L

		fun createPostcode(postcode: String): City = City(postcode, POSTCODE_INTERNAL_ID--)
	}
}
