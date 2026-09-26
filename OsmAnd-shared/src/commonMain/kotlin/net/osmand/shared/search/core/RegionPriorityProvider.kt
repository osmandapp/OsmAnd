package net.osmand.shared.search.core

import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.data.KLatLon
import net.osmand.shared.util.KMapUtils
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Orders the files of a search by how far their pois are from where it runs, in steps of 50 km.
 *
 * A copy of `RegionPriorityProvider` in OsmAnd-java, which stays there for android and tools; this
 * copy is for iOS. Java keeps the files by step in a `TreeMap`; common Kotlin has none, so the
 * steps are walked in order by hand.
 */
class RegionPriorityProvider(phrase: SearchPhrase?) {

	private val BBOX_STEP = 50000 // 50 km
	private val BBOX_MAX = BBOX_STEP * 20 // 1000 km
	private val priorityMap = HashMap<Int, MutableList<BinaryMapIndexReader>>()
	private var regionsPriority: LinkedHashMap<BinaryMapIndexReader, Int>? = null
	private var searchLocation: KLatLon? = null
	private var lastIndexesCount = -1

	init {
		if (phrase != null && phrase.getSettings() != null) {
			this.searchLocation = phrase.getSettings()!!.getOriginalLocation()
			initPriorityMap(phrase)
		}
	}

	fun checkAndUpdate(phrase: SearchPhrase?) {
		if (phrase == null || phrase.getSettings() == null) {
			return
		}

		val newLocation = phrase.getSettings()!!.getOriginalLocation()
		val cnt = phrase.getOfflineIndexes().size
		if (shouldReinitialize(newLocation, cnt)) {
			this.searchLocation = newLocation ?: this.searchLocation
			this.lastIndexesCount = cnt
			this.priorityMap.clear()
			this.regionsPriority = null
			initPriorityMap(phrase)
		}
	}

	private fun shouldReinitialize(newLocation: KLatLon?, cnt: Int): Boolean {
		val searchLocation = searchLocation
		if (searchLocation == null || this.lastIndexesCount != cnt) {
			return true
		}

		if (newLocation != null) {
			val distance = KMapUtils.getDistance(searchLocation, newLocation)
			return distance >= LOCATION_SHIFT_THRESHOLD_METERS
		}
		return false
	}

	fun getOfflineIndexes(): Collection<BinaryMapIndexReader> {
		return initRegionsPriority().keys
	}

	fun getOfflineIndexes(minRadius: Int, maxRadius: Int): List<BinaryMapIndexReader> {
		val result: MutableList<BinaryMapIndexReader> = ArrayList()

		val minPriority = floor(minRadius.toDouble() / BBOX_STEP).toInt()
		val maxPriority = ceil(maxRadius.toDouble() / BBOX_STEP).toInt()

		for (entry in priorityEntries()) {
			val p = entry.key
			if (p >= minPriority && p <= maxPriority) {
				for (r in entry.value) {
					if (!result.contains(r)) {
						result.add(r)
					}
				}
			}
		}
		return result
	}

	fun getRegionWeight(reader: BinaryMapIndexReader?): Int {
		if (reader == null || priorityMap.isEmpty()) {
			return 0
		}
		val priority = initRegionsPriority()[reader]
		if (priority == null) {
			return 0
		}
		return priority
	}

	private fun initRegionsPriority(): LinkedHashMap<BinaryMapIndexReader, Int> {
		val existing = regionsPriority
		if (existing != null) {
			return existing
		}
		val regionsPriority = LinkedHashMap<BinaryMapIndexReader, Int>()
		this.regionsPriority = regionsPriority
		for (entry in priorityEntries()) {
			val prority = entry.key
			for (reader in entry.value) {
				if (!regionsPriority.containsKey(reader)) {
					regionsPriority[reader] = prority
				}
			}
		}
		return regionsPriority
	}

	private fun priorityEntries(): List<Map.Entry<Int, List<BinaryMapIndexReader>>> =
		priorityMap.entries.sortedBy { it.key }

	private fun initPriorityMap(phrase: SearchPhrase?) {
		if (searchLocation == null) {
			return
		}

		if (phrase != null) {
			for (r in phrase.getOfflineIndexes()) {
				val priority = calculatePriorityValue(r)
				priorityMap.getOrPut(priority) { ArrayList() }.add(r)
			}
		}
	}

	private fun calculatePriorityValue(region: BinaryMapIndexReader): Int {
		var i = 0
		while (i * BBOX_STEP <= BBOX_MAX) {
			val rect = KMapUtils.calculate31BboxUsingRhumb(i * BBOX_STEP + 50, searchLocation!!)
			if (region.containsPoiData(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())) {
				return i
			}
			i++
		}
		return BBOX_MAX / BBOX_STEP + 1
	}

	companion object {
		private const val LOCATION_SHIFT_THRESHOLD_METERS = 30000.0 // 30 km
	}
}
