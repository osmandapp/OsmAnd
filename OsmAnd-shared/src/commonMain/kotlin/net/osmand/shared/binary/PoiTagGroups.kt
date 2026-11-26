package net.osmand.shared.binary

import net.osmand.shared.util.collections.KTLongHashSet

/**
 * The tag groups of the administrative areas the amenities of a poi section sit in, as one reader
 * has read them, and the tiles it has read them for. Java keeps them in its `PoiRegion`, which the
 * readers of a file share; here each reader keeps its own ([BinaryMapIndexReader.getTagGroups]), so
 * that an amenity looks its groups up without a lock, at the cost of each reader reading the groups
 * it needs once.
 */
class PoiTagGroups {

	private val tagGroups: MutableList<List<TagValuePair>?> = ArrayList()
	private val tagGroupsRead = KTLongHashSet()

	fun getTagValues(id: Int): List<TagValuePair>? {
		if (id >= tagGroups.size) {
			return null
		}
		return tagGroups[id]
	}

	fun setTagGroups(id: Int, tagValuePairs: List<TagValuePair>) {
		while (id >= tagGroups.size) {
			tagGroups.add(null)
		}
		tagGroups[id] = tagValuePairs
	}

	fun updReadTagGroups(coordsTagGroups: KTLongHashSet) {
		tagGroupsRead.addAll(coordsTagGroups)
	}

	/** Removes from [coordsTagGroups] the tiles whose tag groups this reader has already read. */
	fun checkMissingTagGroups(coordsTagGroups: KTLongHashSet): KTLongHashSet {
		val remaining = KTLongHashSet()
		for (id in coordsTagGroups.toArray()) {
			if (!tagGroupsRead.contains(id)) {
				remaining.add(id)
			}
		}
		coordsTagGroups.clear()
		coordsTagGroups.addAll(remaining.toArray())
		return coordsTagGroups
	}
}
